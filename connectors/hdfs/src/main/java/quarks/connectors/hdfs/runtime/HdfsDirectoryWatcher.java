/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package quarks.connectors.hdfs.runtime;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.slf4j.Logger;
import quarks.function.Supplier;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Watch a directory for files being added to it and create a stream
 * of pathname strings for the files.
 * <p>
 * Hidden files (files starting with ".") are ignored.
 * <p>
 * The order of the files in the stream is dictated by a {@link Comparator}.
 * The default comparator orders files by {@link File#lastModified()} values.
 * There are no guarantees on the processing order of files that
 * have the same lastModified value.
 * Note, lastModified values are subject to filesystem timestamp
 * quantization - e.g., 1second.
 * <p>
 * Note: due to the asynchronous nature of things, if files in the
 * directory may be removed, the receiver of a tuple with a "new" file
 * pathname may need to be prepared for the pathname to no longer be
 * valid when it receives the tuple or during its processing of the tuple.
 * <p>
 * The behavior on MacOS may be unsavory, even as recent as Java8, as
 * MacOs Java lacks a native implementation of {@link WatchService}.
 * The result can be a delay in detecting newly created files (e.g., 10sec)
 * as well not detecting rapid deletion and recreation of a file.
 * See:
 * http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else
 */

public class HdfsDirectoryWatcher implements AutoCloseable,
        FileFilter, Iterable<String> {

    private static final Logger trace = HdfsConnector.getTrace();
    private final Supplier<String> dirSupplier;
    private final Comparator<File> comparator;
    private final Set<String> seenFiles = Collections.synchronizedSet(new HashSet<>());
    private volatile File dirFile;
    private WatchService watcher;
    private DFSInotifyEventInputStream eventStream;
    private Queue<String> pendingNames = new LinkedList<>();


    /**
     * Watch the specified directory and generate tuples corresponding
     * to files that are created in the directory.
     * <p>
     * If a null {@code comparator} is specified, the default comparator
     * described in {@link HdfsDirectoryWatcher} is used.
     *
     * @param dirSupplier the directory to watch
     * @param comparator a comparator to order the processing of
     *        multiple newly seen files in the directory.  may be null.
     */
    public HdfsDirectoryWatcher(Supplier<String> dirSupplier, Comparator<File> comparator) {
        this.dirSupplier = dirSupplier;
        if (comparator == null) {
            comparator = // TODO 2nd order alfanum compare when same LMT?
                    (o1,o2) -> Long.compare(o1.lastModified(),
                                            o2.lastModified());
        }
        this.comparator = comparator;
    }

    private void initialize() throws IOException {

        HdfsAdmin admin = new HdfsAdmin( URI.create( dirSupplier.get() ), new Configuration() );
        eventStream = admin.getInotifyEventStream();

        trace.info("watching directory {}", dirFile);
    }

    @Override
    public void close() throws IOException {
        watcher.close();
    }

    /**
     * Waits for files to become available
     * and adds them through {@link #sortAndSubmit(List)}
     * to the pendingNames list which the iterator pulls from.
     */
    @SuppressWarnings("unchecked")
    private void watchForFiles() throws Exception {

        EventBatch eBatch = eventStream.take();

        List<File> newFiles = new ArrayList<>();
        boolean needFullScan = false;
        for (Event event : eBatch.getEvents()) {

            if (event.getEventType() == Event.EventType.CREATE) {
                Event.CreateEvent createEvent = (Event.CreateEvent) event;
                Path newPath = ((WatchEvent<Path>) watchEvent).context();
                File newFile = toAbsFile(newPath);
                if (accept(newFile))
                    newFiles.add(newFile);
            } else if (ENTRY_DELETE == watchEvent.kind()) {
                Path deletedPath = ((WatchEvent<Path>) watchEvent).context();
                File deletedFile = toAbsFile(deletedPath);
                seenFiles.remove(deletedFile.getName());
            } else if (OVERFLOW == watchEvent.kind()) {
                needFullScan = true;
            }
        }
        key.reset();

        if (needFullScan) {
            Collections.addAll(newFiles, dirFile.listFiles(this));
        }
    }

    private File toAbsFile(Path relPath) {
        return new File(dirFile, relPath.getFileName().toString());
    }

    @Override
    public boolean accept(File pathname) {
        // our "filter" function
        return !pathname.getName().startsWith(".")
                && !seenFiles.contains(pathname.getName());
    }

    @Override
    public Iterator<String> iterator() {
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new WatcherIterator();
    }

    /*
     * Iterator that returns the file names.
     * It is endless for hasNext() always returns
     * true, and next() will block in WatcherService.take
     * if no files are available.
     */
    private class WatcherIterator implements Iterator<String> {

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public String next() {

            for (;;) {

                String name = pendingNames.poll();
                if (name != null)
                    return name;

                // blocks until a file appears
                // note that even when watchForFiles()
                // returns pendingNames might still be empty
                // due to filtering.
                try {
                    watchForFiles();
                } catch (InterruptedException e) {
                    // interpret as shutdown
                    trace.debug("Interrupted");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
