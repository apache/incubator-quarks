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
package org.apache.edgent.metrics;

import org.apache.edgent.metrics.oplets.CounterOp;
import org.apache.edgent.metrics.oplets.RateMeter;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * This interface contains utility methods for manipulating metrics.
 */
public class Metrics {
    /**
     * Increment a counter metric when peeking at each tuple.
     * 
     * @param <T>
     *            TStream tuple type
     * @param stream to stream to instrument
     * @return a {@link TStream} containing the input tuples
     */
    public static <T> TStream<T> counter(TStream<T> stream) {
        return stream.pipe(new CounterOp<T>());
    }

    /**
     * Measure current tuple throughput and calculate one-, five-, and
     * fifteen-minute exponentially-weighted moving averages.
     * 
     * @param <T>
     *            TStream tuple type
     * @param stream to stream to instrument
     * @return a {@link TStream} containing the input tuples
     */
    public static <T> TStream<T> rateMeter(TStream<T> stream) {
        return stream.pipe(new RateMeter<T>());
    }

    /**
     * Add counter metrics to all the org.apache.edgent.org.apache.edgent.topology's streams.
     * <p>
     * {@link CounterOp} oplets are inserted between every two org.apache.edgent.graph
     * vertices with the following exceptions:
     * <ul>
     * <li>Oplets are only inserted upstream from a FanOut oplet.</li>
     * <li>If a chain of Peek oplets exists between oplets A and B, a Metric 
     * oplet is inserted after the last Peek, right upstream from oplet B.</li>
     * <li>If a chain a Peek oplets is followed by a FanOut, a metric oplet is 
     * inserted between the last Peek and the FanOut oplet.</li>
     * <li>Oplets are not inserted immediately downstream from another 
     * {@code CounterOp} oplet (but they are inserted upstream from one.)</li>
     * </ul>
     * The implementation is not idempotent: Calling the method twice 
     * will insert a new set of metric oplets into the org.apache.edgent.graph.
     * @param t
     *            The org.apache.edgent.org.apache.edgent.topology
     * @see org.apache.edgent.graph.Graph#peekAll(org.apache.edgent.function.Supplier, org.apache.edgent.function.Predicate) Graph.peekAll()
     */
    public static void counter(Topology t) {
        // peekAll() embodies the above exclusion semantics
        t.graph().peekAll(
                () -> new CounterOp<>(), 
                v -> !(v.getInstance() instanceof CounterOp)
            );
    }
}
