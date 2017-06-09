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
package org.apache.edgent.test.analytics.sensors;

import static org.apache.edgent.function.Functions.identity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.analytics.sensors.Deadtime;
import org.apache.edgent.analytics.sensors.Filters;
import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

import com.google.gson.JsonObject;

public class FiltersTest  extends DirectTopologyTestBase {
	@Test
	public void testDeadbandIdentity() throws Exception {
	    Topology topology = newTopology("testDeadband");
	    
	    TStream<Double> values = topology.of(12.9, 3.4, 12.3, 15.6, 18.4, -3.7, -4.5, 15.0, 16.0, 30.0, 42.0 );
	    
	    TStream<Double> filtered = Filters.deadband(values, identity(),
	    		v -> v >= 10.0 && v <= 30.0);
	    
        Condition<Long> count = topology.getTester().tupleCount(filtered, 7);
        Condition<List<Double>> contents = topology.getTester().streamContents(filtered, 12.9, 3.4, 12.3, -3.7, -4.5, 15.0, 42.0 );
        complete(topology, count);
        assertTrue(count.valid());
        assertTrue(contents.valid());
	}
	@Test
	public void testDeadbandFunction() throws Exception {
	    Topology topology = newTopology("testDeadbandFunction");
	    
	    TStream<Double> values = topology.of(3.4, 12.3, 15.6, 18.4, -3.7, -4.5, 15.0, 16.0, 30.0, 42.0 );
	    
	    TStream<JsonObject> vj = values.map(d -> {JsonObject j = new JsonObject(); j.addProperty("id", "A"); j.addProperty("reading", d);return j;});
	    
	    TStream<JsonObject> filtered = Filters.deadband(vj,
	    		tuple -> tuple.get("reading").getAsDouble(),
	    		v -> v >= 10.0 && v <= 30.0);
	    
        Condition<Long> count = topology.getTester().tupleCount(filtered, 6);
        Condition<List<JsonObject>> contents = topology.getTester().streamContents(filtered);
        complete(topology, count);
        assertTrue(count.valid());
        
        List<JsonObject> results = contents.getResult();
        assertEquals(6, results.size());
        
        assertEquals("A", results.get(0).get("id").getAsString());
        assertEquals(3.4, results.get(0).get("reading").getAsDouble(), 0.0);
        
        // First value after a period out of range
        assertEquals("A", results.get(1).get("id").getAsString());
        assertEquals(12.3, results.get(1).get("reading").getAsDouble(), 0.0);
        
        assertEquals("A", results.get(2).get("id").getAsString());
        assertEquals(-3.7, results.get(2).get("reading").getAsDouble(), 0.0);
        
        assertEquals("A", results.get(3).get("id").getAsString());
        assertEquals(-4.5, results.get(3).get("reading").getAsDouble(), 0.0);
        
        assertEquals("A", results.get(4).get("id").getAsString());
        assertEquals(15.0, results.get(4).get("reading").getAsDouble(), 0.0);
        
        assertEquals("A", results.get(5).get("id").getAsString());
        assertEquals(42.0, results.get(5).get("reading").getAsDouble(), 0.0);
	}
	
	@Test
	public void testDeadbandMaxSuppression() throws Exception {
	    Topology topology = newTopology("testDeadbandMaxSuppression");
	    
	    TStream<Double> values = topology.of(12.9, 3.4, 12.3, 15.6, 18.4, -3.7, -4.5, 15.0, 16.0, 30.0, 42.0 );
	    
	    // 18.4 will be included as it is delayed since the last inband value.
	    values = values.modify(tuple -> {if (tuple == 18.4)
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} return tuple;});
	    
	    TStream<Double> filtered = Filters.deadband(values, identity(),
	    		v -> v >= 10.0 && v <= 30.0, 3, TimeUnit.SECONDS);
	    
        Condition<Long> count = topology.getTester().tupleCount(filtered, 8);
        Condition<List<Double>> contents = topology.getTester().streamContents(filtered, 12.9, 3.4, 12.3, 18.4, -3.7, -4.5, 15.0, 42.0 );
        complete(topology, count);
        assertTrue(count.valid());
        assertTrue(contents.valid());
	}
    
    @Test
    public void testDeadtime() throws Exception {
        Topology topology = newTopology("testDeadtime");
        
        int maxTupleCnt = 10;
        AtomicInteger cnt = new AtomicInteger();
        
        TStream<Integer> values = topology.poll(() -> {
            int curCnt = cnt.incrementAndGet();
            if (curCnt > maxTupleCnt)
                return null;
            return curCnt;
            }, 100, TimeUnit.MILLISECONDS);
        
        // use a deadtime value that causes filtering of every other tuple
        TStream<Integer> filtered = Filters.deadtime(values, 150, TimeUnit.MILLISECONDS);
        
        Condition<Long> count = topology.getTester().tupleCount(filtered, maxTupleCnt/2);
        Condition<List<Integer>> contents = topology.getTester().streamContents(filtered, 1, 3, 5, 7, 9 );
        complete(topology, count);
        
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testDeadtimeNoDeadtime() throws Exception {
        Topology topology = newTopology("testDeadtimeNoDeadtime");
        
        TStream<Integer> values = topology.of(1,2,3,4,5,6,7,8,9,10);
        
        // no deadtime
        TStream<Integer> filtered = Filters.deadtime(values, 0, TimeUnit.MILLISECONDS);
        
        Condition<Long> count = topology.getTester().tupleCount(filtered, 10);
        Condition<List<Integer>> contents = topology.getTester().streamContents(filtered, 1,2,3,4,5,6,7,8,9,10 );
        complete(topology, count);
        
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testDeadtimeTooShort() throws Exception {
        Topology topology = newTopology("testDeadtimeTooShort");
        
        TStream<Integer> values = topology.of(1,2,3,4,5,6,7,8,9,10);
        
        // no deadtime due to < 1ms
        TStream<Integer> filtered = Filters.deadtime(values, 999, TimeUnit.MICROSECONDS);
        
        Condition<Long> count = topology.getTester().tupleCount(filtered, 10);
        Condition<List<Integer>> contents = topology.getTester().streamContents(filtered, 1,2,3,4,5,6,7,8,9,10 );
        complete(topology, count);
        
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testDeadtimeDynamic() throws Exception {
        // Timing variances on shared machines can cause this test to fail
        assumeTrue(!Boolean.getBoolean("edgent.build.ci"));
        
        Topology topology = newTopology("testDeadtimeDynamic");

        // initial deadtime to consume 5 tuples. get 1, skip 2-6, get 7, skip 8-10
        Deadtime<Integer> deadtime = new Deadtime<>(500, TimeUnit.MILLISECONDS);

        int maxTupleCnt = 10;
        AtomicInteger cnt = new AtomicInteger();
        
        TStream<Integer> values = topology.poll(() -> {
            int curCnt = cnt.incrementAndGet();
            if (curCnt > maxTupleCnt)
                return null;
            if (curCnt == 4) {
                // shorten deadtime, so should now get tup 4,6,8,10
                deadtime.setPeriod(150, TimeUnit.MILLISECONDS);
            }
            else if (curCnt == 7) {
                // lengthen deadtime, so should now exclude 8 too and then 10
                deadtime.setPeriod(250, TimeUnit.MILLISECONDS);
            }
            return curCnt;
            }, 100, TimeUnit.MILLISECONDS);
        
        TStream<Integer> filtered = values.filter(deadtime);
        
        Condition<Long> count = topology.getTester().tupleCount(filtered, 4);
        Condition<List<Integer>> contents = topology.getTester().streamContents(filtered, 1, 4, 6, 9 );
        complete(topology, count);
        
        assertTrue(contents.getResult().toString(), contents.valid());
    }
}
