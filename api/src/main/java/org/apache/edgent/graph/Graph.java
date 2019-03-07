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
package org.apache.edgent.graph;

import java.util.Collection;

import org.apache.edgent.function.Predicate;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.oplet.core.Peek;
import org.apache.edgent.oplet.core.Source;

/**
 * A generic directed org.apache.edgent.graph of vertices, connectors and edges.
 * <p>
 * The org.apache.edgent.graph consists of {@link Vertex} objects, each having
 * 0 or more input and/or output {@link Connector} objects.
 * {@link Edge} objects connect an output connector to
 * an input connector.
 * <p>
 * A vertex has an associated {@link Oplet} instance that will be executed
 * at runtime.
 */
public interface Graph {

    /**
     * Add a new unconnected {@code Vertex} into the org.apache.edgent.graph.
     * <p>
     * 
     * @param <N> an Oplet type
     * @param <C> tuple type of input streams
     * @param <P> tuple type of output streams
     * @param oplet the oplet to associate with the new vertex
     * @param inputs the number of input connectors for the new vertex
     * @param outputs the number of output connectors for the new vertex
     * @return the newly created {@code Vertex} for the oplet
     */
    <N extends Oplet<C, P>, C, P> Vertex<N, C, P> insert(N oplet, int inputs, int outputs);

    /**
     * Create a new unconnected {@link Vertex} associated with the
     * specified source {@link Oplet}.
     * <p>
     * The {@code Vertex} for the oplet has 0 input connectors and one output connector.
     * @param <N> a Source type
     * @param <P> tuple type
     * @param oplet the source oplet
     * @return the output connector for the newly created vertex.
     */
    <N extends Source<P>, P> Connector<P> source(N oplet);

    /**
     * Create a new connected {@link Vertex} associated with the
     * specified {@link Oplet}.
     * <p>
     * The new {@code Vertex} has one input and one output {@code Connector}.
     * An {@link Edge} is created connecting the specified output connector to
     * the new vertice's input connector.
     * 
     * @param <N> an Oplet type
     * @param <C> tuple type of input streams
     * @param <P> tuple type of output streams
     * @param output the output connector to connect to the vertice's input connector
     * @param oplet the oplet to associate with the new {@code Vertex}
     * @return the output connector for the new {@code Vertex}
     */
	<N extends Oplet<C, P>, C, P> Connector<P> pipe(Connector<C> output, N oplet);

    /**
     * Insert Peek oplets returned by the specified {@code Supplier} into 
     * the outputs of all of the oplets which satisfy the specified 
     * {@code Predicate} and where the output's {@link Connector#isConnected()}
     * is true.
     * 
     * @param supplier
     *            Function which provides a Peek oplet to insert
     * @param select
     *            Vertex selection Predicate
     */
    void peekAll(Supplier<? extends Peek<?>> supplier, Predicate<Vertex<?, ?, ?>> select);

    /**
     * Return an unmodifiable view of all vertices in this org.apache.edgent.graph.
     * 
     * @return unmodifiable view of all vertices in this org.apache.edgent.graph
     */
    Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> getVertices();
    
    /**
     * Return an unmodifiable view of all edges in this org.apache.edgent.graph.
     * @return the collection
     */
    Collection<Edge> getEdges();
}

