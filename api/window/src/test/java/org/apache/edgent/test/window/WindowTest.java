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
package org.apache.edgent.test.window;

import static org.apache.edgent.function.Functions.unpartitioned;
import static org.apache.edgent.window.Policies.alwaysInsert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.window.InsertionTimeList;
import org.apache.edgent.window.Policies;
import org.apache.edgent.window.Window;
import org.apache.edgent.window.Windows;
import org.junit.Test;


public class WindowTest {
	
    /**
     * Verifies that the state of the window is correct after each tuple offer.
     */
    @Test
    public void lastCountTest(){
        final int COUNT = 100;
        // The window implementation
        Window<Integer, Integer, ? extends List<Integer>> window = Windows.lastNProcessOnInsert(10, unpartitioned());
        // The states of the window as it slides
        LinkedList<List<Integer> > incrementalWindowStates = new LinkedList<>();
        
        // A processor that records the states of the window
        BiConsumer<List<Integer>, Integer> wp = (tuples, key) -> {
            incrementalWindowStates.addLast(new LinkedList<Integer>(tuples));
        };
        window.registerPartitionProcessor(wp);
        
        // Generate sliding window correct incremental state to compare
        // against the window's
        LinkedList<LinkedList<Integer>> correctWindowStates = new LinkedList<>();
        LinkedList<Integer> previous = null;
        LinkedList<Integer> current = null;
        for(int i = 0; i < COUNT; i++){
            current = new LinkedList<>();         
            if(previous != null)
                current.addAll(previous);
            
            current.addLast(i);
            if(current.size() > 10){
                current.removeFirst();
            }
            previous = current;
            correctWindowStates.addLast(current);              
        }
        
        // Add tuples to window, populating the incrementalWindowStates list.
        for(int i = 0; i < COUNT; i++){
            window.insert(i);
        }

        // Compare correct window states to the window implementation's
        assertTrue(correctWindowStates.size() == incrementalWindowStates.size());
        for(int i = 0; i < correctWindowStates.size(); i++){
            assertTrue(correctWindowStates.get(i).containsAll(incrementalWindowStates.get(i)));
            assertTrue(incrementalWindowStates.get(i).containsAll(correctWindowStates.get(i)));
        }
    }
    
    @Test
    public void keyedWindowTest(){
        final int COUNT = 1000;
        // The window implementation
     // The window implementation
        Window<Integer, Integer, ? extends List<Integer>> window = Windows.lastNProcessOnInsert(10, tuple->tuple%10);
        
        
        // The states of the window as it slides
        LinkedList<List<Integer> > incrementalWindowStates = new LinkedList<>();
        
        // A processor that records the states of the window
        BiConsumer<List<Integer>, Integer> wp = (tuples, key) -> {
            incrementalWindowStates.addLast(new LinkedList<Integer>(tuples));
        };    
        window.registerPartitionProcessor(wp); 
        
        Map<Integer, LinkedList<Integer>> correctPartitionedStates = new HashMap<>();
        List<List<Integer> > correctWindowStates = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            correctPartitionedStates.put(i, new LinkedList<>());
        }
        for(int i = 0; i < COUNT; i++){
            correctPartitionedStates.get(i%10).add(i);
            if(correctPartitionedStates.get(i%10).size() > 10){
                correctPartitionedStates.get(i%10).removeFirst();
            }
            correctWindowStates.add(new ArrayList<>(correctPartitionedStates.get(i%10)));
            window.insert(i);
        }
        
        // Compare correct window states to the window implementation's
        assertTrue(correctWindowStates.size() == incrementalWindowStates.size());
        for(int i = 0; i < correctWindowStates.size(); i++){
            assertTrue(correctWindowStates.get(i).containsAll(incrementalWindowStates.get(i)));
            assertTrue(incrementalWindowStates.get(i).containsAll(correctWindowStates.get(i)));
        }    
    }
    
    @Test
    public void accessPartitionKeyTest(){
        LinkedList<List<Integer> > incrementalWindowStates = new LinkedList<>();
        
        Window<Integer, Integer, ? extends List<Integer>> window = Windows.<Integer, Integer, LinkedList<Integer>>window(
        (partition, tuple) -> {
            if (partition.getKey().equals(1) || partition.getKey().equals(3)) {
                return false;
            }
            return true;
        },
        (partition, tuple) -> { // Contents policy

                }, 
        (partition) -> { // Evict determiner
                    partition.getContents().clear();
                }, 
        Policies.processOnInsert(), 
        tuple -> tuple, 
        () -> new LinkedList<Integer>());

        // A processor that records the states of the window
        BiConsumer<List<Integer>, Integer> wp = (tuples, key) -> {
            incrementalWindowStates.addLast(new LinkedList<Integer>(tuples));
        };    
        window.registerPartitionProcessor(wp); 
        
        
        for(Integer i = 0; i < 5; i++){
            window.insert(i);
        }
        
        assertTrue(incrementalWindowStates.size() == 3);
        assertTrue(incrementalWindowStates.get(0).get(0)==0);
        assertTrue(incrementalWindowStates.get(1).get(0)==2);
        assertTrue(incrementalWindowStates.get(2).get(0)==4);
        
    }
    
    @Test
    public void concurrentWindowAccessTest() throws InterruptedException {
       
        Window<Integer, Integer, ? extends List<Integer>> window = Windows.lastNProcessOnInsert(10, tuple -> 0);
        
        window.registerPartitionProcessor((tuples, key) -> {
            // ensure that the window state doesn't change after .05 seconds
            // Copy window state
            LinkedList<Integer> list_copy = new LinkedList<Integer>(tuples);
            
            // Wait .05 seconds
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Verify that the partition is unchanged
            assertTrue(list_copy.containsAll(tuples));
            assertTrue(tuples.containsAll(list_copy));
        });
           
        
        // Run for five seconds.
        long finishTime = System.currentTimeMillis() + 3000;
        
        List<Thread> threads = new ArrayList<Thread>();
        
        // Ten threads concurrently attempt to insert tuples into the window
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Runnable(){
                Random r = new Random();
                @Override
                public void run() {
                    
                    while(System.currentTimeMillis() < finishTime){                       
                        try{
                            window.insert(r.nextInt());
                        }
                        catch(ConcurrentModificationException cme){
                            org.junit.Assert.fail("State of window changed while processing");
                        }
                    }  
                }
                
            }));
        }
        for(Thread thread : threads){
            thread.start();
            Thread.sleep(10);
        }
        for(Thread thread : threads)
            thread.join();
    }
    
    
    @Test
    public void noWaitConcurrentWindowAccessTest() throws InterruptedException {
        Window<Integer, Integer, ? extends List<Integer>> window = Windows.lastNProcessOnInsert(100, tuple -> 0);
        window.registerPartitionProcessor((tuples, key) -> {});
        long finishTime = System.currentTimeMillis() + 3000;
        
        List<Thread> threads = new ArrayList<Thread>();
        
        // Ten threads concurrently attempt to insert tuples into the window
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Runnable(){
                Random r = new Random();
                @Override
                public void run() {
                    while(System.currentTimeMillis() < finishTime){                       
                        try{
                            window.insert(r.nextInt());
                        }
                        catch(ConcurrentModificationException cme){
                            org.junit.Assert.fail("State of window changed while processing");
                        }
                    }  
                }
                
            }));
        }
        for(Thread thread : threads){
            thread.start();
            Thread.sleep(10);
        }
        for(Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void timeActionTest() throws InterruptedException {
		// Timing variances on shared machines can cause this test to fail
    	assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        List<Long> diffs = Collections.synchronizedList(new ArrayList<>());
        List<Boolean> initialized = Collections.synchronizedList(new ArrayList<>());
        initialized.add(false);
        
        Window<Long, Integer, InsertionTimeList<Long>> window = // new TimeWindowImpl<Long, Integer, LinkedList<Long>>(
        Windows.window(
                Policies.alwaysInsert(), // insertion policy
                Policies.scheduleEvictIfEmpty(1000, TimeUnit.MILLISECONDS), 
                // Policies.evictOlderThan(1000, TimeUnit.MILLISECONDS), 
                Policies.evictOlderWithProcess(1000, TimeUnit.MILLISECONDS), 
                (partition, tuple) -> {
                    if(initialized.get(0).booleanValue() == false){
                        initialized.set(0, true);
                        ScheduledExecutorService ses = partition.getWindow().getScheduledExecutorService();
                        ses.scheduleAtFixedRate(() -> {partition.process();}, 0, 1000, TimeUnit.MILLISECONDS);
                    }}, 
                unpartitioned(),
                () -> new InsertionTimeList<Long>());
        
        window.registerPartitionProcessor((tuples, key) -> {
            if(tuples.size() > 1)
                diffs.add(tuples.get(tuples.size()-1) - tuples.get(0));
        });
        
        window.registerScheduledExecutorService(new ScheduledThreadPoolExecutor(5));
        
        long endTime = System.currentTimeMillis() + 4000;
        List<Thread> threads = new ArrayList<>();
        int NUM_THREADS = 10;
        // Create 10 threads. Each inserts at 1,000 Hz
        for(int i = 0; i < NUM_THREADS; i++){
            threads.add(new Thread(new Runnable() {     
                @Override
                public void run() {
                    while(System.currentTimeMillis() < endTime){
                        window.insert(System.currentTimeMillis());
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }));
        }
        for(int i = 0; i < NUM_THREADS; i++){
            threads.get(i).start();
        }
        for(int i = 0; i < NUM_THREADS; i++){
            threads.get(i).join();
        }
        assertOnTimeEvictions(diffs);
        
    }
    
    @SuppressWarnings("serial")
    @Test
    public void countBatchWindowTest(){
        List<Integer> numBatches = new LinkedList<>();
        Window<Integer, Integer, List<Integer>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.doNothing(),
                        Policies.evictAll(),
                        Policies.processWhenFullAndEvict(113),
                        tuple -> 0,
                        () -> new ArrayList<Integer>());
        window.registerPartitionProcessor(new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> t, Integer u) {
                numBatches.add(1);
            }
        });
        for(int i = 0; i < 1000; i++){
            window.insert(i);
        }
        
        assertTrue(numBatches.size() == 8);
    }

    @SuppressWarnings("serial")
    @Test
    public void timeBatchWindowTest() throws InterruptedException{
        List<Long> numBatches = new LinkedList<>();
        
        ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(5);
        Window<Integer, Integer, List<Integer>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.scheduleEvictOnFirstInsert(1, TimeUnit.SECONDS),
                        Policies.evictAllAndScheduleEvictWithProcess(1, TimeUnit.SECONDS),
                        (partiton, tuple) -> {},
                        tuple -> 0,
                        () -> new ArrayList<Integer>());
        
        window.registerPartitionProcessor(new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> t, Integer u) {
                numBatches.add((long)t.size());
            }
        });
        
        window.registerScheduledExecutorService(new ScheduledThreadPoolExecutor(5));

        ScheduledFuture<?> sf = ses.scheduleAtFixedRate(() -> {
            window.insert(1);
        }, 0, 10, TimeUnit.MILLISECONDS);

        Thread.sleep(4000);
        sf.cancel(true);
        double tolerance = .08;
        for(int i = 0; i < numBatches.size(); i++){
            assertTrue("Values:" + numBatches.toString(), withinTolerance(100.0, numBatches.get(i).doubleValue(), tolerance));
        }    
    }
    
    @SuppressWarnings("serial")
    @Test
    public void keyedTimeBatchWindowTest() throws InterruptedException{
        Map<Integer, List<Long> > numBatches = Collections.synchronizedMap(new HashMap<>());
        for(int i = 0; i < 5; i++)
        	numBatches.put(i, new LinkedList<Long>());
        
        ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(5);
        Window<Integer, Integer, List<Integer>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.scheduleEvictOnFirstInsert(1, TimeUnit.SECONDS),
                        Policies.evictAllAndScheduleEvictWithProcess(1, TimeUnit.SECONDS),
                        (partiton, tuple) -> {},
                        tuple -> tuple,
                        () -> new ArrayList<Integer>());
        
        window.registerPartitionProcessor(new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> t, Integer u) {
            	List<Long> l = numBatches.get(u);
                l.add((long)t.size());
            }
        });
        
        window.registerScheduledExecutorService(new ScheduledThreadPoolExecutor(5));

        ScheduledFuture<?> sf = ses.scheduleAtFixedRate(() -> {
        	for(int i = 0; i < 5; i++)
        		window.insert(i);
        }, 0, 1, TimeUnit.MILLISECONDS);

        Thread.sleep(4000);
        sf.cancel(true);
        try {
          sf.get();
        } catch (Exception e) {
          // expected
        }
        double tolerance = .12;
        
        for(Integer key : numBatches.keySet()){
        	List<Long> batch = numBatches.get(key);
        	for(Long l : batch){
        		assertTrue("Values:" + batch.toString(), withinTolerance(1000.0, l.doubleValue(), tolerance));
        	}
        }
    }
   
    
    @SuppressWarnings("serial")
    @Test
    public void timeBatchEnsureUnique() throws InterruptedException{
        List<List<Integer>> batches = Collections.synchronizedList(new LinkedList<>());

        ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(5);
        Window<Integer, Integer, List<Integer>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.scheduleEvictOnFirstInsert(1, TimeUnit.SECONDS),
                        Policies.evictAllAndScheduleEvictWithProcess(1, TimeUnit.SECONDS),
                        (partiton, tuple) -> {},
                        tuple -> 0,
                        () -> new ArrayList<Integer>());
        
        window.registerPartitionProcessor(new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> t, Integer u) {
                batches.add(new ArrayList<Integer>(t));
            }
        });
        
        window.registerScheduledExecutorService(new ScheduledThreadPoolExecutor(5));

        AtomicInteger count = new AtomicInteger();
        int MAX_TUP_CNT = 300;
        ScheduledFuture<?> sf = ses.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                if(count.get() < MAX_TUP_CNT){
                    window.insert(count.incrementAndGet());
                }
            }
            
        }, 0, 10, TimeUnit.MILLISECONDS);

        long insertMsec = MAX_TUP_CNT * 10 /*10msec/tup*/;
        Thread.sleep(insertMsec + 1000/*extra sec*/);
        sf.cancel(true);
        assertEquals("Invalid test", MAX_TUP_CNT, count.get());
        int numTuples = 0;
        for(int i = 0; i < batches.size() - 1; i++){
            List<Integer> batch = batches.get(i);
            numTuples += batch.size();
            for(int j = i + 1; j < batches.size(); j++){
                assertTrue("Batches have overlapping tuples", Collections.disjoint(batches.get(i), batches.get(j)));
            }
        }
        
        numTuples += batches.get(batches.size() -1).size();
        assertEquals("Number of batch tuples", count.get(), numTuples);
        assertTrue("Number of batches exp:"+MAX_TUP_CNT/100+" got:"+batches.size(),
                withinToleranceAmt((double)MAX_TUP_CNT/100, (double)batches.size(), 1)); // +/- 1
    }
    
    private void assertOnTimeEvictions(List<Long> diffs) {
        double tolerance = .08;
        for(int i = 1; i < diffs.size(); i++){
            assertTrue(withinTolerance(1000.0, diffs.get(i).doubleValue(), tolerance));
        }
        
    }

    public static boolean withinTolerance(double expected, Double actual, double tolerance) {
        double lowBound = (1.0 - tolerance) * expected;
        double highBound = (1.0 + tolerance) * expected;
        return (actual < highBound && actual > lowBound);
    }

    public static boolean withinToleranceAmt(double expected, Double actual, double toleranceAmt) {
        double lowBound = expected - toleranceAmt;
        double highBound = expected + toleranceAmt;
        return (actual < highBound && actual > lowBound);
    }

}
