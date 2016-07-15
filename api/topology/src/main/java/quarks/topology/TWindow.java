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
package quarks.topology;

import java.util.List;
import java.util.concurrent.TimeUnit;

import quarks.function.BiFunction;
import quarks.function.Function;
import quarks.window.Window;

/**
 * Partitioned window of tuples. Logically a window
 * represents an continuously updated ordered list of tuples according to the
 * criteria that created it. For example {@link TStream#last(int, Function) s.last(10, zero())}
 * declares a window with a single partition that at any time contains the last ten tuples seen on
 * stream {@code s}.
 * <P>
 * Windows are partitioned which means the window's configuration
 * is independently maintained for each key seen on the stream.
 * For example with a window created using {@link TStream#last(int, Function) last(3, tuple -> tuple.getId())}
 * then each key has its own window containing the last
 * three tuples with the same key obtained from the tuple's identity using {@code getId()}.
 * </P>
 * <P>
 * A variety of aggregation processing models are provided.
 * <UL>
 * <LI>Partition content change triggered aggregations or timer triggered aggregations.</LI>
 * <LI>Sliding aggregators or batch aggregators.
 *     A sliding aggregator does not affect the contents of a partition;
 *     a particular tuple may be included in multiple aggregations.
 *     Sliding aggregators are typically used to perform "signal smoothing"
 *     over the specified window.
 *     <BR>
 *     Batch aggregators process partition contents in batches, clearing
 *     the contents after a batch is processed; a particular tuple is
 *     present in at most one aggregation.
 *     Batch aggregators are often used to perform data reduction 
 *     - e.g., reducing a stream of 1KHz sampled sensor values down 
 *     to a 1Hz stream of aggregated sensor values.</LI>
 * </UL>
 * Additionally, {@link #process(Window, BiFunction)} enables use of the
 * lower level {@link Window} API to define and use additional 
 * aggregation processing models.
 *
 * @param <T> Tuple type
 * @param <K> Partition key type
 * 
 * @see TStream#last(int, Function) Count based window
 * @see TStream#last(long, java.util.concurrent.TimeUnit, Function) Time based window
 */
public interface TWindow<T, K> extends TopologyElement {
    /**
     * Declares a stream that is a continuous, sliding, 
     * content change triggered aggregation of
     * partitions in this window.
     * <P>
     * Changes in a partition's contents trigger an invocation of
     * {@code aggregator.apply(tuples, key)}, where {@code tuples} is
     * a {@code List<T>} containing all the tuples in the partition in
     * insertion order from oldest to newest.  The list is stable
     * during the aggregator invocation.
     * <UL>
     * <LI>Count-based window: the aggregator is called after each
     * tuple added to a partition.  When an addition results in a tuple
     * being evicted, the eviction occurs before the aggregator is called.</LI>
     * <LI>Time-based window: the aggregator is called after each tuple
     * added to a partition. The aggregator is also called
     * each time one or more tuples are evicted from a partition 
     * (multiple tuples may be evicted at once).  The list will be
     * empty if the eviction results in an empty partition.</LI>
     * </UL>
     * A non-null {@code aggregator} result is added to the returned stream.
     * </P>
     * <P>
     * Thus the returned stream will contain a sequence of tuples where the
     * most recent tuple represents the most up to date aggregation of a
     * partition.
     *
     * @param <U> Tuple type
     * @param aggregator
     *            Logic to aggregation a partition.
     * @return A stream that contains the latest aggregations of partitions in this window.
     */
    <U> TStream<U> aggregate(BiFunction<List<T>, K, U> aggregator);
    
    /**
     * Declares a stream that represents a 
     * content change triggered batched aggregation of
     * partitions in this window.
     * <P>
     * Each partition "batch" triggers an invocation of
     * {@code batcher.apply(tuples, key)}, where {@code tuples} is
     * a {@code List<T>} containing all the tuples in the partition in
     * insertion order from oldest to newest  The list is stable
     * during the batcher invocation.
     * <UL>
     * <LI>Count-based window: a batch occurs when the partition is full.</LI>
     * <LI>Time-based window: a batch occurs every "time" period units.  The
     * list will be empty if no tuples have been received during the period.</LI>
     * </UL>
     * A non-null {@code batcher} result is added to the returned stream.
     * The partition's contents are cleared after a batch is processed.
     * </P>
     * <P>
     * Thus the returned stream will contain a sequence of tuples where the
     * most recent tuple represents the most up to date aggregation of a
     * partition.
     * 
     * @param <U> Tuple type
     * @param batcher
     *            Logic to aggregation a partition.
     * @return A stream that contains the latest aggregations of partitions in this window.
     */
    <U> TStream<U> batch(BiFunction<List<T>, K, U> batcher);

    /**
     * Declares a stream that is a continuous, sliding, 
     * timer triggered aggregation of
     * partitions in this window.
     * <P>
     * Periodically trigger an invocation of
     * {@code aggregator.apply(tuples, key)}, where {@code tuples} is
     * a {@code List<T>} containing all the tuples in the partition in
     * insertion order from oldest to newest.  The list is stable
     * during the aggregator invocation.  
     * The list will be empty if the partition is empty.
     * </P>
     * <P> 
     * A non-null {@code aggregator} result is added to the returned stream.
     * </P>
     * <P>
     * Thus the returned stream will contain a sequence of tuples where the
     * most recent tuple represents the most up to date aggregation of a
     * partition.
     *
     * @param <U> Tuple type
     * @param period how often to invoke the aggregator
     * @param unit TimeUnit for {@code period}
     * @param aggregator
     *            Logic to aggregation a partition.
     * @return A stream that contains the latest aggregations of partitions in this window.
     * 
     * @see #aggregate(BiFunction)
     */
    <U> TStream<U> timedAggregate(long period, TimeUnit unit, BiFunction<List<T>, K, U> aggregator);
    
    /**
     * Declares a stream that represents a 
     * timer triggered batched aggregation of
     * partitions in this window. 
     * <P>
     * Periodically trigger an invocation of
     * {@code batcher.apply(tuples, key)}, where {@code tuples} is
     * a {@code List<T>} containing all the tuples in the partition in
     * insertion order from oldest to newest  The list is stable
     * during the batcher invocation.
     * The list will be empty if the partition is empty.
     * <P>
     * A non-null {@code batcher} result is added to the returned stream.
     * The partition's contents are cleared after a batch is processed.
     * </P>
     * <P>
     * Thus the returned stream will contain a sequence of tuples where the
     * most recent tuple represents the most up to date aggregation of a
     * partition.
     * 
     * @param <U> Tuple type
     * @param period how often to invoke the batcher
     * @param unit TimeUnit for {@code period}
     * @param batcher
     *            Logic to aggregation a partition.
     * @return A stream that contains the latest aggregations of partitions in this window.
     * 
     * @see #batch(BiFunction)
     */
    <U> TStream<U> timedBatch(long period, TimeUnit unit, BiFunction<List<T>, K, U> batcher);
    
    /**
     * Declares a stream that represents an aggregation of
     * partitions in this window using the specified {@link Window}.
     * <P>
     * This method makes it easier to create aggregation streams
     * using the lower level {@code Window} API to construct and
     * supply windows with configurations not directly exposed by {@code TWindow}.
     * 
     * @param window the window implementation
     * @param aggregator the aggregation function
     *  
     * @return A stream that contains the latest aggregations of partitions in this window.
     */
    <U, L extends List<T>> TStream<U> process(Window<T,K,L> window, BiFunction<List<T>, K, U> aggregator);

    /**
     * Returns the key function used to map tuples to partitions.
     * @return Key function used to map tuples to partitions.
     */
    Function<T, K> getKeyFunction();
    
    /**
     * Get the stream that feeds this window.
     * @return stream that feeds this window.
     */
    TStream<T> feeder();
}
