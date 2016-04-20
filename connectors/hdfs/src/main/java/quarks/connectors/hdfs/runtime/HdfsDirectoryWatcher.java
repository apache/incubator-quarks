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
 */

public class HdfsDirectoryWatcher implements AutoCloseable, FileFilter, Iterable<String> {

    private static final Logger trace = HdfsConnector.getTrace();
    private final Supplier<String> dirSupplier;
    private final Comparator<File> comparator;
    private final Set<String> seenFiles = Collections.synchronizedSet(new HashSet<>());
    private volatile File dirFile;
    private String watchingDirectoryPath;
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
     * @param comparator  a comparator to order the processing of
     *                    multiple newly seen files in the directory.  may be null.
     */
    public HdfsDirectoryWatcher(Supplier<String> dirSupplier, Comparator<File> comparator) {
        this.dirSupplier = dirSupplier;
        if (comparator == null) {
            comparator = // TODO 2nd order alfanum compare when same LMT?
                (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified());
        }
        this.comparator = comparator;

    }

    private void initialize() throws IOException {
        URI dirSupplierURI = URI.create(dirSupplier.get());
        this.dirFile = new File(dirSupplier.get());
        this.watchingDirectoryPath = dirSupplierURI.getPath();
        HdfsAdmin admin = new HdfsAdmin(dirSupplierURI, new Configuration());
        eventStream = admin.getInotifyEventStream();

        trace.info("watching directory {}", dirSupplier.get());
    }

    @Override
    public void close() throws IOException {
        System.out.println("Close");
    }

    protected void sortAndSubmit(List<File> files) {
        if (files.size() > 1) {
            Collections.sort(files, comparator);
        }

        for (File file : files) {
            if (accept(file) && file.exists()) {
                pendingNames.add(file.getAbsolutePath());
                seenFiles.add(file.getName());
            }
        }
    }

    /**
     * Compare HDFS path to check given watchingDirectory is parent of filePath.
     *
     * @param watchingDirectory the path of directory to watch
     * @param filePath          file path that founded
     */
    private boolean isParentDirectory(String watchingDirectory, String filePath) {
        return filePath.startsWith(watchingDirectory);
    }

    /**
     */
    @SuppressWarnings("unchecked")
    private void watchForFiles() throws Exception {

        EventBatch eBatch = eventStream.take();

        List<File> newFiles = new ArrayList<>();

        //For Develop Logging
/*        for (Event event : eBatch.getEvents()) {
            switch (event.getEventType()) {
            case CREATE:
                Event.CreateEvent createEvent = (Event.CreateEvent) event;
                if(isParentDirectory(watchingDirectoryPath, createEvent.getPath())){
                    trace.info("inotify CREATE called. Tx Id = " + eBatch.getTxid());
                }
                break;
            case CLOSE:
                Event.CloseEvent closeEvent = (Event.CloseEvent) event;
                if(isParentDirectory(watchingDirectoryPath, closeEvent.getPath())){
                    trace.info("inotify CLOSE called. Tx Id = " + eBatch.getTxid());
                }
                break;
            case APPEND:
                Event.AppendEvent appendEvent = (Event.AppendEvent) event;
                if(isParentDirectory(watchingDirectoryPath, appendEvent.getPath())){
                    trace.info("inotify APPEND called. Tx Id = " + eBatch.getTxid());
                }
                break;
            case RENAME:
                Event.RenameEvent renameEvent = (Event.RenameEvent) event;
                if(isParentDirectory(watchingDirectoryPath, renameEvent.getDstPath())){
                    trace.info("inotify RENAME called. Tx Id = " + eBatch.getTxid());
                    trace.info("renameEvent.getDstPath() = " + renameEvent.getDstPath() + ", renameEvent.getSrcPath() = " + renameEvent.getSrcPath() );
                }
                break;
            case METADATA:
                Event.MetadataUpdateEvent metadataEvent = (Event.MetadataUpdateEvent) event;
                if(isParentDirectory(watchingDirectoryPath, metadataEvent.getPath())){
                    trace.info("inotify METADATA called. Tx Id = " + eBatch.getTxid());
                }
                break;
            case UNLINK:
                Event.UnlinkEvent unlinkEvent = (Event.UnlinkEvent) event;
                if(isParentDirectory(watchingDirectoryPath, unlinkEvent.getPath())){
                    trace.info("inotify UNLINK called. Tx Id = " + eBatch.getTxid());
                }
                break;
            default:
                break;
            }
        }*/

        for (Event event : eBatch.getEvents()) {
            switch (event.getEventType()) {
            case CREATE:
                Event.CreateEvent createEvent = (Event.CreateEvent) event;
                if (isParentDirectory(watchingDirectoryPath, createEvent.getPath()) && !createEvent
                    .getPath().endsWith("_COPYING_")) {
                    File newFile = toAbsFile(createEvent.getPath());
                    if (accept(newFile)) {
                        newFiles.add(newFile);
                    }
                }
                break;
            case UNLINK:
                Event.UnlinkEvent unlinkEvent = (Event.UnlinkEvent) event;
                if(isParentDirectory(watchingDirectoryPath, unlinkEvent.getPath())){
                    File deletedFile = toAbsFile(unlinkEvent.getPath());
                    seenFiles.remove(deletedFile.getName());
                }
                break;
            case RENAME:
                Event.RenameEvent renameEvent = (Event.RenameEvent) event;
                if(isParentDirectory(watchingDirectoryPath, renameEvent.getDstPath())){
                    File newFile = toAbsFile(renameEvent.getDstPath());
                    if (accept(newFile)) {
                        newFiles.add(newFile);
                    }
                }
                break;
            default:
                break;
            }
        }
        sortAndSubmit(newFiles);
    }

    private File toAbsFile(String relPath) {
        return new File(dirFile, relPath);
    }

    @Override
    public boolean accept(File pathname) {
        // our "filter" function
        return !pathname.getName().startsWith(".") && !seenFiles.contains(pathname.getName());
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

            for (; ; ) {

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
