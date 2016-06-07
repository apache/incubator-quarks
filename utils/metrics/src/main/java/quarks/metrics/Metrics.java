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
package quarks.metrics;

import quarks.function.Functions;
import quarks.metrics.oplets.CounterOp;
import quarks.metrics.oplets.RateMeter;
import quarks.topology.TStream;
import quarks.topology.Topology;

/**
 * This interface contains utility methods for manipulating metrics.
 */
public class Metrics {
    /**
     * Increment a counter metric when peeking at each tuple.
     * 
     * <P>
     * Same as {@code stream.peek(new CounterOp<T>())}
     * </P>
     * 
     * @param <T>
     *            TStream tuple type
     * @param stream the stream to monitor
     * @return the {@code stream} argument
     */
    public static <T> TStream<T> counter(TStream<T> stream) {
        return stream.peek(new CounterOp<T>());
    }

    /**
     * Measure current tuple throughput and calculate one-, five-, and
     * fifteen-minute exponentially-weighted moving averages.
     * 
     * <P>
     * Same as {@code stream.peek(new RateMeter<T>())}
     * </P>
     * 
     * @param <T>
     *            TStream tuple type
     * @param stream the stream to monitor
     * @return the {@code stream} argument
     */
    public static <T> TStream<T> rateMeter(TStream<T> stream) {
        return stream.peek(new RateMeter<T>());
    }

    /**
     * Add counter metrics to all the topology's streams.
     * <p>
     * {@link CounterOp} oplets are inserted between every two graph
     * vertices with the following exceptions:
     * <ul>
     * <li>Oplets are only inserted upstream from a FanOut oplet.</li>
     * <li>If a chain of Peek oplets exists between oplets A and B, a Metric 
     * oplet is inserted after the last Peek, right upstream from oplet B.</li>
     * <li>If a chain a Peek oplets is followed by a FanOut, a metric oplet is 
     * inserted between the last Peek and the FanOut oplet.</li>
     * </ul>
     * The implementation is not idempotent: Calling the method twice 
     * will insert a new set of metric oplets into the graph.
     * @param t
     *            The topology
     * @see quarks.graph.Graph#peekAll(quarks.function.Supplier, quarks.function.Predicate) Graph.peekAll()
     */
    public static void counter(Topology t) {
        // peekAll() embodies the above exclusion semantics
        t.graph().peekAll(() -> new CounterOp<>(), Functions.alwaysTrue());
    }
}
