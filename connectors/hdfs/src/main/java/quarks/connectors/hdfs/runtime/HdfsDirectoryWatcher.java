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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.slf4j.Logger;
import quarks.function.Supplier;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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

public class HdfsDirectoryWatcher implements AutoCloseable, Iterable<String> {

    private static final Logger trace = HdfsConnector.getTrace();
    private final Supplier<String> dirSupplier;
    private final Comparator<String> comparator;
    private final Set<String> seenFiles = Collections.synchronizedSet(new HashSet<>());
    private String hostPath;
    private String watchingDirectoryPath;
    private DFSInotifyEventInputStream eventStream;
    private Queue<String> pendingNames = new LinkedList<>();
    private FileSystem hdfs;
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
    public HdfsDirectoryWatcher(Supplier<String> dirSupplier, Comparator<String> comparator) {
        this.dirSupplier = dirSupplier;
        if (comparator == null) {
            comparator = (o1, o2) -> Integer.compare(o1.length(), o1.length());
        }
        this.comparator = comparator;

    }

    //To-Do: Do not throws exception
    private void initialize() throws IOException {
        URI dirSupplierURI = URI.create(dirSupplier.get());
        Configuration conf = new Configuration();
        this.hostPath =
            dirSupplierURI.getScheme() + "://" + dirSupplierURI.getHost() + ":" + dirSupplierURI
                .getPort();

        trace.info("dirFile.getPath() = " + hostPath + dirSupplierURI.getPath());
        conf.set("fs.defaultFS", hostPath);
        this.hdfs = FileSystem.get(conf);
        this.watchingDirectoryPath = dirSupplierURI.getPath();
        HdfsAdmin admin = new HdfsAdmin(dirSupplierURI, new Configuration());
        this.eventStream = admin.getInotifyEventStream();

        trace.info("watching directory {}", dirSupplier.get());
    }

    @Override
    public void close() throws IOException {
        System.out.println("Close");
    }

    protected void sortAndSubmit(List<String> files) {
        if (files.size() > 1) {
            Collections.sort(files, comparator);
        }

        for (String file : files) {


            try {
                Path path=new Path(file);
                System.out.println(file.toString()+ ", hdfs.exists(path) : " + hdfs.exists(path));
                if (accept(file) && hdfs.exists(path)) {
                    pendingNames.add(file);
                    seenFiles.add(file);
                    double rand = Math.random();
                    for(String f : seenFiles){
                        System.out.println(rand + " : " + f);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
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

        List<String> newFiles = new ArrayList<>();

        //For Develop Logging
        /*for (Event event : eBatch.getEvents()) {
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
                    if (accept(createEvent.getPath())) {
                        newFiles.add(toFullPath(createEvent.getPath()));
                    }
                }
                break;
            case UNLINK:
                Event.UnlinkEvent unlinkEvent = (Event.UnlinkEvent) event;
                if(isParentDirectory(watchingDirectoryPath, unlinkEvent.getPath())){
                    seenFiles.remove(toFullPath(unlinkEvent.getPath()));
                }
                break;
            case RENAME:
                Event.RenameEvent renameEvent = (Event.RenameEvent) event;
                if(isParentDirectory(watchingDirectoryPath, renameEvent.getDstPath())){
                    if (accept(renameEvent.getDstPath())) {
                        trace.info("Rename accept called : " + renameEvent.getDstPath());
                        newFiles.add(toFullPath(renameEvent.getDstPath()));
                    }
                }
                break;
            default:
                break;
            }
        }
        sortAndSubmit(newFiles);
    }

    private String toFullPath(String relPath) {
        return new String(hostPath + relPath);
    }

    public boolean accept(String pathname) {
        // our "filter" function
        trace.info("pathname.getName() = " + pathname);
        return !pathname.startsWith(".") && !seenFiles.contains(pathname);
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
