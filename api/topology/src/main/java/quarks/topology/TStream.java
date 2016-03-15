/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.topology;

import quarks.function.*;
import quarks.oplet.core.Pipe;
import quarks.oplet.core.Sink;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A {@code TStream} is a declaration of a continuous sequence of tuples. A
 * connected topology of streams and functional transformations is built using
 * {@link Topology}. <BR>
 * Generic methods on this interface provide the ability to
 * {@link #filter(Predicate) filter}, {@link #map(Function)
 * map (or transform)} or {@link #sink(Consumer) sink} this declared stream using a
 * function.
 * <P>
 * {@code TStream} is not a runtime representation of a stream,
 * it is a declaration used in building a topology.
 * The actual runtime stream is created once the topology
 * is {@link quarks.execution.Submitter#submit(Object) submitted}
 * to a runtime.
 * 
 * </P>
 * @param <T>
 *            Tuple type.
 */
public interface TStream<T> extends TopologyElement {

    /**
     * Declare a new stream that filters tuples from this stream. Each tuple
     * {@code t} on this stream will appear in the returned stream if
     * {@link Predicate#test(Object) filter.test(t)} returns {@code true}. If
     * {@code filter.test(t)} returns {@code false} then then {@code t} will not
     * appear in the returned stream.
     * <P>
     * Examples of filtering out all empty strings from stream {@code s} of type
     * {@code String}
     * 
     * <pre>
     * <code>
     * TStream&lt;String> s = ...
     * TStream&lt;String> filtered = s.filter(t -> !t.isEmpty());
     *             
     * </code>
     * </pre>
     * 
     * </P>
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
     * 
     * <pre>
     * <code>
     * // Using lambda expression
     * TStream&lt;String> strings = ...
     * TStream&lt;Double> doubles = strings.map(v -> Double.valueOf(v));
     * 
     * // Using method reference
     * TStream&lt;String> strings = ...
     * TStream&lt;Double> doubles = strings.map(Double::valueOf);
     * 
     * </code>
     * </pre>
     * 
     * </P>
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
     * 
     * <pre>
     * <code>
     * TStream&lt;String> lines = ...
     * TStream&lt;String> words = lines.flatMap(
     *                     line -> Arrays.asList(line.split(" ")));
     *             
     * </code>
     * </pre>
     * 
     * </P>
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
     * 
     * <pre>
     * if r < 0 the tuple is discarded
     * else it is sent to the stream at position (r % n) in the returned array.
     * </pre>
     * </P>
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
     * 
     * <pre>
     * List&lt;TStream&lt;String>> streams = stream.split(2, tuple -> tuple.length());
     * 
     * TStream&lt;String> stream0 = stream.filter(tuple -> (tuple.length() % 2) == 0);
     * TStream&lt;String> stream1 = stream.filter(tuple -> (tuple.length() % 2) == 1);
     * </pre>
     * </P>
     * <P>
     * Example of splitting a stream of log records by their level attribute:
     * 
     * <pre>
     * <code>
     * TStream&lt;LogRecord> lrs = ...
     * List&lt;&lt;TStream&lt;LogRecord>> splits = lrr.split(3, lr -> {
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
     * </P>
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
     * @param enumClass
     *            enum data to split
     * @param splitter
     *            the splitter function
     * @return EnumMap<E,TStream<T>>
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
     * method will be called when the topology's execution is terminated.
     * <P>
     * Example of terminating a stream of {@code String} tuples by printing them
     * to {@code System.out}.
     * 
     * <pre>
     * <code>
     * TStream&lt;String> values = ...
     * values.sink(t -> System.out.println(tuple));
     * </code>
     * </pre>
     * 
     * </P>
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
     * {@link quarks.execution.services.RuntimeServices runtime services}.
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
     * 
     * <pre>
     * <code>
     * TStream&lt;String> strings = ...
     * TStream&lt;String> modifiedStrings = strings.modify(t -> t.concat("extra"));
     * </code>
     * </pre>
     * 
     * </P>
     * <P>
     * This method is equivalent to
     * {@code map(Function<T,T> modifier}).
     * </P
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
     * To create a window partitioned using the tuple as the key use {@link quarks.function.Functions#identity() identity()}
     * as the key function.
     * </P>
     * <P>
     * To create an unpartitioned window use a key function that returns a constant,
     * by convention {@link quarks.function.Functions#unpartitioned() unpartitioned()} is recommended.
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
     * To create a window partitioned using the tuple as the key use {@link quarks.function.Functions#identity() identity()}
     * as the key function.
     * </P>
     * <P>
     * To create an unpartitioned window use a key function that returns a constant,
     * by convention {@link quarks.function.Functions#unpartitioned() unpartitioned()} is recommended.
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
     * @param other
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
}
