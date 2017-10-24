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
package org.apache.edgent.test.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Functions;
import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.apache.edgent.topology.plumbing.Valve;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;

@Ignore
public abstract class PlumbingTest extends TopologyAbstractTest {
	

	@Test
    public void testBlockingDelay() throws Exception {
		// Timing variances on shared machines can cause this test to fail
		assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology topology = newTopology();
        
        TStream<String> strings = topology.strings("a", "b", "c", "d");
        
        TStream<Long> starts = strings.map(v -> System.currentTimeMillis());
        
        // delay stream
        starts = PlumbingStreams.blockingDelay(starts, 300, TimeUnit.MILLISECONDS);
        
        // calculate delay
        starts = starts.modify(v -> System.currentTimeMillis() - v);
        
        starts = starts.filter(v -> v >= 300);
        
        Condition<Long> tc = topology.getTester().tupleCount(starts, 4);
        complete(topology, tc);
        assertTrue("valid:" + tc.getResult(), tc.valid());
    }

    @Test
    public void testBlockingThrottle() throws Exception {
		// Timing variances on shared machines can cause this test to fail
    	assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology topology = newTopology();
        
        TStream<String> strings = topology.strings("a", "b", "c", "d");

        TStream<Long> emittedDelays = strings.map(v -> 0L);
        
        // throttle stream
        long[] lastEmittedTimestamp = { 0 };
        emittedDelays = PlumbingStreams.blockingThrottle(emittedDelays, 300, TimeUnit.MILLISECONDS)
                .map(t -> {
                    // compute the delay since the last emitted tuple
                    long now = System.currentTimeMillis();
                    if (lastEmittedTimestamp[0] == 0)
                        lastEmittedTimestamp[0] = now;
                    t = now - lastEmittedTimestamp[0];
                    lastEmittedTimestamp[0] = now;
                    // System.out.println("### "+t);
                    return t;
                    })
                .map(t -> {
                    // simulate 200ms downstream processing delay
                    try {
                        Thread.sleep(TimeUnit.MILLISECONDS.toMillis(200));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } return t;
                    }) ;

        // should end up with throttled delays close to 300 (not 500 like
        // a blockingDelay() under these same conditions would yield)
        emittedDelays = emittedDelays.filter(v -> v <= 320);
        
        Condition<Long> tc = topology.getTester().tupleCount(emittedDelays, 4);
        complete(topology, tc);
        assertTrue("valid:" + tc.getResult(), tc.valid());
    }

    @Test
    public void testOneShotDelay() throws Exception {

        Topology topology = newTopology();
        
        TStream<String> strings = topology.strings("a", "b", "c", "d");
        
        TStream<Long> starts = strings.map(v -> System.currentTimeMillis());
        
        // delay stream
        starts = PlumbingStreams.blockingOneShotDelay(starts, 300, TimeUnit.MILLISECONDS);
        
        // calculate display
        starts = starts.modify(v -> System.currentTimeMillis() - v);
        
        // the first tuple shouldn't satisfy the predicate
        starts = starts.filter(v -> v < 300);
        
        Condition<Long> tc = topology.getTester().tupleCount(starts, 3);
        complete(topology, tc);
        assertTrue("valid:" + tc.getResult(), tc.valid());
    }

    public static class TimeAndId {
    	private static AtomicInteger ids = new AtomicInteger();
    	long ms;
    	final int id;
    	
    	public TimeAndId() {
    		this.ms = System.currentTimeMillis();
    		this.id = ids.incrementAndGet();
    	}
    	public TimeAndId(TimeAndId tai) {
    		this.ms = System.currentTimeMillis() - tai.ms;
    		this.id = tai.id;
    	}
    	@Override
    	public String toString() {
    		return "TAI:" + id + "@" + ms;
    	}
    	
    }
    
    @Test
    public void testPressureReliever() throws Exception {
		// Timing variances on shared machines can cause this test to fail
		assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology topology = newTopology();
        
        TStream<TimeAndId> raw = topology.poll(() -> new TimeAndId(), 10, TimeUnit.MILLISECONDS);
           
        
        TStream<TimeAndId> pr = PlumbingStreams.pressureReliever(raw, Functions.unpartitioned(), 5);
        
        // insert a blocking delay acting as downstream operator that cannot keep up
        TStream<TimeAndId> slow = PlumbingStreams.blockingDelay(pr, 200, TimeUnit.MILLISECONDS);
        
        // calculate the delay
        TStream<TimeAndId> slowM = slow.modify(v -> new TimeAndId(v));
        
        // Also process raw that should be unaffected by the slow path
        TStream<String> processed = raw.asString();
        
        
        Condition<Long> tcSlowCount = topology.getTester().atLeastTupleCount(slow, 20);
        Condition<List<TimeAndId>> tcRaw = topology.getTester().streamContents(raw);
        Condition<List<TimeAndId>> tcSlow = topology.getTester().streamContents(slow);
        Condition<List<TimeAndId>> tcSlowM = topology.getTester().streamContents(slowM);
        Condition<List<String>> tcProcessed = topology.getTester().streamContents(processed);
        complete(topology, tcSlowCount);
        
        assertTrue(tcProcessed.getResult().size() > tcSlowM.getResult().size());
        for (TimeAndId delay : tcSlowM.getResult())
            assertTrue("delay:"+delay, delay.ms < 300);

        // Must not lose any tuples in the non relieving path
        Set<TimeAndId> uniq = new HashSet<>(tcRaw.getResult());
        assertEquals(tcRaw.getResult().size(), uniq.size());

        // Must not lose any tuples in the non relieving path
        Set<String> uniqProcessed = new HashSet<>(tcProcessed.getResult());
        assertEquals(tcProcessed.getResult().size(), uniqProcessed.size());
        
        assertEquals(uniq.size(), uniqProcessed.size());
           
        // Might lose tuples, but must not have send duplicates
        uniq = new HashSet<>(tcSlow.getResult());
        assertEquals(tcSlow.getResult().size(), uniq.size());
    }
    
    @Test
    public void testPressureRelieverWithInitialDelay() throws Exception {

        Topology topology = newTopology();
        
        
        TStream<String> raw = topology.strings("A", "B", "C", "D", "E", "F", "G", "H");
        
        TStream<String> pr = PlumbingStreams.pressureReliever(raw, v -> 0, 100);
        
        TStream<String> pr2 = PlumbingStreams.blockingOneShotDelay(pr, 5, TimeUnit.SECONDS);
        
        Condition<Long> tcCount = topology.getTester().tupleCount(pr2, 8);
        Condition<List<String>> contents = topology.getTester().streamContents(pr2, "A", "B", "C", "D", "E", "F", "G", "H");
        complete(topology, tcCount);
        
        assertTrue(tcCount.valid());
        assertTrue(contents.valid());
    }
    
    @Test
    public void testValveState() throws Exception {
        Valve<Integer> valve = new Valve<>();
        assertTrue(valve.isOpen());
        
        valve = new Valve<>(true);
        assertTrue(valve.isOpen());
        
        valve = new Valve<>(false);
        assertFalse(valve.isOpen());
        
        valve.setOpen(true);
        assertTrue(valve.isOpen());
        
        valve.setOpen(false);
        assertFalse(valve.isOpen());
    }
    
    @Test
    public void testValveInitiallyOpen() throws Exception {
        Topology top = newTopology("testValve");

        TStream<Integer> values = top.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        Valve<Integer> valve = new Valve<>();
        AtomicInteger cnt = new AtomicInteger();
        TStream<Integer> filtered = values
                                    .peek(tuple -> {
                                        // reject 4,5,6
                                        int curCnt = cnt.incrementAndGet();
                                        if (curCnt > 6)
                                            valve.setOpen(true);
                                        else if (curCnt > 3)
                                            valve.setOpen(false);
                                        })
                                    .filter(valve);

        Condition<Long> count = top.getTester().tupleCount(filtered, 7);
        Condition<List<Integer>> contents = top.getTester().streamContents(filtered, 1,2,3,7,8,9,10 );
        complete(top, count);
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testValveInitiallyClosed() throws Exception {
        Topology top = newTopology("testValve");
        
        TStream<Integer> values = top.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        Valve<Integer> valve = new Valve<>(false);
        
        AtomicInteger cnt = new AtomicInteger();
        TStream<Integer> filtered = values
                                    .peek(tuple -> {
                                        // reject all but 4,5,6
                                        int curCnt = cnt.incrementAndGet();
                                        if (curCnt > 6)
                                            valve.setOpen(false);
                                        else if (curCnt > 3)
                                            valve.setOpen(true);
                                        })
                                    .filter(valve);

        Condition<Long> count = top.getTester().tupleCount(filtered, 3);
        Condition<List<Integer>> contents = top.getTester().streamContents(filtered, 4,5,6 );
        complete(top, count);
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    private Function<Integer,JsonObject> fakeAnalytic(int channel, long period, TimeUnit unit) {
      return value -> { 
        try {
          Thread.sleep(unit.toMillis(period));
          JsonObject jo = new JsonObject();
          jo.addProperty("channel", channel);
          jo.addProperty("result", value);
          return jo;
        } catch (InterruptedException e) {
          throw new RuntimeException("channel="+channel+" interrupted", e);
        }
      };
    }

    private Function<TStream<Integer>,TStream<JsonObject>> fakePipeline(int channel, long period, TimeUnit unit) {
      return stream -> stream.map(fakeAnalytic(channel, period, unit)).filter(t->true).tag("pipeline-ch"+channel);
    }
    
    @Test
    public void testConcurrentMap() throws Exception {
        Topology top = newTopology("testConcurrentMap");
        
        int ch = 0;
        List<Function<Integer,JsonObject>> mappers = new ArrayList<>();
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 100, TimeUnit.MILLISECONDS));
        // a couple much faster just in case something's amiss with queues
        mappers.add(fakeAnalytic(ch++, 3, TimeUnit.MILLISECONDS));
        mappers.add(fakeAnalytic(ch++, 13, TimeUnit.MILLISECONDS));
        
        Function<List<JsonObject>,Integer> combiner = list -> {
            int sum = 0;
            int cnt = 0;
            System.out.println("combiner: "+list);
            for(JsonObject jo : list) {
              assertEquals(cnt++, jo.get("channel").getAsInt());
              sum += jo.get("result").getAsInt();
            }
            return sum;
        };

        TStream<Integer> values = top.of(1, 2, 3);
        Integer[] resultTuples = new Integer[]{
            1*mappers.size(),
            2*mappers.size(),
            3*mappers.size(),
        };
        
        TStream<Integer> result = PlumbingStreams.concurrentMap(values, mappers, combiner);
        
        Condition<Long> count = top.getTester().tupleCount(result, 3);
        Condition<List<Integer>> contents = top.getTester().streamContents(result, resultTuples );

        long begin = System.currentTimeMillis();
        complete(top, count);
        long end = System.currentTimeMillis();

        assertTrue(contents.getResult().toString(), contents.valid());
        
        long actDuration = end - begin;
        long expMinSerialDuration = resultTuples.length * mappers.size() * 100;
        long expMinDuration = resultTuples.length * 100;
        
        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
        
        // a gross level performance check w/concurrent channels
        if (Boolean.getBoolean("edgent.build.ci"))
          System.err.println(top.getName()+" WARNING skipped performance check on 'ci' system use");
        else
          assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
              actDuration < 0.5 * expMinSerialDuration);
    }
    
    @Test
    public void testConcurrent() throws Exception {
        Topology top = newTopology("testConcurrent");
        
        int ch = 0;
        List<Function<TStream<Integer>,TStream<JsonObject>>> pipelines = new ArrayList<>();
        pipelines.add(fakePipeline(ch++, 100, TimeUnit.MILLISECONDS));
        pipelines.add(fakePipeline(ch++, 100, TimeUnit.MILLISECONDS));
        pipelines.add(fakePipeline(ch++, 100, TimeUnit.MILLISECONDS));
        pipelines.add(fakePipeline(ch++, 100, TimeUnit.MILLISECONDS));
        pipelines.add(fakePipeline(ch++, 100, TimeUnit.MILLISECONDS));
        
        Function<List<JsonObject>,Integer> combiner = list -> {
            int sum = 0;
            int cnt = 0;
            System.out.println("combiner: "+list);
            for(JsonObject jo : list) {
              assertEquals(cnt++, jo.get("channel").getAsInt());
              sum += jo.get("result").getAsInt();
            }
            return sum;
        };
        
        TStream<Integer> values = top.of(1, 2, 3);
        Integer[] resultTuples = new Integer[]{
            1*pipelines.size(),
            2*pipelines.size(),
            3*pipelines.size(),
        };
        
        TStream<Integer> result = PlumbingStreams.concurrent(values, pipelines, combiner).tag("result");
        
        Condition<Long> count = top.getTester().tupleCount(result, 3);
        Condition<List<Integer>> contents = top.getTester().streamContents(result, resultTuples );

        long begin = System.currentTimeMillis();
        complete(top, count);
        long end = System.currentTimeMillis();
        
        assertTrue(contents.getResult().toString(), contents.valid());
        
        long actDuration = end - begin;
        long expMinSerialDuration = resultTuples.length * pipelines.size() * 100;
        long expMinDuration = resultTuples.length * 100;
        
        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
        
        // a gross level performance check w/concurrent channels
        if (Boolean.getBoolean("edgent.build.ci"))
          System.err.println(top.getName()+" WARNING skipped performance check on 'ci' system use");
        else
          assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
              actDuration < 0.5 * expMinSerialDuration);
    }

    private BiFunction<Integer,Integer,JsonObject> fakeParallelAnalytic(long period, TimeUnit unit) {
      return (value,channel) -> { 
        try {
          Thread.sleep(unit.toMillis(period));  // simulate work for this period
          JsonObject jo = new JsonObject();
          jo.addProperty("channel", channel);
          jo.addProperty("result", value);
          return jo;
        } catch (InterruptedException e) {
          throw new RuntimeException("channel="+channel+" interrupted", e);
        }
      };
    }
    
    private BiFunction<TStream<Integer>,Integer,TStream<JsonObject>> fakeParallelPipeline(long period, TimeUnit unit) {
      return (stream,channel) -> stream
          .map(value -> fakeParallelAnalytic(period, unit).apply(value,channel))
          .filter(t->true)
          .tag("pipeline-ch"+channel);
    }
    
    private Function<JsonObject,JsonObject> fakeJsonAnalytic(int channel, long period, TimeUnit unit) {
      return jo -> { 
        try {
          Thread.sleep(unit.toMillis(period));  // simulate work for this period
          return jo;
        } catch (InterruptedException e) {
          throw new RuntimeException("channel="+channel+" interrupted", e);
        }
      };
    }
    
    @SuppressWarnings("unused")
    private BiFunction<TStream<JsonObject>,Integer,TStream<JsonObject>> fakeParallelPipelineTiming(long period, TimeUnit unit) {
      return (stream,channel) -> stream
          .map(jo -> { jo.addProperty("startPipelineMsec", System.currentTimeMillis());
                       return jo; })
          .map(fakeJsonAnalytic(channel, period, unit))
          .filter(t->true)
          .map(jo -> { jo.addProperty("endPipelineMsec", System.currentTimeMillis());
                      return jo; })
          .tag("pipeline-ch"+channel);
    }
    
    @Test
    public void testParallelMap() throws Exception {
        Topology top = newTopology("testParallelMap");
        
        BiFunction<Integer,Integer,JsonObject> mapper = 
            fakeParallelAnalytic(100, TimeUnit.MILLISECONDS);
        
        int width = 5;
        ToIntFunction<Integer> splitter = tuple -> tuple % width;
        
        Integer[] resultTuples = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        TStream<Integer> values = top.of(resultTuples);
        
        TStream<JsonObject> result = PlumbingStreams.parallelMap(values, width, splitter, mapper).tag("result");
        TStream<Integer> result2 = result.map(jo -> {
            int r = jo.get("result").getAsInt();
            assertEquals(splitter.applyAsInt(r), jo.get("channel").getAsInt());
            return r;
          });
        
        Condition<Long> count = top.getTester().tupleCount(result, resultTuples.length);
        Condition<List<Integer>> contents = top.getTester().contentsUnordered(result2, resultTuples);
    
        long begin = System.currentTimeMillis();
        complete(top, count);
        long end = System.currentTimeMillis();
        
        assertTrue(contents.getResult().toString(), contents.valid());
        
        long actDuration = end - begin;
        long expMinSerialDuration = resultTuples.length * 100;
        long expMinDuration = (resultTuples.length / width) * 100;
        
        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
        
        // a gross level performance check w/parallel channels
        if (Boolean.getBoolean("edgent.build.ci"))
          System.err.println(top.getName()+" WARNING skipped performance check on 'ci' system use");
        else
          assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
              actDuration < 0.5 * expMinSerialDuration);
    }
    
    @Test
    public void testParallel() throws Exception {
        Topology top = newTopology("testParallel");
        
        BiFunction<TStream<Integer>,Integer,TStream<JsonObject>> pipeline = 
            fakeParallelPipeline(100, TimeUnit.MILLISECONDS);
        
        int width = 5;
        ToIntFunction<Integer> splitter = tuple -> tuple % width;
        
        Integer[] resultTuples = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        TStream<Integer> values = top.of(resultTuples);
        
        TStream<JsonObject> result = PlumbingStreams.parallel(values, width, splitter, pipeline).tag("result");
        TStream<Integer> result2 = result.map(jo -> {
            int r = jo.get("result").getAsInt();
            assertEquals(splitter.applyAsInt(r), jo.get("channel").getAsInt());
            return r;
          });
        
        Condition<Long> count = top.getTester().tupleCount(result, resultTuples.length);
        Condition<List<Integer>> contents = top.getTester().contentsUnordered(result2, resultTuples);
        
        long begin = System.currentTimeMillis();
        complete(top, count);
        long end = System.currentTimeMillis();
        
        assertTrue(contents.getResult().toString(), contents.valid());
        
        long actDuration = end - begin;
        long expMinSerialDuration = resultTuples.length * 100;
        long expMinDuration = (resultTuples.length / width) * 100;
        
        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
        
        // a gross level performance check w/parallel channels
        if (Boolean.getBoolean("edgent.build.ci"))
          System.err.println(top.getName()+" WARNING skipped performance check on 'ci' system use");
        else
          assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
              actDuration < 0.5 * expMinSerialDuration);
    }
    
    @Test
    public void testParallelBalanced() throws Exception {
        Topology top = newTopology("testParallelBalanced");
        
        // arrange for even channels to process ~2x as many as odd channels.
        BiFunction<TStream<Integer>,Integer,TStream<JsonObject>> pipeline =
            (stream,ch) -> {
              long delay = (ch % 2 == 0) ? 10 : 20;
              return stream.map(fakeAnalytic(ch, delay, TimeUnit.MILLISECONDS));
            };
        
        int width = 4;
        int tupCnt = 60;
        int expEvenChCnt = 2 * (tupCnt / 3);   // even:2/3rds, odd:1/3rd 
        Integer[] resultTuples = new Integer[tupCnt];
        for (int i = 0; i < tupCnt; i++)
          resultTuples[i] = i;
        AtomicInteger[] chCounts = new AtomicInteger[width];
        for (int ch = 0; ch < width; ch++)
          chCounts[ch] = new AtomicInteger();
        
        TStream<Integer> values = top.of(resultTuples);
        
        TStream<JsonObject> result = PlumbingStreams.parallelBalanced(values, width, pipeline).tag("result");
        TStream<Integer> result2 = result.map(jo -> {
            int r = jo.get("result").getAsInt();
            int ch = jo.get("channel").getAsInt();
            chCounts[ch].incrementAndGet();
            return r;
          });
        
        Condition<Long> count = top.getTester().tupleCount(result, resultTuples.length);
        Condition<List<Integer>> contents = top.getTester().contentsUnordered(result2, resultTuples);
        
        long begin = System.currentTimeMillis();
        complete(top, count);
        long end = System.currentTimeMillis();
        
        assertTrue(contents.getResult().toString(), contents.valid());
        
        long actDuration = end - begin;
        long expMinSerialDuration = resultTuples.length * 20;
        long expMinDuration = (resultTuples.length / width) * 20;
        
        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
        System.out.println(top.getName()+" chCounts="+Arrays.asList(chCounts));
        
        // a gross level performance check w/parallel channels
        if (Boolean.getBoolean("edgent.build.ci"))
          System.err.println(top.getName()+" WARNING skipped performance check on 'ci' system use");
        else
          assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
              actDuration < 0.5 * expMinSerialDuration);
        
        // Verify the balancing seemed to work.
        // On loaded systems we've seen eCnt:37 oCnt:23.  Settle for expEvenCnt +- 15%
        double thresholdCnt = expEvenChCnt * 0.15;
        int evenChCnt = 0;
        for (int ch = 0; ch < width; ch++) {
          assertTrue("ch:"+ch, chCounts[ch].get() != 0);
          if (ch % 2 == 0)
            evenChCnt += chCounts[ch].get();
        }
        assertTrue(
                String.format("evenChCnt:%d expEvenChCnt:%d +-:%d", evenChCnt, expEvenChCnt, thresholdCnt),
                evenChCnt >= (expEvenChCnt - thresholdCnt)
                && evenChCnt <= (expEvenChCnt + thresholdCnt)); 
    }
    
//    @Test
//    public void testParallelTiming() throws Exception {
//        Topology top = newTopology("testParallelTiming");
//        
//        BiFunction<TStream<JsonObject>,Integer,TStream<JsonObject>> pipeline = 
//            fakeParallelPipelineTiming(100, TimeUnit.MILLISECONDS);
//        
//        int width = 5;
//        // ToIntFunction<Integer> splitter = tuple -> tuple % width;
//        ToIntFunction<JsonObject> splitter = jo -> jo.get("result").getAsInt() % width;
//        
//        Integer[] resultTuples = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
//        TStream<Integer> values = top.of(resultTuples);
//        
//        TStream<JsonObject> inStream = values.map(value -> {
//            JsonObject jo = new JsonObject();
//            jo.addProperty("result", value);
//            jo.addProperty("channel", splitter.applyAsInt(jo));
//            jo.addProperty("enterParallelMsec", System.currentTimeMillis());
//            return jo;
//          });
//        TStream<JsonObject> result = PlumbingStreams.parallel(inStream, width, splitter, pipeline).tag("result");
//        TStream<Integer> result2 = result.map(jo -> {
//            jo.addProperty("exitParallelMsec", System.currentTimeMillis());
//            System.out.println("ch="+jo.get("channel").getAsInt()
//                +" endPipeline-startPipeline="
//                  +(jo.get("endPipelineMsec").getAsLong()
//                    - jo.get("startPipelineMsec").getAsLong())
//                +" exitParallel-startPipeine="
//                  +(jo.get("exitParallelMsec").getAsLong()
//                      - jo.get("startPipelineMsec").getAsLong()));
//            int r = jo.get("result").getAsInt();
//            assertEquals(splitter.applyAsInt(jo), jo.get("channel").getAsInt());
//            return r;
//          });
//        
//        Condition<Long> count = top.getTester().tupleCount(result, resultTuples.length);
//        Condition<List<Integer>> contents = top.getTester().contentsUnordered(result2, resultTuples);
//        long begin = System.currentTimeMillis();
//        complete(top, count);
//        long end = System.currentTimeMillis();
//        assertTrue(contents.getResult().toString(), contents.valid());
//        
//        long actDuration = end - begin;
//        
//        long expMinSerialDuration = resultTuples.length * 100;
//        long expMinDuration = (resultTuples.length / width) * 100;
//        
//        System.out.println(top.getName()+" expMinDuration="+expMinDuration+" actDuration="+actDuration+" expMinSerialDuration="+expMinSerialDuration);
//        
//        // a gross level performance check w/parallel channels
//        assertTrue("expMinSerialDuration="+expMinSerialDuration+" actDuration="+actDuration, 
//            actDuration < 0.5 * expMinSerialDuration);
//    }

    @Test
    public void testGate() throws Exception {
        Topology topology = newTopology("testGate");

        TStream<String> raw = topology.strings("a", "b", "c", "d", "e");

        Semaphore semaphore = new Semaphore(1);
        raw = PlumbingStreams.gate(raw, semaphore);

        ArrayList<Integer> resultAvailablePermits = new ArrayList<>();
        ArrayList<Integer> arrayResult = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            arrayResult.add(0);
            arrayResult.add(1);
        }

        raw.sink(t -> {
            //Add 0 to list because semaphore.acquire() in sync has occurred
            resultAvailablePermits.add(semaphore.availablePermits());
            semaphore.release();
            //Add 1 to list because semaphore.release() has executed
            resultAvailablePermits.add(semaphore.availablePermits());
        });

        Condition<List<String>> contents = topology.getTester()
            .streamContents(raw, "a", "b", "c", "d", "e");
        complete(topology, contents);

        assertTrue("valid:" + contents.getResult(), contents.valid());
        assertTrue("valid:" + resultAvailablePermits, resultAvailablePermits.equals(arrayResult));
    }

    @Test
    public void testGateWithLocking() throws Exception {
        Topology topology = newTopology("testGateWithLocking");

        TStream<String> raw = topology.strings("a", "b", "c", "d", "e");

        Semaphore semaphore = new Semaphore(3);
        raw = PlumbingStreams.gate(raw, semaphore);

        ArrayList<Integer> resultAvailablePermits = new ArrayList<>();
        ArrayList<Integer> arrayResult = new ArrayList<>();
        arrayResult.add(2);
        arrayResult.add(1);
        arrayResult.add(0);

        raw.sink(t -> {
            //Add number of availablePermits
            resultAvailablePermits.add(semaphore.availablePermits());
        });

        Condition<List<String>> contents = topology.getTester().streamContents(raw, "a", "b", "c");
        complete(topology, contents, 1000, TimeUnit.MILLISECONDS);

        assertTrue("valid:" + contents.getResult(), contents.valid());
        assertTrue("valid:" + resultAvailablePermits, resultAvailablePermits.equals(arrayResult));
    }

}
