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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.graph.Graph;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.apache.edgent.topology.tester.Tester;

/**
 * A declaration of a org.apache.edgent.org.apache.edgent.topology of streaming data.
 * 
 * This class provides some fundamental generic methods to create source
 * streams, such as {@link #source(Supplier) source},
 * {@link #poll(Supplier, long, TimeUnit) poll}, 
 * {@link #strings(String...) strings}.
 * <P>
 * See <a href="doc-files/sources.html">Edgent Source Streams</a>.
 * </P>
 */
public interface Topology extends TopologyElement {

	/**
	 * Name of this org.apache.edgent.org.apache.edgent.topology.
	 * @return Name of this org.apache.edgent.org.apache.edgent.topology.
	 * @see TopologyProvider#newTopology(String)
	 */
    String getName();

    /**
     * Declare a new source stream that iterates over the return of
     * {@code Iterable<T> get()} from {@code data}. Once all the tuples from
     * {@code data.get()} have been submitted on the stream, no more tuples are
     * submitted. <BR>
     * The returned stream will be endless if the iterator returned from the
     * {@code Iterable} never completes.
     * <p>
     * If {@code data} implements {@link AutoCloseable}, its {@code close()}
     * method will be called when the org.apache.edgent.org.apache.edgent.topology's execution is terminated.
     * 
     * @param <T> Tuple type
     * @param data
     *            Function that produces that data for the stream.
     * @return New stream containing the tuples from the iterator returned by
     *         {@code data.get()}.
     * 
     * @see <a href="doc-files/sources.html">Edgent Source Streams</a>
     */
    <T> TStream<T> source(Supplier<Iterable<T>> data);

    /**
     * Declare an endless source stream. {@code data.get()} will be called
     * repeatably. Each non-null returned value will be present on the stream.
     * <p>
     * If {@code data} implements {@link AutoCloseable}, its {@code close()}
     * method will be called when the org.apache.edgent.org.apache.edgent.topology's execution is terminated.
     * 
     * @param <T> Tuple type
     * @param data
     *            Supplier of the tuples.
     * @return New stream containing the tuples from calls to {@code data.get()}
     *         .
     * 
     * @see <a href="doc-files/sources.html">Edgent Source Streams</a>
     */
    <T> TStream<T> generate(Supplier<T> data);

    /**
     * Declare a new source stream that calls {@code data.get()} periodically.
     * Each non-null value returned will appear on the returned stream. Thus
     * each call to {code data.get()} will result in zero tuples or one tuple on
     * the stream.
     * <p>
     * If {@code data} implements {@link AutoCloseable}, its {@code close()}
     * method will be called when the org.apache.edgent.org.apache.edgent.topology's execution is terminated.
     * </p><p>
     * The poll rate may be changed when the org.apache.edgent.org.apache.edgent.topology is running via a runtime
     * {@link org.apache.edgent.execution.mbeans.PeriodMXBean PeriodMXBean}.
     * In order to use this mechanism the caller must provide a 
     * alias for the stream when building the org.apache.edgent.org.apache.edgent.topology.
     * The {@code PeriodMXBean} is registered with the {@link ControlService}
     * with type {@link TStream#TYPE} and the stream's alias.  
     * e.g.,
     * </p>
     * <pre>{@code
     * Topology t = ...
     * TStream<Integer> stream = t.poll(...).alias("myStreamControlAlias");
     * 
     * // change the poll frequency at runtime
     * static <T> void setPollFrequency(TStream<T> pollStream, long period, TimeUnit unit) {
     *     ControlService cs = t.getRuntimeServiceSupplier().getService(ControlService.class);
     *     String alias = pollStream.getAlias();
     *     PeriodMXBean control = cs.getControl(TStream.TYPE, alias, PeriodMXBean.class);
     *     control.setPoll(period, unit);
     * }
     * }</pre>
     *
     * @param <T> Tuple type
     * @param data
     *            Function that produces that data for the stream.
     * @param period
     *            Approximate period {code data.get()} will be called.
     * @param unit
     *            Time unit of {@code period}.
     * @return New stream containing the tuples returned by {@code data.get()}.
     * 
     * @see <a href="doc-files/sources.html">Edgent Source Streams</a>
     */
    <T> TStream<T> poll(Supplier<T> data, long period, TimeUnit unit);

    /**
     * Declare a stream populated by an event system. At startup
     * {@code eventSetup.accept(eventSubmitter))} is called by the runtime with
     * {@code eventSubmitter} being a {@code Consumer<T>}. Calling
     * {@code eventSubmitter.accept(t)} results in {@code t} being present on
     * the returned stream if it is not null. If {@code t} is null then no
     * action is taken. <BR>
     * It is expected that {@code eventSubmitter} is called from the event
     * handler callback registered with the event system.
     * <P>
     * Downstream processing is isolated from the event source
     * to ensure that event listener is not blocked by a long
     * or slow processing flow.
     * </P>
     * <p>
     * If {@code eventSetup} implements {@link AutoCloseable}, its {@code close()}
     * method will be called when the org.apache.edgent.org.apache.edgent.topology's execution is terminated.
     * </P>
     * 
     * @param <T> Tuple type
     * @param eventSetup handler to receive the {@code eventSubmitter}
     * @return New stream containing the tuples added by {@code eventSubmitter.accept(t)}.
     * 
     * @see PlumbingStreams#pressureReliever(TStream, org.apache.edgent.function.Function, int)
     * 
     * @see <a href="doc-files/sources.html">Edgent Source Streams</a>
     */
    <T> TStream<T> events(Consumer<Consumer<T>> eventSetup);

    /**
     * Declare a stream of strings.
     * @param strings Strings that will be present on the stream.
     * @return Stream containing all values in {@code strings}.
     */
    TStream<String> strings(String... strings);
    
    /**
     * Declare a stream of objects.
     * @param <T> Tuple type
     * @param values Values that will be present on the stream.
     * @return Stream containing all values in {@code values}.
     */
    @SuppressWarnings("unchecked")
    <T> TStream<T> of(T... values);

    /**
     * Declare a stream of constants from a collection.
     * The returned stream will contain all the tuples in {@code tuples}.
     * @param <T> Tuple type
     * @param tuples Tuples that will be present on the stream.
     * @return Stream containing all values in {@code tuples}.
     */
    <T> TStream<T> collection(Collection<T> tuples);
    
    /**
     * Get the tester for this org.apache.edgent.org.apache.edgent.topology.
     * 
     * @return tester for this org.apache.edgent.org.apache.edgent.topology.
     */
    Tester getTester();

    /**
     * Get the underlying org.apache.edgent.graph.
     * @return the underlying org.apache.edgent.graph.
     */
    Graph graph();
    
    /**
     * Return a function that at execution time
     * will return a {@link RuntimeServices} instance
     * a stream function can use. 
     * 
     * @return Function that at execution time
     * will return a {@link RuntimeServices} instance
     */
    Supplier<RuntimeServices> getRuntimeServiceSupplier();
}
