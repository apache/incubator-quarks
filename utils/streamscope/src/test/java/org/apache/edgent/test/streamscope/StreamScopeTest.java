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
package org.apache.edgent.test.streamscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.streamscope.StreamScope;
import org.apache.edgent.streamscope.StreamScopeRegistry;
import org.apache.edgent.streamscope.StreamScope.Sample;
import org.apache.edgent.topology.TopologyAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public abstract class StreamScopeTest extends TopologyAbstractTest {	
    
    @Test
    public void testStreamScopeFn() throws Exception {

        StreamScope<Integer> ss = new StreamScope<>();

        List<Sample<Integer>> samples; 
        Sample<Integer> sample;

        assertFalse(ss.isEnabled());
        assertNotNull(ss.bufferMgr());
        assertNotNull(ss.triggerMgr());
        assertEquals(0, ss.getSampleCount());
        samples = ss.getSamples();
        assertNotNull(samples);
        assertEquals(0, samples.size());
        
        // ---------------- no capture when not enabled
        ss.accept(1);
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        
        ss.setEnabled(true);
        
        // ---------------- single capture
        // note: getSamples() removes captured tuples
        ss.accept(100);
        assertEquals(1, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(1, samples.size());
        sample = samples.get(0);
        assertEquals(100, sample.tuple().intValue());
        assertTrue(sample.timestamp() != 0);
        assertTrue(sample.nanoTime() != 0);
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        
        // ---------------- next capture/get; different lists
        List<Sample<Integer>> savedSamples = samples;
        ss.accept(101);
        assertEquals(1, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(101, samples.get(0).tuple().intValue());
        assertTrue(samples != savedSamples);
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        
        // ---------------- multi capture
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(101, samples.get(1).tuple().intValue());
        assertEquals(102, samples.get(2).tuple().intValue());
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        
        // ----------------  disable => clears capture buffer
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        assertEquals(3, ss.getSampleCount());
        ss.setEnabled(false);
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());

        ss.setEnabled(true);

        // ---------------- buffer control at the limit (no eviction)
        ss.bufferMgr().setMaxRetentionCount(3);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(101, samples.get(1).tuple().intValue());
        assertEquals(102, samples.get(2).tuple().intValue());
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        
        // ---------------- buffer control with eviction
        ss.bufferMgr().setMaxRetentionCount(2);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        assertEquals(2, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(101, samples.get(0).tuple().intValue());
        assertEquals(102, samples.get(1).tuple().intValue());
        assertEquals(0, ss.getSampleCount());
        assertEquals(0, ss.getSamples().size());
        ss.bufferMgr().setMaxRetentionCount(10);
        
        // ---------------- trigger byCount
        ss.triggerMgr().setCaptureByCount(3);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        ss.accept(103);
        ss.accept(104);
        ss.accept(105);
        ss.accept(106);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(103, samples.get(1).tuple().intValue());
        assertEquals(106, samples.get(2).tuple().intValue());
        
        // ---------------- trigger continuous / ByCount(1)
        ss.triggerMgr().setCaptureByCount(1);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(101, samples.get(1).tuple().intValue());
        assertEquals(102, samples.get(2).tuple().intValue());
        
        // ---------------- trigger byPredicate
        ss.triggerMgr().setCaptureByPredicate(t -> t % 2 == 0);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        ss.accept(103);
        ss.accept(104);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(102, samples.get(1).tuple().intValue());
        assertEquals(104, samples.get(2).tuple().intValue());
        
        // ---------------- trigger byTime
        ss.triggerMgr().setCaptureByTime(100, TimeUnit.MILLISECONDS);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        Thread.sleep(110);
        ss.accept(103);
        ss.accept(104);
        ss.accept(105);
        Thread.sleep(110);
        ss.accept(106);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(103, samples.get(1).tuple().intValue());
        assertEquals(106, samples.get(2).tuple().intValue());
        
        // ---------------- trigger pause
        ss.triggerMgr().setCaptureByCount(1);
        ss.accept(100);
        ss.accept(101);
        ss.triggerMgr().setPaused(true);
        assertTrue(ss.triggerMgr().isPaused());
        ss.accept(102);
        ss.accept(103);
        ss.triggerMgr().setPaused(false);
        assertFalse(ss.triggerMgr().isPaused());
        ss.accept(104);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(101, samples.get(1).tuple().intValue());
        assertEquals(104, samples.get(2).tuple().intValue());
        
        // ---------------- trigger pauseOn
        ss.triggerMgr().setCaptureByCount(1);
        ss.triggerMgr().setPauseOn(t -> t == 102);
        ss.accept(100);
        ss.accept(101);
        ss.accept(102);
        ss.accept(103);
        ss.accept(104);
        ss.accept(105);
        assertTrue(ss.triggerMgr().isPaused());
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(100, samples.get(0).tuple().intValue());
        assertEquals(101, samples.get(1).tuple().intValue());
        assertEquals(102, samples.get(2).tuple().intValue());
        ss.triggerMgr().setPaused(false);
        ss.accept(1000);
        ss.accept(1010);
        ss.accept(102);
        ss.accept(1030);
        assertEquals(3, ss.getSampleCount());
        samples = ss.getSamples();
        assertEquals(1000, samples.get(0).tuple().intValue());
        assertEquals(1010, samples.get(1).tuple().intValue());
        assertEquals(102, samples.get(2).tuple().intValue());
        
    }

    @Test
    public void testStreamScopeRegistry() throws Exception {

      StreamScope<Integer> ss1 = new StreamScope<>();
      StreamScope<Integer> ss2 = new StreamScope<>();
      
      StreamScopeRegistry rgy = new StreamScopeRegistry();
      
      assertNotNull(rgy.getNames());
      assertEquals(0, rgy.getNames().size());
      assertNotNull(rgy.getStreamScopes());
      assertEquals(0, rgy.getStreamScopes().size());
      assertNull(rgy.lookup("xyzzy"));
      rgy.unregister("xyzzy");
      rgy.unregister(ss1);
      
      // ---------- name generation / parse functions
      String alias1Name = StreamScopeRegistry.nameForStreamAlias("alias1");
      assertNotNull(alias1Name);
      String alias2Name = StreamScopeRegistry.nameForStreamAlias("alias2");
      assertNotNull(alias2Name);
      assertFalse(alias1Name.equals(alias2Name));
      String alias1 = StreamScopeRegistry.streamAliasFromName(alias1Name);
      assertEquals("alias1", alias1);
      
      String id1Name = StreamScopeRegistry.nameForStreamId("id1");
      assertNotNull(id1Name);
      String id2Name = StreamScopeRegistry.nameForStreamId("id2");
      assertNotNull(id2Name);
      assertFalse(id1Name.equals(id2Name));
      String id1 = StreamScopeRegistry.streamIdFromName(id1Name);
      assertEquals("id1", id1);
      
      String streamId1 = StreamScopeRegistry.mkStreamId("JOB_1", "OP_2", 0);
      assertNotNull(streamId1);
      String streamId2 = StreamScopeRegistry.mkStreamId("JOB_1", "OP_2", 1);
      assertNotNull(streamId2);
      assertFalse(streamId1.equals(streamId2));
      id1Name = StreamScopeRegistry.nameForStreamId(streamId1);
      id1 = StreamScopeRegistry.streamIdFromName(id1Name);
      assertEquals(id1, streamId1);

      assertFalse(StreamScopeRegistry.nameForStreamAlias("1")
          .equals(StreamScopeRegistry.nameForStreamId("1")));
      
      // ---------- register
      rgy.register(alias1Name, ss1);
      rgy.register(alias2Name, ss2);
      rgy.register(id2Name, ss2);

      // ---------- lookup
      assertSame(ss1, rgy.lookup(alias1Name));
      assertSame(ss2, rgy.lookup(alias2Name));
      assertSame(null, rgy.lookup(id1Name));
      assertSame(ss2, rgy.lookup(id2Name));
     
      // ----------- getNames
      assertEquals(3, rgy.getNames().size());
      assertTrue(rgy.getNames().contains(alias1Name));
      assertFalse(rgy.getNames().contains(id1Name));
      assertTrue(rgy.getNames().contains(alias2Name));
      assertTrue(rgy.getNames().contains(id2Name));
      
      // ----------- getStreamScopes
      assertEquals(2, rgy.getStreamScopes().keySet().size());
      assertTrue(rgy.getStreamScopes().keySet().contains(ss1));
      assertTrue(rgy.getStreamScopes().keySet().contains(ss2));
      assertEquals(1, rgy.getStreamScopes().get(ss1).size());
      assertTrue(rgy.getStreamScopes().get(ss1).contains(alias1Name));
      assertEquals(2, rgy.getStreamScopes().get(ss2).size());
      assertTrue(rgy.getStreamScopes().get(ss2).contains(alias2Name));
      assertFalse(rgy.getStreamScopes().get(ss2).contains(id1Name));
      assertTrue(rgy.getStreamScopes().get(ss2).contains(id2Name));
      
      // ---------- unregister
      rgy.unregister(alias1Name);
      assertNull(rgy.lookup(alias1Name));
      assertEquals(2, rgy.getNames().size());
      assertFalse(rgy.getNames().contains(alias1Name));
      assertFalse(rgy.getStreamScopes().keySet().contains(ss1));
      rgy.unregister(id1Name);
      assertTrue(rgy.getStreamScopes().keySet().contains(ss2));
      
      rgy.unregister(alias2Name);
      assertNull(rgy.lookup(alias2Name));
      assertEquals(1, rgy.getNames().size());
      assertFalse(rgy.getNames().contains(alias2Name));
      assertTrue(rgy.getStreamScopes().keySet().contains(ss2));
      assertSame(ss2, rgy.lookup(id2Name));
      rgy.unregister(id2Name);
      assertEquals(0, rgy.getNames().size());
      assertEquals(0, rgy.getStreamScopes().keySet().size());
      
      rgy.register(alias2Name, ss2);
      rgy.register(id2Name, ss2);
      rgy.unregister(ss2);
      assertEquals(0, rgy.getNames().size());
      assertEquals(0, rgy.getStreamScopes().keySet().size());
    }

}
