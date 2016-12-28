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
package org.apache.edgent.test.connectors.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.connectors.command.CommandStreams;
import org.apache.edgent.test.connectors.common.FileUtil;
import org.apache.edgent.test.connectors.common.TestRepoPath;
import org.apache.edgent.test.providers.direct.DirectTestSetup;
import org.apache.edgent.test.topology.TopologyAbstractTest;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

import com.google.gson.JsonObject;

public class CommandStreamsTest extends TopologyAbstractTest implements DirectTestSetup {
    
    private String[] stdLines = new String[] {
            "Line 1",
            "Line 2",
            "Line 3",
    };
    
    private boolean isWindows() {
      return System.getProperty("os.name").contains("Windows");
    }
    
    private String[] mkCatFileCmd(String path) {
      if (isWindows()) {
        return new String[] {"cmd", "/c", "type", path};
      }
      else {
        return new String[] {"cat", path};
      }
    }
    
    private String[] mkCatStdInOutCmd() {
      if (isWindows()) {
        return new String[] {"cmd", "/c", "findstr", "^"};
      }
      else {
        return new String[] {"cat"};
      }
    }

    public String[] getLines() {
        return stdLines;
    }
    
    private static void delay(long millis) {
      try {
        Thread.sleep(millis);
      }
      catch (InterruptedException e) { }
    }
    
    @Test
    public void testTokenize() {
      String cmdString = "myCmd arg1    arg2\targ3";
      String[] expected = new String[]{"myCmd", "arg1", "arg2", "arg3"};
      
      assertArrayEquals(expected, CommandStreams.tokenize(cmdString).toArray(new String[0]));
    }
    
    @Test
    public void testPeriodicSource() throws Exception {
      Topology t = newTopology("testPeriodicSource");
      
      Path tempFile1 = FileUtil.createTempFile("test1", ".txt", getLines());
      System.out.println("Test: "+t.getName()+" "+tempFile1);
      
      ProcessBuilder cmd = new ProcessBuilder(mkCatFileCmd(tempFile1.toString()));
      
      int NUM_POLLS = 3;
      List<String> expLines = new ArrayList<>();
      for (int i = 0; i < NUM_POLLS; i++) {
        expLines.addAll(Arrays.asList(getLines()));
      }
      
      TStream<List<String>> ls = CommandStreams.periodicSource(t, cmd, 1, TimeUnit.SECONDS);
      TStream<String> s = ls.flatMap(list -> list); 
      
      try {
        completeAndValidate("", t, s, 10, expLines.toArray(new String[0]));
      }
      finally {
          tempFile1.toFile().delete();
      }
    }
    
    @Test
    public void testGenerate() throws Exception {
      Topology t = newTopology("testGenerate");

      Path tempFile1 = FileUtil.createTempFile("test1", ".txt", getLines());
      System.out.println("Test: "+t.getName()+" "+tempFile1);
      
      ProcessBuilder cmd = new ProcessBuilder(mkCatFileCmd(tempFile1.toString()));
      
      // N.B. if looking at trace: EDGENT-224 generate() continues running after job is closed
      TStream<String> s = CommandStreams.generate(t, cmd);
      
      try {
        completeAndValidate("", t, s, 10, getLines());
      }
      finally {
          tempFile1.toFile().delete();
      }
    }
    
    @Test
    public void testGenerateRestart() throws Exception {
      Topology t = newTopology("testGenerateRestart");

      Path tempFile1 = FileUtil.createTempFile("test1", ".txt", getLines());
      System.out.println("Test: "+t.getName()+" "+tempFile1);
      
      ProcessBuilder cmd = new ProcessBuilder(mkCatFileCmd(tempFile1.toString()));

      int NUM_RUNS = 3;
      List<String> expLines = new ArrayList<>();
      for (int i = 0; i < NUM_RUNS; i++) {
        expLines.addAll(Arrays.asList(getLines()));
      }
      
      // N.B. if looking at trace: EDGENT-224 generate() continues running after job is closed
      TStream<String> s = CommandStreams.generate(t, cmd);
      
      completeAndValidate("", t, s, 10 + ((NUM_RUNS-1) * 1/*restart delay time*/), expLines.toArray(new String[0]));
    }
    
    @Test
    public void testSink() throws Exception {
      Topology t = newTopology("testSink");

      Path tempFile1 = FileUtil.createTempFile("test1", ".txt", new String[0]);
      System.out.println("Test: "+t.getName()+" "+tempFile1);
      
      ProcessBuilder cmd = new ProcessBuilder(mkCatStdInOutCmd())
          .redirectOutput(Redirect.appendTo(tempFile1.toFile()));
      
      TStream<String> s = t.strings(getLines());
      
      TSink<String> sink = CommandStreams.sink(s, cmd);
      
      assertNotNull(sink);
      
      try {
        // start the job, sleep for a bit (await the timeout) then validate sink output
        Condition<?> never = t.getTester().tupleCount(s, Long.MAX_VALUE);
        t.getTester().complete(getSubmitter(), new JsonObject(), never, 3, TimeUnit.SECONDS);

        FileUtil.validateFile(tempFile1, getLines());
      }
      finally {
          tempFile1.toFile().delete();
      }
    }

    @Test
    public void testSinkRestart() throws Exception {
      Topology t = newTopology("testSinkRestart");

      // until someone cares enough to create Win version of sinkcmd
      assumeTrue(!isWindows());

      Path tempFile1 = FileUtil.createTempFile("test1", ".txt", new String[0]);
      System.out.println("Test: "+t.getName()+" "+tempFile1);

      int batchSize = getLines().length;
      
      // tell cmd to terminate after each batch of lines
      ProcessBuilder cmd = new ProcessBuilder("sh", getCmdPath("sinkcmd"), ""+batchSize)
          .redirectOutput(Redirect.appendTo(tempFile1.toFile()))
          .redirectError(Redirect.to(new File("/dev/stderr")));

      int NUM_RUNS = 3;
      List<String> expLines = new ArrayList<>();
      for (int i = 0; i < NUM_RUNS; i++) {
        expLines.addAll(Arrays.asList(getLines()));
      }
      AtomicInteger cnt = new AtomicInteger();
     
      TStream<String> s = t.strings(expLines.toArray(new String[0]))
          .filter(tup -> {
            // need to slow things down so the sinker has time to notice
            // the cmd has terminated.  otherwise we'll get ahead,
            // tuples will get dropped on the floor and validation will fail.
            if (cnt.incrementAndGet() > batchSize) {
              // System.out.println("SLEEPING on cnt "+ cnt.get() + " for "+tup);
              delay(1_000);
              cnt.set(1);
            }
            return true;
          });
      
      TSink<String> sink = CommandStreams.sink(s, cmd);
      
      assertNotNull(sink);
      
      try {
        // start the job, sleep for a bit (await the timeout) then validate sink output
        Condition<?> never = t.getTester().tupleCount(s, Long.MAX_VALUE);
        t.getTester().complete(getSubmitter(), new JsonObject(), never,
            6 + ((NUM_RUNS-1) * 1/*restart delay*/), TimeUnit.SECONDS);
        
        FileUtil.validateFile(tempFile1, expLines.toArray(new String[0]));
      }
      finally {
          tempFile1.toFile().delete();
      }
    }
    
    private String getCmdPath(String cmd) {
      return TestRepoPath.getPath("connectors", "command", "src", "test", "scripts", cmd);
    }

}
