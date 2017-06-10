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
package org.apache.edgent.test.connectors.file;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.edgent.connectors.file.FileStreams;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.test.connectors.common.FileUtil;
import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.junit.Test;

public class FileStreamsTest extends DirectTopologyTestBase {
    
    String[] stdLines = new String[] {
            "If you can keep your head when all about you",
            "Are losing theirs and blaming it on you,",
            "If you can trust yourself when all men doubt you,",
            "But make allowance for their doubting too;"                
    };

    public String[] getLines() {
        return stdLines;
    }

    /**
     * Test that directory watcher creates the correct output.
     * @throws Exception on failure
     */
    @Test
    public void testDirectoryWatcherOrder() throws Exception {
        Topology t = newTopology("testDirectoryWatcherOrder");

        runDirectoryWatcher(t, 20, 1);
    }
    
    @Test
    public void testDirectoryWatcherOrderWithDelete() throws Exception {
        Topology t = newTopology("testDirectoryWatcherOrderWithDelete");
        
        runDirectoryWatcher(t, 20, 3);
    }
    
    @Test
    public void testDirectoryWatcherPreExisting() throws Exception {
        Topology t = newTopology("testDirectoryWatcherPreExisting");
        
        runDirectoryWatcher(t, 20, -1);
    }
    
    private void runDirectoryWatcher(Topology t, int numberOfFiles, int repeat) throws Exception {
        
        boolean preExistingMode = repeat < 0;
        repeat = Math.abs(repeat);
        
        System.out.println("##### "+t.getName());
        final Path dir = Files.createTempDirectory("testdw");
        final String[] files = new String[numberOfFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = dir.resolve("A" + (numberOfFiles - i)).toAbsolutePath()
                    .toString();
        }
        List<String> expectedFileNames = new ArrayList<>();
        for (int r = 0; r < repeat; r++)
            expectedFileNames.addAll(Arrays.asList(files));
        
        if (preExistingMode) {
            // exercise the case where files exist when the watcher starts
            // also test that files starting with "." (hiddden files)
            // are ignored.  Add the file here but not to the expected list.
            String[] filesWithHidden = Arrays.copyOf(files, files.length+1);
            File f = new File(files[0]);
            File hidden = new File(f.getParent(), f.getName().replaceFirst("^", "."));
            filesWithHidden[files.length] = hidden.toString();
            createFiles(filesWithHidden, repeat);
        }
        else {
            // Create the files from within the topology.
            //
            // Due to vagaries / delays that can occur in operator startup, 
            // delay the initial file creation to give the watcher a chance to startup.
            //
            // e.g., with numberOfFiles=20 & repeat=1, each group of files
            // only lasts 20*(10ms*2) => 200ms.  That can easily happen before
            // the watcher is started and has done its first dir.listFiles(),
            // with the result being not seeing/processing the expected number
            // of files.
    
            if (repeat > 1) {
                if ("Mac OS X".equals(System.getProperty("os.name"))) {
                    // This test does delete/recreate too fast for this platform's
                    // WatchService.  See comments in FileStreams.directoryWatcher()
                    // and in DirectoryWatcher.
                    System.err.println("Test "+t.getName()+": sigh not on MacOS");
                    assumeTrue(false);
                }
            }

            int finalRepeat = repeat;
            PlumbingStreams.blockingOneShotDelay(
                    t.collection(Arrays.asList(0L)), 3, TimeUnit.SECONDS)
            .sink((beacon) -> createFiles(files, finalRepeat));
        }

        TStream<String> fileNames = FileStreams.directoryWatcher(t, 
                () -> dir.toAbsolutePath().toString());
        
        try {
            // These tests require unordered validation because the
            // files are created only 10msec apart and the filesystem
            // and/or event system may not preserve the actual ordering
            // at that resolution.
            
            fileNames.sink(str -> System.out.println("got file "+str));
            
            completeAndValidate(false/*ordered*/, "", t, fileNames, 20,
                    expectedFileNames.toArray(new String[0]));
        }
        finally {
            deleteFilesAndDir(dir, files);
        }
    }

    private void deleteFilesAndDir(final Path dir, final String[] files) {
        // Ensure we clean up!
        for (int i = 0; i < files.length; i++) {
            Path path = Paths.get(files[i]);
            path.toFile().delete();
        }
        dir.toFile().delete();
    }

    private void createFiles(String[] files, int repeat) {
        try {
            for (int r = 0; r < repeat; r++) {
                for (int i = 0; i < files.length; i++) {
                    Path path = Paths.get(files[i]);
                    if (r > 0) {
                        path.toFile().delete();
                        Thread.sleep(10);
                        // System.out.println(new Date() + " deleted " + path.getFileName());
                    }
                    Files.createFile(path);
                    Thread.sleep(10);
                    // System.out.println(new Date() + " created " + path.getFileName());
                }
            }
        } catch (InterruptedException e) {
            // shutdown
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testTextFileReader() throws Exception {
        Topology t = newTopology("testTextFileReader");
        
        String[] lines = getLines();
        String[] ucLines = Stream.of(lines)
                .map(line -> line.toUpperCase())
                .toArray(String[]::new);
        String[] allLines = Stream.concat(Stream.of(lines), Stream.of(ucLines))
                .toArray(String[]::new);
        
        Path tempFile1 = FileUtil.createTempFile("test1", "txt", lines);
        Path tempFile2 = FileUtil.createTempFile("test2", "txt", ucLines);
        
        TStream<String> contents = FileStreams.textFileReader(
                t.strings(tempFile1.toAbsolutePath().toString(),
                        tempFile2.toAbsolutePath().toString()));
        
        try {
            completeAndValidate("", t, contents, 10, allLines);
        }
        finally {
            tempFile1.toFile().delete();
            tempFile2.toFile().delete();
        }
    }

    @Test
    public void testTextFileReaderProblemPaths() throws Exception {
        Topology t = newTopology("testTextFileReaderProblemPaths");
        
        String[] lines = getLines();
        String[] ucLines = Stream.of(lines)
                .map(line -> line.toUpperCase())
                .toArray(String[]::new);
        String[] allLines = Stream.concat(Stream.of(lines), Stream.of(ucLines))
                .toArray(String[]::new);
        
        Path tempFile1 = FileUtil.createTempFile("test1", "txt", lines);
        Path tempFile2 = FileUtil.createTempFile("test2", "txt", ucLines);
        
        // ensure a problem in one file (tuple) doesn't affect others.
        // The problem files should result in a log entry but otherwise be ignored.
        
        TStream<String> contents = FileStreams.textFileReader(
                t.strings(tempFile1.toAbsolutePath().toString(),
                        "/no-such-file",
                        "/tmp",
                        tempFile2.toAbsolutePath().toString()));
        
        try {
            completeAndValidate("", t, contents, 10, allLines);
        }
        finally {
            tempFile1.toFile().delete();
            tempFile2.toFile().delete();
        }
    }

    @Test
    public void testTextFileReaderPrePost() throws Exception {
        Topology t = newTopology("testTextFileReaderPrePost");
        
        String[] lines = getLines();
        String[] ucLines = Stream.of(lines)
                .map(line -> line.toUpperCase())
                .toArray(String[]::new);
        
        Path tempFile1 = FileUtil.createTempFile("test1", "txt", lines);
        Path tempFile2 = FileUtil.createTempFile("test2", "txt", ucLines);
        
        // Be insensitive to Windows path separators and "/tmp" location
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        File tmpDir = File.createTempFile("anything", "anything");
        tmpDir.delete();
        tmpDir = tmpDir.getParentFile();
        
        Function<String,String> preFn
            = path -> String.format("[PRE-FUNCTION] path:%s", path);
        BiFunction<String,Exception,String> postFn
            = (path,exc) -> String.format("[POST-FUNCTION] path:%s exc=%s",
                    path, Objects.toString(exc));
        

        List<String> allLines = new ArrayList<>();
        allLines.add(preFn.apply(tempFile1.toAbsolutePath().toString()));
        allLines.addAll(Arrays.asList(lines));
        allLines.add(postFn.apply(tempFile1.toAbsolutePath().toString(), null));
        //
        String noSuchFilePath = new File(tmpDir, "no-such-file").toString();
        allLines.add(preFn.apply(noSuchFilePath));
        allLines.add(postFn.apply(noSuchFilePath, new NoSuchFileException(noSuchFilePath)));
        //
        String tmpDirPath = tmpDir.toString();
        allLines.add(preFn.apply(tmpDirPath));
        allLines.add(postFn.apply(tmpDirPath,
                                    isWindows
                                        ? new AccessDeniedException(tmpDirPath)
                                        : new IOException("Is a directory")));
        //
        allLines.add(preFn.apply(tempFile2.toAbsolutePath().toString()));
        allLines.addAll(Arrays.asList(ucLines));
        allLines.add(postFn.apply(tempFile2.toAbsolutePath().toString(), null));
        
        TStream<String> contents = FileStreams.textFileReader(
                t.strings(tempFile1.toAbsolutePath().toString(),
                        noSuchFilePath,
                        tmpDirPath,
                        tempFile2.toAbsolutePath().toString()),
                preFn, postFn
                );

        try {
            completeAndValidate("", t, contents, 10, allLines.toArray(new String[0]));
        }
        finally {
            tempFile1.toFile().delete();
            tempFile2.toFile().delete();
        }
    }
}
