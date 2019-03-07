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
package org.apache.edgent.topology.spi;

import java.util.Collections;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Functions;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.oplet.core.Sink;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * Abstract stream that uses the functional primitives to implement most
 * methods.
 * <p>
 * The functional primitives are:
 * </p>
 * <UL>
 * <LI>{@link TStream#filter(org.apache.edgent.function.Predicate)}</LI>
 * <LI>{@link TStream#map(org.apache.edgent.function.Function)}</LI>
 * <LI>{@link TStream#sink(org.apache.edgent.function.Consumer)}
 * </UL>
 * These methods are unimplemented, thus left to the specific implementation
 * used to build the org.apache.edgent.org.apache.edgent.topology.
 *
 * @param <G>
 *            Type of the {@link Topology} implementation.
 * @param <T>
 *            Type of data on the stream.
 */
public abstract class AbstractTStream<G extends Topology, T> implements TStream<T> {

    private final G topology;

    protected AbstractTStream(G topology) {
        this.topology = topology;
    }

    @Override
    public G topology() {
        return topology;
    }
    
    protected void verify(TStream<T> other) {
        if (topology() != other.topology())
            throw new IllegalArgumentException();
    }
    
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
     * <pre>
     * <code>
     * TStream&lt;String&gt; strings = ...
     * TStream&lt;String&gt; modifiedStrings = strings.modify(t -&gt; t.concat("extra"));
     * </code>
     * </pre>
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
     @Override
     public TStream<T> modify(UnaryOperator<T> modifier) {
        return map(modifier);
    }

    /**
     * Convert this stream to a stream of {@code String} tuples by calling
     * {@code toString()} on each tuple. This is equivalent to
     * {@code map(Object::toString)}.
     * 
     * @return Declared stream that will contain each the string representation
     *         of each tuple on this stream.
     */
    @Override
    public TStream<String> asString() {
        return map(Object::toString);
    }

    /**
     * Utility method to print the contents of this stream to {@code System.out}
     * at runtime. Each tuple is printed using {@code System.out.println(tuple)}
     * .
     * 
     * @return {@code TSink} for the sink processing.
     */
    @Override
    public TSink<T> print() {
        return sink(tuple -> System.out.println(tuple));
    }

    /**
     * Declare a stream that will contain all tuples from this stream and
     * {@code other}. A stream cannot be unioned with itself, in this case
     * {@code this} will be returned.
     * 
     * @param other TStream
     * @return A stream that is the union of {@code this} and {@code other}.
     */
    @Override
    public TStream<T> union(TStream<T> other) {
        return union(Collections.singleton(other));
    }
    
    @Override
    public TSink<T> sink(Consumer<T> sinker) {
        return sink(new Sink<>(Functions.synchronizedConsumer(sinker)));
    }
}
