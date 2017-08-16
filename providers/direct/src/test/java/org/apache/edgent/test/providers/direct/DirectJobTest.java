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
package org.apache.edgent.test.providers.direct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.execution.Configs;
import org.apache.edgent.execution.Job;
import org.apache.edgent.graph.Vertex;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.oplet.core.PeriodicSource;
import org.apache.edgent.oplet.core.Pipe;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.junit.Test;

import com.google.gson.JsonObject;

public class DirectJobTest extends DirectTopologyTestBase {
    @Test
    public void jobName0() throws Exception {
        String[] data = new String[] {};
        String topologyName = "topoName";
        Topology t = newTopology(topologyName);
        t.strings(data);
        Job job = awaitCompleteExecution(t);
        assertTrue(job.getName().startsWith(topologyName));
    }

    @Test
    public void jobName1() throws Exception {
        String[] data = new String[] {};
        String topologyName = "topoName";
        Topology t = newTopology(topologyName);
        t.strings(data);
        JsonObject config = new JsonObject();
        config.addProperty(Configs.JOB_NAME, (String)null);
        Job job = awaitCompleteExecution(t, config);
        assertTrue(job.getName().startsWith(topologyName));
    }

    @Test
    public void jobName2() throws Exception {
        String[] data = new String[] {};
        String jobName = "testJob";
        Topology t = newTopology();
        t.strings(data);
        JsonObject config = new JsonObject();
        config.addProperty(Configs.JOB_NAME, jobName);
        Job job = awaitCompleteExecution(t, config);
        assertEquals(jobName, job.getName());
    }

    @Test
    public void jobDone0() throws Exception {
        String[] data = new String[] {};
        Topology t = newTopology();
        @SuppressWarnings("unused")
        TStream<String> s = t.strings(data);

        Job job = awaitCompleteExecution(t);
        assertEquals("job.getCurrentState() must be RUNNING", Job.State.RUNNING, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());

        job.stateChange(Job.Action.CLOSE);
        assertEquals("job.getCurrentState() must be CLOSED", Job.State.CLOSED, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
    }

    @Test
    public void jobDone1() throws Exception {
        String[] data = new String[] {"a", "b", "c"};
        Topology t = newTopology();
        @SuppressWarnings("unused")
        TStream<String> s = t.strings(data);

        Job job = awaitCompleteExecution(t);
        assertEquals("job.getCurrentState() must be RUNNING", Job.State.RUNNING, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
        job.stateChange(Job.Action.CLOSE);
        assertEquals("job.getCurrentState() must be CLOSED", Job.State.CLOSED, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
    }

    @Test
    public void jobDone2() throws Exception {
        final int NUM_TUPLES = 1000000;
        Integer[] data = new Integer[NUM_TUPLES];
        AtomicInteger numTuples = new AtomicInteger();

        for (int i = 0; i < data.length; i++) {
            data[i] = new Integer(i);
        }
        Topology t = newTopology();
        TStream<Integer> ints = t.collection(Arrays.asList(data));
        ints.sink(tuple -> numTuples.incrementAndGet());

        Job job = awaitCompleteExecution(t);
        Thread.sleep(1500); // wait for numTuples visibility 
        assertEquals(NUM_TUPLES, numTuples.get());
        assertEquals("job.getCurrentState() must be RUNNING", Job.State.RUNNING, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
        job.stateChange(Job.Action.CLOSE);
        assertEquals("job.getCurrentState() must be CLOSED", Job.State.CLOSED, job.getCurrentState());
        assertEquals("job.getCurrentState() must be HEALTHY", Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
    }

    @Test
    public void jobPeriodicSource() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        @SuppressWarnings("unused")
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 100, TimeUnit.MILLISECONDS);

        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        assertTrue(n.get() > 0); // At least one tuple was processed
        job.stateChange(Job.Action.CLOSE);
        assertEquals(Job.State.CLOSED, job.getCurrentState());
    }

    @Test
    public void jobPeriodicSourceCancellation() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        @SuppressWarnings("unused")
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 500, TimeUnit.MILLISECONDS);

        // Get the source oplet
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = t.graph().getVertices();
        PeriodicSource<?> src = null;
        for (Vertex<? extends Oplet<?, ?>, ?, ?> v : vertices) {
            Oplet<?,?> op = v.getInstance();
            assertTrue(op instanceof PeriodicSource);
            src = (PeriodicSource<?>) op;
            assertEquals(500, src.getPeriod());
            assertSame(TimeUnit.MILLISECONDS, src.getUnit());
        }
        
        // Submit job
        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        int tupleCount = n.get(); 
        assertTrue("Expected more tuples than "+ tupleCount, tupleCount > 0); // At least one tuple was processed
        assertEquals(Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());

        // Changing the period cancels the source's task and schedules new one
        src.setPeriod(100); 

        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        
        // More tuples processed after resetting the period
        assertTrue("Expected more tuples than "+ n.get(), n.get() > 3*tupleCount);

        job.stateChange(Job.Action.CLOSE);
        assertEquals(Job.State.CLOSED, job.getCurrentState());
        assertEquals(Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
    }

    @Test
    public void jobProcessSource() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        @SuppressWarnings("unused")
        TStream<Integer> ints = t.generate(() -> n.incrementAndGet());

        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        assertEquals(Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        assertTrue(n.get() > 0); // At least one tuple was processed
        job.stateChange(Job.Action.CLOSE);
        assertEquals(Job.State.CLOSED, job.getCurrentState());
        assertEquals(Job.Health.HEALTHY, job.getHealth());
        assertEquals("", job.getLastError());
    }

    @Test(expected = TimeoutException.class)
    public void jobTimesOut() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        @SuppressWarnings("unused")
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 100, TimeUnit.MILLISECONDS);

        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        try {
            job.complete(700, TimeUnit.MILLISECONDS);
        } finally {
            assertTrue(n.get() > 0); // At least one tuple was processed
            assertEquals(Job.State.RUNNING, job.getCurrentState());
            assertEquals(Job.Health.HEALTHY, job.getHealth());
            assertEquals("", job.getLastError());
        }
    }

    @Test(expected = ExecutionException.class)
    public void jobPeriodicSourceError() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 100, TimeUnit.MILLISECONDS);
        ints.pipe(new FailedOplet<Integer>(5, 0));
        
        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        try {
            job.complete(10, TimeUnit.SECONDS); 
        } finally {
            // RUNNING even though execution error 
            assertEquals(Job.State.RUNNING, job.getCurrentState());
            assertEquals(Job.Health.UNHEALTHY, job.getHealth());
            assertEquals("java.lang.RuntimeException: Expected Test Exception", job.getLastError());
        }
    }

    @Test(expected = ExecutionException.class)
    public void jobProcessSourceError() throws Exception {
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        TStream<Integer> ints = t.generate(() -> n.incrementAndGet());
        ints.pipe(new FailedOplet<Integer>(12, 100));

        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t);
        Job job = fj.get();
        assertEquals(Job.State.RUNNING, job.getCurrentState());
        try {
            job.complete(10, TimeUnit.SECONDS); 
        } finally {
            // RUNNING even though execution error 
            assertEquals(Job.State.RUNNING, job.getCurrentState());
            assertEquals(Job.Health.UNHEALTHY, job.getHealth());
            assertEquals("java.lang.RuntimeException: Expected Test Exception", job.getLastError());
        }
    }

    private Job awaitCompleteExecution(Topology t) throws InterruptedException, ExecutionException {
        return awaitCompleteExecution(t, null);
    }

    private Job awaitCompleteExecution(Topology t, JsonObject config) throws InterruptedException, ExecutionException {
        Future<Job> fj = ((DirectProvider)getTopologyProvider()).submit(t, config);
        Job job = fj.get();
        job.complete();
        return job;
    }

    /**
     * Test oplet which fails after receiving a given number of tuples.
     * @param <T>
     */
    @SuppressWarnings("serial")
    private static class FailedOplet<T> extends Pipe<T,T> {
        private final int threshold;
        private final int sleepMillis;
        
        /**
         * Create test oplet.
         * 
         * @param afterTuples number of tuples to receive before failing 
         * @param sleepMillis milliseconds of sleep before processing each tuple
         */
        public FailedOplet(int afterTuples, int sleepMillis) {
            if (afterTuples < 0)
                throw new IllegalArgumentException("afterTuples="+afterTuples);
            if (sleepMillis < 0)
                throw new IllegalArgumentException("sleepMillis="+sleepMillis);
            this.threshold = afterTuples;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public void close() throws Exception {
        }
        @Override
        public void accept(T tuple) {
            if (sleepMillis > 0) {
                sleep(sleepMillis);
            }
            injectError(threshold); 
            submit(tuple);
        }

        private AtomicInteger count = new AtomicInteger(0);
        protected void injectError(int errorAt) {
            if (count.getAndIncrement() == errorAt)
                throw new RuntimeException("Expected Test Exception");
        }
        
        protected static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
