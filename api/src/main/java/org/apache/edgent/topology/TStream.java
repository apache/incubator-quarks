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
package org.apache.edgent.topology;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Predicate;
import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.oplet.core.FanIn;
import org.apache.edgent.oplet.core.Pipe;
import org.apache.edgent.oplet.core.Sink;

/**
 * A {@code TStream} is a declaration of a continuous sequence of tuples. A
 * connected org.apache.edgent.org.apache.edgent.topology of streams and functional transformations is built using
 * {@link Topology}. <BR>
 * Generic methods on this interface provide the ability to
 * {@link #filter(Predicate) filter}, {@link #map(Function)
 * map (or transform)} or {@link #sink(Consumer) sink} this declared stream using a
 * function.
 * <P>
 * {@code TStream} is not a runtime representation of a stream,
 * it is a declaration used in building a org.apache.edgent.org.apache.edgent.topology.
 * The actual runtime stream is created once the org.apache.edgent.org.apache.edgent.topology
 * is {@link org.apache.edgent.execution.Submitter#submit(Object) submitted}
 * to a runtime.
 * 
 * </P>
 * @param <T>
 *            Tuple type.
 */
public interface TStream<T> extends TopologyElement {
  
    /**
     * TYPE is used to identify {@link ControlService} mbeans registered for
     * for a TStream.
     * The value is {@value} 
     */
    public static final String TYPE = "stream";
    // N.B. to avoid build problems due to org.apache.edgent.org.apache.edgent.topology <=> oplet,
    // other code contain a copy of this value (ugh) as TSTREAM_TYPE

    /**
     * Declare a new stream that filters tuples from this stream. Each tuple
     * {@code t} on this stream will appear in the returned stream if
     * {@link Predicate#test(Object) filter.test(t)} returns {@code true}. If
     * {@code filter.test(t)} returns {@code false} then then {@code t} will not
     * appear in the returned stream.
     * <P>
     * Examples of filtering out all empty strings from stream {@code s} of type
     * {@code String}
     * </P>
     * 
     * <pre>
     * <code>
     * TStream&lt;String&gt; s = ...
     * TStream&lt;String&gt; filtered = s.filter(t -&gt; !t.isEmpty());
     *             
     * </code>
     * </pre>
     * 
     * @param predicate
     *            Filtering logic to be executed against each tuple.
     * @return Filtered stream
     */
    TStream<T> filter(Predicate<T> predicate);

    /**
     * Declare a new stream that maps (or transforms) each tuple from this stream into one
     * (or zero) tuple of a different type {@code U}. For each tuple {@code t}
     * on this stream, the returned stream will contain a tuple that is the
     * result of {@code mapper.apply(t)} when the return is not {@code null}.
     * If {@code mapper.apply(t)} returns {@code null} then no tuple
     * is submitted to the returned stream for {@code t}.
     * 
     * <P>
     * Examples of transforming a stream containing numeric values as
     * {@code String} objects into a stream of {@code Double} values.
     * </P>
     * 
     * <pre>
     * <code>
     * // Using lambda expression
     * TStream&lt;String&gt; strings = ...
     * TStream&lt;Double&gt; doubles = strings.map(v -&gt; Double.valueOf(v));
     * 
     * // Using method reference
     * TStream&lt;String&gt; strings = ...
     * TStream&lt;Double&gt; doubles = strings.map(Double::valueOf);
     * 
     * </code>
     * </pre>
     * 
     * @param <U> Tuple type of output stream
     * @param mapper
     *            Mapping logic to be executed against each tuple.
     * @return Stream that will contain tuples of type {@code U} mapped from this
     *         stream's tuples.
     */
    <U> TStream<U> map(Function<T, U> mapper);
    
    /**
     * Declare a new stream that maps tuples from this stream into one or
     * more (or zero) tuples of a different type {@code U}. For each tuple
     * {@code t} on this stream, the returned stream will contain all non-null tuples in
     * the {@code Iterator<U>} that is the result of {@code mapper.apply(t)}.
     * Tuples will be added to the returned stream in the order the iterator
     * returns them.
     * 
     * <BR>
     * If the return is null or an empty iterator then no tuples are added to
     * the returned stream for input tuple {@code t}.
     * <P>
     * Examples of mapping a stream containing lines of text into a stream
     * of words split out from each line. The order of the words in the stream
     * will match the order of the words in the lines.
     * </P>
     * 
     * <pre>
     * <code>
     * TStream&lt;String&gt; lines = ...
     * TStream&lt;String&gt; words = lines.flatMap(
     *                     line -&gt; Arrays.asList(line.split(" ")));
     *             
     * </code>
     * </pre>
     * 
     * @param <U> Type of mapped input tuples.
     * @param mapper
     *            Mapper logic to be executed against each tuple.     
     * @return Stream that will contain tuples of type {@code U} mapped and flattened from this
     *         stream's tuples.
     */
    <U> TStream<U> flatMap(Function<T, Iterable<U>> mapper);

    /**
     * Split a stream's tuples among {@code n} streams as specified by
     * {@code splitter}.
     * 
     * <P>
     * For each tuple on the stream, {@code splitter.applyAsInt(tuple)} is
     * called. The return value {@code r} determines the destination stream:
     * </P>
     * 
     * <pre>
     * if r &lt; 0 the tuple is discarded
     * else it is sent to the stream at position (r % n) in the returned array.
     * </pre>
     *
     * <P>
     * Each split {@code TStream} is exposed by the API. The user has full
     * control over the each stream's processing pipeline. Each stream's
     * pipeline must be declared explicitly. Each stream can have different
     * processing pipelines.
     * </P>
     * <P>
     * An N-way {@code split()} is logically equivalent to a collection of N
     * {@code filter()} invocations, each with a predicate to select the tuples
     * for its stream. {@code split()} is more efficient. Each tuple is analyzed
     * only once by a single {@code splitter} instance to identify the
     * destination stream. For example, these are logically equivalent:
     * </P>
     * <pre>
     * List&lt;TStream&lt;String&gt;&gt; streams = stream.split(2, tuple -&gt; tuple.length());
     * 
     * TStream&lt;String&gt; stream0 = stream.filter(tuple -&gt; (tuple.length() % 2) == 0);
     * TStream&lt;String&gt; stream1 = stream.filter(tuple -&gt; (tuple.length() % 2) == 1);
     * </pre>
     * <P>
     * Example of splitting a stream of log records by their level attribute:
     * </P>
     * 
     * <pre>
     * <code>
     * TStream&lt;LogRecord&gt; lrs = ...
     * List&lt;&lt;TStream&lt;LogRecord&gt;&gt; splits = lrr.split(3, lr -&gt; {
            if (SEVERE.equals(lr.getLevel()))
                return 0;
            else if (WARNING.equals(lr.getLevel()))
                return 1;
            else
                return 2;
        });
     * splits.get(0). ... // SEVERE log record processing pipeline
     * splits.get(1). ... // WARNING log record  processing pipeline
     * splits.get(2). ... // "other" log record processing pipeline
     * </code>
     * </pre>
     * 
     * @param n
     *            the number of output streams
     * @param splitter
     *            the splitter function
     * @return List of {@code n} streams
     * 
     * @throws IllegalArgumentException
     *             if {@code n <= 0}
     */
    List<TStream<T>> split(int n, ToIntFunction<T> splitter);

    /**
     * Split a stream's tuples among {@code enumClass.size} streams as specified by
     * {@code splitter}.
     *
     * @param <E> Enum type
     * @param enumClass
     *            enum data to split
     * @param splitter
     *            the splitter function
     * @return EnumMap&lt;E,TStream&lt;T&gt;&gt;
     * @throws IllegalArgumentException
     * if {@code enumclass.size <= 0}
     */
    <E extends Enum<E>> EnumMap<E,TStream<T>> split(Class<E> enumClass, Function<T, E> splitter);

    /**
     * Declare a stream that contains the same contents as this stream while
     * peeking at each element using {@code peeker}. <BR>
     * For each tuple {@code t} on this stream, {@code peeker.accept(t)} will be
     * called.
     * 
     * @param peeker
     *            Function to be called for each tuple.
     * @return {@code this}
     */
    TStream<T> peek(Consumer<T> peeker);

    /**
     * Sink (terminate) this stream using a function. For each tuple {@code t} on this stream
     * {@link Consumer#accept(Object) sinker.accept(t)} will be called. This is
     * typically used to send information to external systems, such as databases
     * or dashboards.
     * <p>
     * If {@code sinker} implements {@link AutoCloseable}, its {@code close()}
     * method will be called when the org.apache.edgent.org.apache.edgent.topology's execution is terminated.
     * </P>
     * <P>
     * Example of terminating a stream of {@code String} tuples by printing them
     * to {@code System.out}.
     * </P>
     * 
     * <pre>
     * <code>
     * TStream&lt;String&gt; values = ...
     * values.sink(t -&gt; System.out.println(tuple));
     * </code>
     * </pre>
     * 
     * @param sinker
     *            Logic to be executed against each tuple on this stream.
     * @return sink element representing termination of this stream.
     */
    TSink<T> sink(Consumer<T> sinker);
    
    /**
     * Sink (terminate) this stream using a oplet.
     * This provides a richer api for a sink than
     * {@link #sink(Consumer)} with a full life-cycle of
     * the oplet as well as easy access to
     * {@link org.apache.edgent.execution.services.RuntimeServices runtime services}.
     * 
     * @param oplet Oplet processes each tuple without producing output.
     * @return sink element representing termination of this stream.
     */
    TSink<T> sink(Sink<T> oplet);

    /**
     * Declare a stream that contains the output of the specified {@link Pipe}
     * oplet applied to this stream.
     * 
     * @param <U> Tuple type of the returned stream.
     * @param pipe The {@link Pipe} oplet.
     * 
     * @return Declared stream that contains the tuples emitted by the pipe
     *      oplet. 
     */
    <U> TStream<U> pipe(Pipe<T, U> pipe);

    /**
     * Declare a stream that contains the output of the specified 
     * {@link FanIn} oplet applied to this stream and {@code others}.
     * 
     * @param <U> Tuple type of the returned streams.
     * @param fanin The {@link FanIn} oplet.
     * @param others The other input streams. 
     *        Must not be empty or contain duplicates or {@code this}
     * 
     * @return a stream that contains the tuples emitted by the oplet.
     * @see #union(Set)
     * @see #pipe(Pipe)
     * @see #sink(Sink)
     */
    <U> TStream<U> fanin(FanIn<T,U> fanin, List<TStream<T>> others);

    /**
     * Declare a new stream that modifies each tuple from this stream into one
     * (or zero) tuple of the same type {@code T}. For each tuple {@code t}
     * on this stream, the returned stream will contain a tuple that is the
     * result of {@code modifier.apply(t)} when the return is not {@code null}.
     * The function may return the same reference as its input {@code t} or
     * a different object of the same type.
     * If {@code modifier.apply(t)} returns {@code null} then no tuple
     * is submitted to the returned stream for {@code t}.
     * 
     * <P>
     * Example of modifying a stream  {@code String} values by adding the suffix '{@code extra}'.
     * </P>
     * 
     * <pre>
     * <code>
     * TStream&lt;String&gt; strings = ...
     * TStream&lt;String&gt; modifiedStrings = strings.modify(t -&gt; t.concat("extra"));
     * </code>
     * </pre>
     * 
     * <P>
     * This method is equivalent to
     * {@code map(Function<T,T> modifier}).
     * </P>
     * 
     * @param modifier
     *            Modifier logic to be executed against each tuple.
     * @return Stream that will contain tuples of type {@code T} modified from this
     *         stream's tuples.
     */
    TStream<T> modify(UnaryOperator<T> modifier);

    /**
     * Convert this stream to a stream of {@code String} tuples by calling
     * {@code toString()} on each tuple. This is equivalent to
     * {@code map(Object::toString)}.
     * 
     * @return Declared stream that will contain each the string representation
     *         of each tuple on this stream.
     */
    TStream<String> asString();

    /**
     * Utility method to print the contents of this stream
     * to {@code System.out} at runtime. Each tuple is printed
     * using {@code System.out.println(tuple)}.
     * @return {@code TSink} for the sink processing.
     */
    TSink<T> print();
    
    /**
     * Declare a partitioned window that continually represents the last {@code count}
     * tuples on this stream for each partition. Each partition independently maintains the last
     * {@code count} tuples for each key seen on this stream.
     * If no tuples have been seen on the stream for a key then the corresponding partition will be empty.
     * <BR>
     * The window is partitioned by each tuple's key, obtained by {@code keyFunction}.
     * For each tuple on the stream {@code keyFunction.apply(tuple)} is called
     * and the returned value is the tuple's key. For any two tuples {@code ta,tb} in a partition
     * {@code keyFunction.apply(ta).equals(keyFunction.apply(tb))} is true.
     * <BR>
     * The key function must return keys that implement {@code equals()} and {@code hashCode()} correctly.
     * <P>
     * To create a window partitioned using the tuple as the key use {@link org.apache.edgent.function.Functions#identity() identity()}
     * as the key function.
     * </P>
     * <P>
     * To create an unpartitioned window use a key function that returns a constant,
     * by convention {@link org.apache.edgent.function.Functions#unpartitioned() unpartitioned()} is recommended.
     * </P>
     * 
     * @param <K> Key type.
     * 
     * @param count Number of tuples to maintain in each partition.
     * @param keyFunction Function that defines the key for each tuple.
     * @return Window on this stream representing the last {@code count} tuples for each partition.
     */
    <K> TWindow<T, K> last(int count, Function<T, K> keyFunction);
    
    /**
     * Declare a partitioned window that continually represents the last {@code time} seconds of 
     * tuples on this stream for each partition. If no tuples have been 
     * seen on the stream for a key in the last {@code time} seconds then the partition will be empty.
     * Each partition independently maintains the last
     * {@code count} tuples for each key seen on this stream.
     * <BR>
     * The window is partitioned by each tuple's key, obtained by {@code keyFunction}.
     * For each tuple on the stream {@code keyFunction.apply(tuple)} is called
     * and the returned value is the tuple's key. For any two tuples {@code ta,tb} in a partition
     * {@code keyFunction.apply(ta).equals(keyFunction.apply(tb))} is true.
     * <BR>
     * The key function must return keys that implement {@code equals()} and {@code hashCode()} correctly.
     * <P>
     * To create a window partitioned using the tuple as the key use {@link org.apache.edgent.function.Functions#identity() identity()}
     * as the key function.
     * </P>
     * <P>
     * To create an unpartitioned window use a key function that returns a constant,
     * by convention {@link org.apache.edgent.function.Functions#unpartitioned() unpartitioned()} is recommended.
     * </P>
     * 
     * @param <K> Key type.
     * 
     * @param time Time to retain a tuple in a partition.
     * @param unit Unit for {@code time}.
     * @param keyFunction Function that defines the key for each tuple.
     * @return Partitioned window on this stream representing the last {@code count} tuple.
     */
    <K> TWindow<T, K> last(long time, TimeUnit unit, Function<T, K> keyFunction);
    
    /**
     * Declare a stream that will contain all tuples from this stream and
     * {@code other}. A stream cannot be unioned with itself, in this case
     * {@code this} will be returned.
     * 
     * @param other the other stream
     * @return A stream that is the union of {@code this} and {@code other}.
     */
    TStream<T> union(TStream<T> other);

    /**
     * Declare a stream that will contain all tuples from this stream and all the
     * streams in {@code others}. A stream cannot be unioned with itself, in
     * this case the union will only contain tuples from this stream once. If
     * {@code others} is empty or only contains {@code this} then {@code this}
     * is returned.
     * 
     * @param others
     *            Stream to union with this stream.
     * @return A stream that is the union of {@code this} and {@code others}.
     */
    TStream<T> union(Set<TStream<T>> others);
    
    /**
     * Adds the specified tags to the stream.  Adding the same tag to 
     * a stream multiple times will not change the result beyond the 
     * initial application.
     * 
     * @param values
     *            Tag values.
     * @return The tagged stream.
     */
    TStream<T> tag(String... values);

    /**
     * Returns the set of tags associated with this stream.
     * 
     * @return set of tags
     */
    Set<String> getTags(); 
    
    /**
     * Set an alias for the stream.
     * <p>
     * The alias must be unique within the org.apache.edgent.org.apache.edgent.topology.
     * The alias may be used in various contexts:
     * </p>
     * <ul>
     * <li>Runtime control services for the stream are registered with this alias.</li>
     * </ul>
     * 
     * @param alias an alias for the stream.
     * @return this
     * @throws IllegalStateException if the an alias has already been set.
     * @see ControlService
     */
    TStream<T> alias(String alias);
    
    /**
     * Returns the stream's alias if any.
     * @return the alias. null if one has not be set.
     */
    String getAlias();
    
    /**
     * Join this stream with a partitioned window of type {@code U} with key type {@code K}.
     * For each tuple on this stream, it is joined with the contents of {@code window}
     * for the key {@code keyer.apply(tuple)}. Each tuple is
     * passed into {@code joiner} and the return value is submitted to the
     * returned stream. If call returns null then no tuple is submitted.
     * 
     * @param <J> Tuple type of result stream
     * @param <U> Tuple type of window to join with
     * @param <K> Key type
     * @param keyer Key function for this stream to match the window's key.
     * @param window Keyed window to join this stream with.
     * @param joiner Join function.
     * @return A stream that is the results of joining this stream with
     *         {@code window}.
     */ 
    <J, U, K> TStream<J> join(Function<T, K> keyer, TWindow<U, K> window, BiFunction<T, List<U>, J> joiner);
    
    /**
     * Join this stream with the last tuple seen on a stream of type {@code U}
     * with partitioning.
     * For each tuple on this
     * stream, it is joined with the last tuple seen on {@code lastStream}
     * with a matching key (of type {@code K}).
     * <BR>
     * Each tuple {@code t} on this stream will match the last tuple
     * {@code u} on {@code lastStream} if
     * {@code keyer.apply(t).equals(lastStreamKeyer.apply(u))}
     * is true.
     * <BR>
     * The assumption is made that
     * the key classes correctly implement the contract for {@code equals} and
     * {@code hashCode()}.
     * <P>Each tuple is
     * passed into {@code joiner} and the return value is submitted to the
     * returned stream. If call returns null then no tuple is submitted.
     * </P>
     * @param <J> Tuple type of result stream
     * @param <U> Tuple type of stream to join with
     * @param <K> Key type
     * @param keyer Key function for this stream
     * @param lastStream Stream to join with.
     * @param lastStreamKeyer Key function for {@code lastStream}
     * @param joiner Join function.
     * @return A stream that is the results of joining this stream with
     *         {@code lastStream}.
     */
    <J, U, K> TStream<J> joinLast(Function<T, K> keyer, TStream<U> lastStream, Function<U, K> lastStreamKeyer, BiFunction<T, U, J> joiner);
    
}
