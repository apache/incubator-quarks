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

import java.util.List;

import org.apache.edgent.oplet.Oplet;

/**
 * A {@code Vertex} in a org.apache.edgent.graph.
 * <p>
 * A {@code Vertex} has an {@link Oplet} instance
 * that will be executed at runtime and zero or
 * more input ports and zero or more output ports.
 * Each output port is represented by a {@link Connector} instance.
 * 
 * @param <N> the type of the {@code Oplet}
 * @param <C> Data type the oplet consumes in its input ports.
 * @param <P> Data type the oplet produces on its output ports.
 */
public interface Vertex<N extends Oplet<C, P>, C, P> {

	/**
	 * Get the vertice's {@link Graph}.
	 * @return the org.apache.edgent.graph
	 */
    Graph graph();

    /**
     * Get the instance of the oplet that will be executed.
     * 
     * @return the oplet
     */
    N getInstance();

    /**
     * Get the vertice's collection of output connectors.
     * @return an immutable collection of the output connectors.
     */
    List<? extends Connector<P>> getConnectors();

    /**
     * Add an output port to the vertex.
     * @return {@code Connector} representing the output port.
     */
    Connector<P> addOutput();
}
