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
package quarks.test.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import quarks.execution.DirectSubmitter;
import quarks.execution.Job;
import quarks.graph.Edge;
import quarks.graph.Graph;
import quarks.graph.Vertex;
import quarks.metrics.Metrics;
import quarks.metrics.MetricsSetup;
import quarks.oplet.Oplet;
import quarks.oplet.OpletContext;
import quarks.oplet.core.Peek;
import quarks.test.topology.TopologyAbstractTest;
import quarks.topology.TStream;
import quarks.topology.Topology;

@Ignore("abstract, provides common tests for concrete implementations")
public abstract class MetricsEverywhereTest extends TopologyAbstractTest {

    protected MetricRegistry metricRegistry;

    // Register Metrics service before each test.
    @Before
    public void createMetricRegistry() {
        metricRegistry = new MetricRegistry();
        MetricsSetup.withRegistry(((DirectSubmitter<?,?>)getSubmitter()).getServices(), metricRegistry);
    }

    /*
     * Test that Metrics are automatically unregistered after the job is closed
     */
    @Test
    public void automaticMetricCleanup1() throws Exception {
        // Declare topology with custom metric oplet
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 10, TimeUnit.MILLISECONDS);
        ints.peek(new TestOplet<Integer>());

        // Submit job
        Future<? extends Job> fj = getSubmitter().submit(t);
        Job job = fj.get();
        Thread.sleep(TimeUnit.MILLISECONDS.toMillis(50));

        // At least one tuple was processed
        int tupleCount = n.get(); 
        assertTrue("Expected more tuples than "+ tupleCount, tupleCount > 0);

        // Each test oplet registers two metrics 
        Map<String, Metric> all = metricRegistry.getMetrics();
        assertEquals(2, all.size());
        
        // After close all metrics have been unregistered 
        job.stateChange(Job.Action.CLOSE);
        assertEquals(0, all.size());
    }

    /*
     * Test that Metrics are automatically unregistered after the job is closed
     * in a topology with two oplets registering metrics.
     */
    @Test
    public void automaticMetricCleanup2() throws Exception {
        // Declare topology with custom metric oplet
        Topology t = newTopology();
        AtomicInteger n = new AtomicInteger(0);
        TStream<Integer> ints = t.poll(() -> n.incrementAndGet(), 10, TimeUnit.MILLISECONDS);
        TStream<Integer> ints2 = ints.peek(new TestOplet<Integer>());
        ints2.peek(new TestOplet<Integer>());

        // Submit job
        Future<? extends Job> fj = getSubmitter().submit(t);
        Job job = fj.get();
        Thread.sleep(TimeUnit.MILLISECONDS.toMillis(50));

        // Each test oplet registers two metrics 
        Map<String, Metric> all = metricRegistry.getMetrics();
        assertEquals(4, all.size());
        
        // After close all metrics have been unregistered 
        job.stateChange(Job.Action.CLOSE);
        assertEquals(0, all.size());
    }

    // Apply Metrics on all streams, simple graph
    @Test
    public void metricsEverywhereSimple() throws Exception {
        
        Topology t = newTopology();
        Graph g = t.graph();

        // Source
        TStream<Integer> d = integers(t, 1, 2, 3);
        d.sink(tuple -> System.out.print("."));
        
        // Insert counter metrics into all the topology streams 
        Metrics.counter(t);

        printGraph(g);
        
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        assertEquals(3, vertices.size());
        
        Collection<Edge> edges = g.getEdges();
        assertEquals(2, edges.size());
    }

    // Apply Metrics on all streams, graph with split oplet and Metric oplet
    // Counter oplets are inserted everywhere except after OP_2 (Counter).
    @Test
    public void metricsEverywhereSplit() throws Exception {
        
        /*                        -- OP_2 (Counter) --- OP_3 (Sink)
         *                       / 
         * OP_0 -- OP_1(Split) ----- OP_4 (Sink)
         *                       \
         *                        -- OP_5 (Sink)
         *                        
         * Note, OP_2 (Counter) is a peek oplet and as such
         * is part of the split[0] stream's peek-chain.  
         * Metrics insertion only occurs at the end of a peek-chain,
         * so there is NOT an injected metric between 
         * OP_1(Split) -> OP_2 (Counter).
         * 
         * The net is metrics are injected where the "#" are:
         * 
         *                        -- OP_2 (Counter) -#- OP_3 (Sink)
         *                       / 
         * OP_0 -#- OP_1(Split) ---#- OP_4 (Sink)
         *                       \
         *                        -#- OP_5 (Sink)
         */
        Topology t = newTopology();
        Graph g = t.graph();

        // Source
        TStream<Integer> d = integers(t, 1, 2, 3);

        // Split
        List<TStream<Integer>> splits = d.split(3, tuple -> {
            switch (tuple.intValue()) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return 2;
            }
        });

        // Insert counter metric for the zeroes stream
        Metrics.counter(splits.get(0)).sink(tuple -> System.out.print("."));

        splits.get(1).sink(tuple -> System.out.print("#"));
        splits.get(2).sink(tuple -> System.out.print("@"));

        // Insert counter metrics into all the topology streams 
        Metrics.counter(t);

        printGraph(g);

        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        assertEquals(10, vertices.size());

        Collection<Edge> edges = g.getEdges();
        assertEquals(9, edges.size());
    }

    @Test
    public void metricsEverywhereFanOut() {
        
        Topology t = newTopology();
        Graph g = t.graph();

        /*                   -- OP_3 (Sink)
         *                  / 
         * OP_0 -- FanOut ----- OP_4 (Sink)
         */
        TStream<Integer> d = integers(t, 1, 2, 3);
        d.sink(tuple -> System.out.print("."));
        d.sink(tuple -> System.out.print("@"));
        
        // Insert counter metrics into all the topology streams 
        Metrics.counter(t);

        printGraph(g);
        
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        assertEquals(5, vertices.size());
        
        Collection<Edge> edges = g.getEdges();
        assertEquals(4, edges.size());
    }

    /**
     * Test Peek. This will only work with an embedded setup.
     * 
     * @throws Exception
     */
    @Test
    public void metricsEverywherePeek() throws Exception {

        Topology t = newTopology();
        Graph g = t.graph();

        TStream<String> s = t.strings("a", "b", "c");
        List<String> peekedValues = new ArrayList<>();
        TStream<String> speek = s.peek(tuple -> peekedValues.add(tuple));
        speek.sink(tuple -> System.out.print("."));

        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        assertEquals(3, vertices.size());

        Collection<Edge> edges = g.getEdges();
        assertEquals(2, edges.size());

        Metrics.counter(t);

        printGraph(g);

        // One single counter inserted after the peek 
        vertices = g.getVertices();
        assertEquals(4, vertices.size());

        edges = g.getEdges();
        assertEquals(3, edges.size());
    }

    @Test
    public void metricsEverywhereMultiplePeek() throws Exception {

        Topology t = newTopology();
        Graph g = t.graph();

        TStream<String> s = t.strings("a", "b", "c");
        List<String> peekedValues = new ArrayList<>();
        TStream<String> speek = s.peek(tuple -> peekedValues.add(tuple + "1st"));
        TStream<String> speek2 = speek.peek(tuple -> peekedValues.add(tuple + "2nd"));
        TStream<String> speek3 = speek2.peek(tuple -> peekedValues.add(tuple + "3rd"));
        speek3.sink(tuple -> System.out.print("."));

        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        assertEquals(5, vertices.size());

        Collection<Edge> edges = g.getEdges();
        assertEquals(4, edges.size());

        Metrics.counter(t);

        printGraph(g);

        // One single counter inserted after the 3rd peek 
        vertices = g.getVertices();
        assertEquals(6, vertices.size());

        edges = g.getEdges();
        assertEquals(5, edges.size());
    }

    @Test
    public void metricsEverywherePeekThenFanout() throws Exception {
        _testFanoutWithPeek(false);
    }

    @Test
    public void metricsEverywhereFanoutThenPeek() throws Exception {
        _testFanoutWithPeek(true);
    }

    private void _testFanoutWithPeek(boolean after) throws Exception {
        Topology t = newTopology();
        Graph g = t.graph();

        /*                            -- Filter -- Sink(.)
         *                           / 
         * Source -- Peek -- FanOut ---- Modify -- Sink(@)
         * 
         */
        TStream<Integer> d = integers(t, 1, 2, 3);
        List<Integer> peekedValues = new ArrayList<>();
        
        if (!after)
            d.peek(tuple -> peekedValues.add(tuple));

        TStream<Integer> df = d.filter(tuple -> tuple.intValue() > 0);
        TStream<Integer> dm = d.modify(tuple -> new Integer(tuple.intValue() + 1));

        if (after)
            d.peek(tuple -> peekedValues.add(tuple));

        df.sink(tuple -> System.out.print("."));
        dm.sink(tuple -> System.out.print("@"));
        
        assertEquals(7, g.getVertices().size());
        assertEquals(6, g.getEdges().size());

        // Insert counter metrics into all the streams 
        Metrics.counter(t);

        printGraph(g);

        assertEquals(10, g.getVertices().size());
        assertEquals(9, g.getEdges().size());
    }

    private <T> TStream<T> integers(Topology t, @SuppressWarnings("unchecked") T... values) {
        return t.source(() -> Arrays.asList(values));
    }

    private void printGraph(Graph g) {
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String json = gson.toJson(new GraphType(g));
//        System.out.println(json);
    }

    @SuppressWarnings("serial")
    private static class TestOplet<T> extends Peek<T> {
        private Meter meter;
        private Gauge<Long> gauge;

        @Override
        public void close() throws Exception {
        }

        @Override
        protected void peek(T tuple) {
            meter.mark();
        }
        
        @Override
        public final void initialize(OpletContext<T, T> context) {
            super.initialize(context);

            this.meter = new Meter();
            this.gauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return System.currentTimeMillis();
                }
            };

            MetricRegistry registry = context.getService(MetricRegistry.class);
            if (registry != null) {
                registry.register(context.uniquify("testMeter"), meter);
                registry.register(context.uniquify("testGauge"), gauge);
            }
        }
    }
}
