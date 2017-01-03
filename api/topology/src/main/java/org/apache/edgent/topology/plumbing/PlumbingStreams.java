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
package org.apache.edgent.topology.plumbing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.oplet.plumbing.Barrier;
import org.apache.edgent.oplet.plumbing.Isolate;
import org.apache.edgent.oplet.plumbing.PressureReliever;
import org.apache.edgent.oplet.plumbing.UnorderedIsolate;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TopologyProvider;

/**
 * Plumbing utilities for {@link TStream}.
 * Methods that manipulate the flow of tuples in a streaming topology,
 * but are not part of the logic of the application.
 */
public class PlumbingStreams {
  
    /**
     * Insert a blocking delay between tuples.
     * Returned stream is the input stream delayed by {@code delay}.
     * <p>
     * Delays less than 1msec are translated to a 0 delay.
     * <p>
     * This function always adds the {@code delay} amount after receiving
     * a tuple before forwarding it.  
     * <p>
     * Downstream tuple processing delays will affect
     * the overall delay of a subsequent tuple.
     * <p>
     * e.g., the input stream contains two tuples t1 and t2 and
     * the delay is 100ms.  The forwarding of t1 is delayed by 100ms.
     * Then if a downstream processing delay of 80ms occurs, this function
     * receives t2 80ms after it forwarded t1 and it will delay another
     * 100ms before forwarding t2.  Hence the overall delay between forwarding
     * t1 and t2 is 180ms.
     * See {@link #blockingThrottle(long, TimeUnit) blockingThrottle}.
     *
     * @param <T> Tuple type
     * @param stream Stream t
     * @param delay Amount of time to delay a tuple.
     * @param unit Time unit for {@code delay}.
     * 
     * @return Stream that will be delayed.
     */
    public static <T> TStream<T> blockingDelay(TStream<T> stream, long delay, TimeUnit unit) {
        return stream.map(t -> {try {
            Thread.sleep(unit.toMillis(delay));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } return t;}) ;
    }
    
    /**
     * Maintain a constant blocking delay between tuples.
     * The returned stream is the input stream throttled by {@code delay}.
     * <p>
     * Delays less than 1msec are translated to a 0 delay.
     * <p>
     * Sample use:
     * <pre>{@code
     * TStream<String> stream = topology.strings("a", "b, "c");
     * // Create a stream with tuples throttled to 1 second intervals.
     * TStream<String> throttledStream = blockingThrottle(stream, 1, TimeUnit.SECOND);
     * // print out the throttled tuples as they arrive
     * throttledStream.peek(t -> System.out.println(new Date() + " - " + t));
     * }</pre>
     * <p>
     * The function adjusts for downstream processing delays.
     * The first tuple is not delayed.  If {@code delay} has already
     * elapsed since the prior tuple was forwarded, the tuple 
     * is forwarded immediately.
     * Otherwise, forwarding the tuple is delayed to achieve
     * a {@code delay} amount since forwarding the prior tuple.
     * <p>
     * e.g., the input stream contains two tuples t1 and t2 and
     * the delay is 100ms.  The forwarding of t1 is delayed by 100ms.
     * Then if a downstream processing delay of 80ms occurs, this function
     * receives t2 80ms after it forwarded t1 and it will only delay another
     * 20ms (100ms - 80ms) before forwarding t2.  
     * Hence the overall delay between forwarding t1 and t2 remains 100ms.
     * 
     * @param <T> tuple type
     * @param stream the stream to throttle
     * @param delay Amount of time to delay a tuple.
     * @param unit Time unit for {@code delay}.
     * @return the throttled stream
     */
    public static <T> TStream<T> blockingThrottle(TStream<T> stream, long delay, TimeUnit unit) {
        return stream.map( blockingThrottle(delay, unit) );
    }

    private static <T> Function<T,T> blockingThrottle(long delay, TimeUnit unit) {
        long[] nextTupleTime = { 0 };
        return t -> {
            long now = System.currentTimeMillis();
            if (nextTupleTime[0] != 0) {
                if (now < nextTupleTime[0]) {
                    try {
                        Thread.sleep(nextTupleTime[0] - now);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    now = System.currentTimeMillis();
                }
            }
            nextTupleTime[0] = now + unit.toMillis(delay);
            return t;
        };
    }
    
    /**
     * Insert a blocking delay before forwarding the first tuple and
     * no delay for subsequent tuples.
     * <p>
     * Delays less than 1msec are translated to a 0 delay.
     * <p>
     * Sample use:
     * <pre>{@code
     * TStream<String> stream = topology.strings("a", "b, "c");
     * // create a stream where the first tuple is delayed by 5 seconds. 
     * TStream<String> oneShotDelayedStream =
     *      stream.map( blockingOneShotDelay(5, TimeUnit.SECONDS) );
     * }</pre>
     * 
     * @param <T> tuple type
     * @param stream input stream
     * @param delay Amount of time to delay a tuple.
     * @param unit Time unit for {@code delay}.
     * @return the delayed stream
     */
    public static <T> TStream<T> blockingOneShotDelay(TStream<T> stream, long delay, TimeUnit unit) {
        return stream.map( blockingOneShotDelay(delay, unit) );
    }

    private static <T> Function<T,T> blockingOneShotDelay(long delay, TimeUnit unit) {
        long[] initialDelay = { unit.toMillis(delay) };
        return t -> {
            if (initialDelay[0] != -1) {
                try {
                    Thread.sleep(initialDelay[0]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                initialDelay[0] = -1;
            }
            return t;
            };
    }
    
    /**
     * Relieve pressure on upstream processing by discarding tuples.
     * This method ensures that upstream processing is not
     * constrained by any delay in downstream processing,
     * for example by a connector not being able to connect
     * to its external system.
     * <P>
     * Any downstream processing of the returned stream is isolated
     * from {@code stream} so that any slow down does not affect {@code stream}.
     * When the downstream processing cannot keep up with rate of
     * {@code stream} tuples will be dropped from returned stream.
     * <BR>
     * Up to {@code count} of the most recent tuples per key from {@code stream}
     * are maintained when downstream processing is slow, any older tuples
     * that have not been submitted to the returned stream will be discarded.
     * <BR>
     * Tuple order is maintained within a partition but is not guaranteed to
     * be maintained across partitions.
     * </P>
     * 
     * @param stream Stream to be isolated from downstream processing.
     * @param keyFunction Function defining the key of each tuple.
     * @param count Maximum number of tuples to maintain when downstream processing is backing up.
     * @return Stream that is isolated from and thus relieves pressure on {@code stream}.
     * 
     * @param <T> Tuple type.
     * @param <K> Key type.
     * @see #isolate(TStream, int) isolate
     */
    public static <T,K> TStream<T> pressureReliever(TStream<T> stream, Function<T,K> keyFunction, int count) {
        return stream.pipe(new PressureReliever<>(count, keyFunction));
    }
    
    /**
     * Isolate upstream processing from downstream processing.
     * <BR>
     * Implementations may throw {@code OutOfMemoryExceptions} 
     * if the processing against returned stream cannot keep up
     * with the arrival rate of tuples on {@code stream}.
     *
     * @param <T> Tuple type
     * @param stream Stream to be isolated from downstream processing.
     * @param ordered {@code true} to maintain arrival order on the returned stream,
     * {@code false} to not guaranteed arrival order.
     * @return Stream that is isolated from {@code stream}.
     */
    public static <T> TStream<T> isolate(TStream<T> stream, boolean ordered) {
        return stream.pipe(
                ordered ? new Isolate<T>() : new UnorderedIsolate<T>());
    }
    
    /**
     * Isolate upstream processing from downstream processing.
     * <P>
     * If the processing against the returned stream cannot keep up
     * with the arrival rate of tuples on {@code stream}, upstream
     * processing will block until there is space in the queue between
     * the streams.
     * </P><P>
     * Processing of tuples occurs in the order they were received.
     * </P>
     * 
     * @param <T> Tuple type
     * @param stream Stream to be isolated from downstream processing.
     * @param queueCapacity size of the queue between {@code stream} and
     *        the returned stream.
     * @return Stream that is isolated from {@code stream}.
     * @see #pressureReliever(TStream, Function, int) pressureReliever
     */
    public static <T> TStream<T> isolate(TStream<T> stream, int queueCapacity) {
      return stream.pipe(new Isolate<T>(queueCapacity));
    }
    
    /**
     * Perform analytics concurrently.
     * <P>
     * This is a convenience function that calls
     * {@link #concurrent(TStream, List, Function)} after
     * creating {@code pipeline} and {@code combiner} functions
     * from the supplied {@code mappers} and {@code combiner} arguments.
     * </P><P>
     * That is, it is logically, if not exactly, the same as:
     * </P>
     * <pre>{@code
     * List<Function<TStream<T>,TStream<U>>> pipelines = new ArrayList<>();
     * for (Function<T,U> mapper : mappers)
     *   pipelines.add(s -> s.map(mapper));
     * concurrent(stream, pipelines, combiner);
     * }</pre>
     * 
     * @param <T> Tuple type on input stream.
     * @param <U> Tuple type generated by mappers.
     * @param <R> Tuple type of the result.
     * 
     * @param stream input stream
     * @param mappers functions to be run concurrently.  Each mapper MUST
     *                 return a non-null result.
     *                 A runtime error will be generated if a null result
     *                 is returned.
     * @param combiner function to create a result tuple from the list of
     *                 results from {@code mappers}.
     *                 The input list order is 1:1 with the {@code mappers} list.
     *                 I.e., list entry [0] is the result from mappers[0],
     *                 list entry [1] is the result from mappers[1], etc.
     * @return result stream
     */
    public static <T,U,R> TStream<R> concurrentMap(TStream<T> stream, List<Function<T,U>> mappers, Function<List<U>,R> combiner) {
      Objects.requireNonNull(stream, "stream");
      Objects.requireNonNull(mappers, "mappers");
      Objects.requireNonNull(combiner, "combiner");
      
      List<Function<TStream<T>,TStream<U>>> pipelines = new ArrayList<>();
      for (Function<T,U> mapper : mappers) {
        pipelines.add(s -> s.map(mapper));
      }
      
      return concurrent(stream, pipelines, combiner);
    }

    /**
     * Perform analytics concurrently.
     * <P>
     * Process input tuples one at at time, invoking the specified
     * analytics ({@code pipelines}) concurrently, combine the results,
     * and then process the next input tuple in the same manner.
     * </P><P>
     * Logically, instead of doing this:
     * </P>
     * <pre>{@code
     * sensorReadings<T> -> A1 -> A2 -> A3 -> results<R>
     * }</pre>
     * create a graph that's logically like this:
     * <pre>{@code
     * - 
     *                      |-> A1 ->|
     * sensorReadings<T> -> |-> A2 ->| -> results<R>
     *                      |-> A3 ->|
     * 
     * }</pre>
     * more specifically a graph like this:
     * <pre>{@code
     * -
     *           |-> isolate(1) -> pipeline1 -> |
     * stream -> |-> isolate(1) -> pipeline2 -> |-> barrier(10) -> combiner 
     *           |-> isolate(1) -> pipeline3 -> |
     *                . . .
     * }</pre>
     * <P>
     * The typical use case for this is when an application has a collection
     * of independent analytics to perform on each tuple and the analytics
     * are sufficiently long running such that performing them concurrently
     * is desired.
     * </P><P>
     * Note, this is in contrast to "parallel" stream processing,
     * which in Java8 Streams and other contexts means processing multiple
     * tuples in parallel, each on a replicated processing pipeline.
     * </P><P>
     * Threadsafety - one of the following must be true:
     * </P>
     * <ul>
     * <li>the tuples from {@code stream} are threadsafe</li>
     * <li>the {@code pipelines} do not modify the input tuples</li>
     * <li>the {@code pipelines} provide their own synchronization controls
     *     to protect concurrent modifications of the input tuples</li>
     * </ul>
     * <P>
     * Logically, a thread is allocated for each of the {@code pipelines}.
     * The actual degree of concurrency may be {@link TopologyProvider} dependent.
     * </P>
     * 
     * @param <T> Tuple type on input stream.
     * @param <U> Tuple type generated by pipelines.
     * @param <R> Tuple type of the result.
     * 
     * @param stream input stream
     * @param pipelines a list of functions to add a pipeline to the topology.
     *                 Each {@code pipeline.apply()} is called with {@code stream}
     *                 as the input, yielding the pipeline's result stream.
     *                 For each input tuple, a pipeline MUST create exactly one output tuple.
     *                 Tuple flow into the pipelines will cease if that requirement
     *                 is not met.
     * @param combiner function to create a result tuple from the list of
     *                 results from {@code pipelines}.
     *                 The input tuple list's order is 1:1 with the {@code pipelines} list.
     *                 I.e., list entry [0] is the result from pipelines[0],
     *                 list entry [1] is the result from pipelines[1], etc.
     * @return result stream
     * @see #barrier(List, int) barrier
     */
    public static <T,U,R> TStream<R> concurrent(TStream<T> stream, List<Function<TStream<T>,TStream<U>>> pipelines, Function<List<U>,R> combiner) {
      Objects.requireNonNull(stream, "stream");
      Objects.requireNonNull(pipelines, "pipelines");
      Objects.requireNonNull(combiner, "combiner");
      
      int barrierQueueCapacity = 10;  // don't preclude pipelines from getting ahead some.
      
      // Add concurrent (isolated) fanouts
      List<TStream<T>> fanouts = new ArrayList<>(pipelines.size());
      for (int i = 0; i < pipelines.size(); i++)
        fanouts.add(isolate(stream, 1).tag("concurrent.isolated-ch"+i));
      
      // Add pipelines
      List<TStream<U>> results = new ArrayList<>(pipelines.size());
      int ch = 0;
      for (Function<TStream<T>,TStream<U>> pipeline : pipelines) {
        results.add(pipeline.apply(fanouts.get(ch)).tag("concurrent-ch"+ch));
        ch++;
      }
      
      // Add the barrier
      TStream<List<U>> barrier = barrier(results, barrierQueueCapacity).tag("concurrent.barrier");
      
      // Add the combiner
      return barrier.map(combiner);
    }

    /**
     * A tuple synchronization barrier.
     * <P>
     * Same as {@code barrier(others, 1)}
     * </P>
     * @param <T> Tuple type
     * @param streams input streams
     * @return the output stream
     * @see #barrier(List, int)
     */
    public static <T> TStream<List<T>> barrier(List<TStream<T>> streams) {
      return barrier(streams, 1);
    }

    /**
     * A tuple synchronization barrier.
     * <P>
     * A barrier has n input streams with tuple type {@code T}
     * and one output stream with tuple type {@code List<T>}.
     * Once the barrier receives one tuple on each of its input streams,
     * it generates an output tuple containing one tuple from each input stream.
     * It then waits until it has received another tuple from each input stream.
     * </P><P>
     * Input stream 0's tuple is in the output tuple's list[0],
     * stream 1's tuple in list[1], and so on.
     * </P><P>
     * The barrier's output stream is isolated from the input streams.
     * </P><P>
     * The barrier has a queue of size {@code queueCapacity} for each
     * input stream.  When a tuple for an input stream is received it is
     * added to its queue.  The stream will block if the queue is full.
     * </P>
     *
     * @param <T> Type of the tuple.
     * 
     * @param streams the list of input streams
     * @param queueCapacity the size of each input stream's queue
     * @return the output stream
     * @see Barrier
     */
    public static <T> TStream<List<T>> barrier(List<TStream<T>> streams, int queueCapacity) {
      List<TStream<T>> others = new ArrayList<>(streams);
      TStream<T> s1 = others.remove(0);
      return s1.fanin(new Barrier<T>(queueCapacity), others);
    }

    /**
     * Perform an analytic function on tuples in parallel.
     * <P>
     * Same as {@code parallel(stream, width, splitter, (s,ch) -> s.map(t -> mapper.apply(t, ch))}
     * </P>
     * @param <T> Input stream tuple type
     * @param <U> Result stream tuple type
     * @param stream input stream
     * @param splitter the tuple channel allocation function
     * @param mapper analytic function
     * @param width number of channels
     * @return the unordered result stream
     * @see #roundRobinSplitter(int) roundRobinSplitter
     * @see #concurrentMap(TStream, List, Function) concurrentMap
     */
    public static <T,U> TStream<U> parallelMap(TStream<T> stream, int width, ToIntFunction<T> splitter, BiFunction<T,Integer,U> mapper) {
      BiFunction<TStream<T>,Integer,TStream<U>> pipeline = (s,ch) -> s.map(t -> mapper.apply(t, ch));
      return parallel(stream, width, splitter, pipeline);
    }
    
    /**
     * Perform an analytic pipeline on tuples in parallel.
     * <P>
     * Splits {@code stream} into {@code width} parallel processing channels,
     * partitioning tuples among the channels using {@code splitter}.
     * Each channel runs a copy of {@code pipeline}.
     * The resulting stream is isolated from the upstream parallel channels.
     * </P><P>
     * The ordering of tuples in {@code stream} is not maintained in the
     * results from {@code parallel}.
     * </P><P>
     * {@code pipeline} is not required to yield a result for each input
     * tuple.
     * </P><P>
     * A common splitter function is a {@link #roundRobinSplitter(int) roundRobinSplitter}.
     * </P><P>
     * The generated graph looks like this:
     * </P>
     * <pre>{@code
     * -
     *                                    |-> isolate(10) -> pipeline-ch1 -> |
     * stream -> split(width,splitter) -> |-> isolate(10) -> pipeline-ch2 -> |-> union -> isolate(width)
     *                                    |-> isolate(10) -> pipeline-ch3 -> |
     *                                                . . .
     * }</pre>
     * 
     * @param <T> Input stream tuple type
     * @param <R> Result stream tuple type
     * 
     * @param stream the input stream
     * @param width number of parallel processing channels
     * @param splitter the tuple channel allocation function
     * @param pipeline the pipeline for each channel.  
     *        {@code pipeline.apply(inputStream,channel)}
     *        is called to generate the pipeline for each channel.
     * @return the isolated unordered result from each parallel channel
     * @see #roundRobinSplitter(int) roundRobinSplitter
     * @see #concurrent(TStream, List, Function) concurrent
     */
    public static <T,R> TStream<R> parallel(TStream<T> stream, int width, ToIntFunction<T> splitter, BiFunction<TStream<T>,Integer,TStream<R>> pipeline) {
      Objects.requireNonNull(stream, "stream");
      if (width < 1)
        throw new IllegalArgumentException("width");
      Objects.requireNonNull(splitter, "splitter");
      Objects.requireNonNull(pipeline, "pipeline");
      
      // Add the splitter
      List<TStream<T>> channels = stream.split(width, splitter);
      for (int ch = 0; ch < width; ch++)
        channels.set(ch, channels.get(ch).tag("parallel.split-ch"+ch));
      
      // Add concurrency (isolation) to the channels
      int chBufferSize = 10; // don't immediately block stream if channel is busy
      for (int ch = 0; ch < width; ch++)
        channels.set(ch, isolate(channels.get(ch), chBufferSize).tag("parallel.isolated-ch"+ch));
      
      // Add pipelines
      List<TStream<R>> results = new ArrayList<>(width);
      for (int ch = 0; ch < width; ch++) {
        results.add(pipeline.apply(channels.get(ch), ch).tag("parallel-ch"+ch));
      }
      
      // Add the Union
      TStream<R> result =  results.get(0).union(new HashSet<>(results)).tag("parallel.union");
      
      // Add the isolate - keep channel threads to just their pipeline processing
      return isolate(result, width);
    }

    /**
     * Perform an analytic pipeline on tuples in parallel.
     * <P>
     * Splits {@code stream} into {@code width} parallel processing channels,
     * partitioning tuples among the channels in a load balanced fashion.
     * Each channel runs a copy of {@code pipeline}.
     * The resulting stream is isolated from the upstream parallel channels.
     * </P><P>
     * The ordering of tuples in {@code stream} is not maintained in the
     * results from {@code parallel}.
     * </P><P>
     * A {@code pipeline} <b>MUST</b> yield a result for each input
     * tuple.  Failure to do so will result in the channel remaining
     * in a busy state and no longer available to process additional tuples.
     * </P><P>
     * A {@link LoadBalancedSplitter} is used to distribute tuples.
     * </P><P>
     * The generated graph looks like this:
     * </P>
     * <pre>{@code
     * -
     *                                    |-> isolate(1) -> pipeline-ch1 -> peek(splitter.channelDone()) -> |
     * stream -> split(width,splitter) -> |-> isolate(1) -> pipeline-ch2 -> peek(splitter.channelDone()) -> |-> union -> isolate(width)
     *                                    |-> isolate(1) -> pipeline-ch3 -> peek(splitter.channelDone()) -> |
     *                                                . . .
     * }</pre>
     * <P>
     * Note, this implementation requires that the splitter is used from
     * only a single JVM.  The {@link org.apache.edgent.providers.direct.DirectProvider DirectProvider}
     * provider meets this requirement.
     * </P>
     * 
     * @param <T> Input stream tuple type
     * @param <R> Result stream tuple type
     * 
     * @param stream the input stream
     * @param width number of parallel processing channels
     * @param pipeline the pipeline for each channel.  
     *        {@code pipeline.apply(inputStream,channel)}
     *        is called to generate the pipeline for each channel.
     * @return the isolated unordered result from each parallel channel
     * @see #parallel(TStream, int, ToIntFunction, BiFunction)
     * @see LoadBalancedSplitter
     */
    public static <T,R> TStream<R> parallelBalanced(TStream<T> stream, int width, BiFunction<TStream<T>,Integer,TStream<R>> pipeline) {
      Objects.requireNonNull(stream, "stream");
      if (width < 1)
        throw new IllegalArgumentException("width");
      Objects.requireNonNull(pipeline, "pipeline");
      
      LoadBalancedSplitter<T> splitter = new LoadBalancedSplitter<>(width);
      
      // Add the splitter
      List<TStream<T>> channels = stream.split(width, splitter);
      for (int ch = 0; ch < width; ch++)
        channels.set(ch, channels.get(ch).tag("parallel.split-ch"+ch));
      
      // Add concurrency (isolation) to the channels
      int chBufferSize = 1; // 1 is enough with load balanced impl
      for (int ch = 0; ch < width; ch++)
        channels.set(ch, isolate(channels.get(ch), chBufferSize).tag("parallel.isolated-ch"+ch));
      
      // Add pipelines
      List<TStream<R>> results = new ArrayList<>(width);
      for (int ch = 0; ch < width; ch++) {
        final int finalCh = ch;
        results.add(pipeline.apply(channels.get(ch), ch)
            .tag("parallel-ch"+ch)
            .peek(tuple -> splitter.channelDone(finalCh)));
      }
      
      // Add the Union
      TStream<R> result =  results.get(0).union(new HashSet<>(results)).tag("parallel.union");
      
      // Add the isolate - keep channel threads to just their pipeline processing
      return isolate(result, width);
    }
    
    /**
     * A round-robin splitter ToIntFunction
     * <P>
     * The splitter function cycles among the {@code width} channels
     * on successive calls to {@code roundRobinSplitter.applyAsInt()},
     * returning {@code 0, 1, ..., width-1, 0, 1, ..., width-1}.
     * </P>
     * @param <T> Tuple type
     * @param width number of splitter channels
     * @return the splitter
     * @see TStream#split(int, ToIntFunction) TStream.split
     * @see PlumbingStreams#parallel(TStream, int, ToIntFunction, BiFunction) parallel
     */
    public static <T> ToIntFunction<T> roundRobinSplitter(int width) {
      AtomicInteger cnt = new AtomicInteger();
      return tuple -> cnt.getAndIncrement() % width;
    }
    /**
     * Control the flow of tuples to an output stream.
     * <P>
     * A {@link Semaphore} is used to control the flow of tuples
     * through the {@code gate}.
     * The gate acquires a permit from the
     * semaphore to pass the tuple through, blocking until a permit is
     * acquired (and applying backpressure upstream while blocked).
     * Elsewhere, some code calls {@link Semaphore#release(int)}
     * to make permits available.
     * </P><P>
     * If a TopologyProvider is used that can distribute a topology's
     * streams to different JVM's the gate and the code releasing the
     * permits must be in the same JVM.
     * </P><P>
     * Sample use:
     * <BR>
     * Suppose you wanted to control processing such that concurrent
     * pipelines processed each tuple in lock-step.
     * I.e., You want all of the pipelines to start processing a tuple
     * at the same time and not start a new tuple until the current
     * tuple had been fully processed by each of them:
     * </P>
     * <pre>{@code
     * TStream<Integer> readings = ...;
     * Semaphore gateControl = new Semaphore(1); // allow the first to pass through
     * TStream<Integer> gated = PlumbingStreams.gate(readings, gateControl);
     * 
     * // Create the concurrent pipeline combiner and have it
     * // signal that concurrent processing of the tuple has completed.
     * // In this sample the combiner just returns the received list of
     * // each pipeline result.
     * 
     * Function<TStream<List<Integer>>,TStream<List<Integer>>> combiner =
     *   stream -> stream.map(list -> {
     *       gateControl.release();
     *       return list;
     *     });
     *   
     * TStream<List<Integer>> results = PlumbingStreams.concurrent(gated, pipelines, combiner);
     * }</pre>
     * @param <T> Tuple type
     * @param stream the input stream
     * @param semaphore gate control
     * @return gated stream
     */
    public static <T> TStream<T> gate(TStream<T> stream, Semaphore semaphore) {
        return stream.map(tuple -> {
            try {
                semaphore.acquire();
                return tuple;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", e);
            }
        });
    }
}