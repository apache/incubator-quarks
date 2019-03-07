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

import java.util.Set;

import org.apache.edgent.oplet.core.Peek;

/**
 * A {@code Connector} represents an output port of a {@code Vertex}.
 * 
 * A {@code Connector} supports two methods to add processing for tuples
 * submitted to the port:
 * <UL>
 * <LI>{@link #connect(Vertex, int)} : Connect this to an input port of another
 * {@code Vertex}. Any number of connections can be made. Any tuple submitted by
 * the output port will appear on all connections made through this method. For
 * any tuple {@code t} ordering of appearance across the connected input ports
 * is not guaranteed.</LI>
 * <LI>{@link #peek(Peek)} : Insert a peek after the output port and before any
 * connections made by {@link #connect(Vertex, int)}. Multiple peeks can be
 * inserted. A tuple {@code t} submitted by the output port will be seen by all
 * peek oplets. The ordering of the peek is guaranteed such that the peeks
 * are processed in the order they were added to this {@code Connector} with the
 * {@code t} being seen first by the first peek added.
 * <LI>
 * </UL>
 * For example with peeks {@code P1,P2,P3} added in that order and connections
 * {@code C1,C2} added, the org.apache.edgent.graph will be logically:
 *
 * <pre>
 * {@code
 *                      -->C1
 * port-->P1-->P2-->P3--|
 *                      -->C2
 * }
 * </pre>
 * 
 * A tuple {@code t} submitted by the port will be peeked at by {@code P1}, then
 * {@code P2} then {@code P3}. After {@code P3} peeked at the tuple, {@code C1}
 * and {@code C2} will process the tuple in an arbitrary order.
 * 
 * @param <T>
 *            Type of the data item produced by the output port
 */
public interface Connector<T> {
	
	/**
	 * Gets the {@code Graph} for this {@code Connector}.
	 * 
	 * @return the {@code Graph} for this {@code Connector}.
	 */
    Graph graph();

    /**
     * Connect this {@code Connector} to the specified target's input. This
     * method may be called multiple times to fan out to multiple input ports.
     * Each tuple submitted to this output port will be processed by all
     * connections.
     * 
     * @param target
     *            the {@code Vertex} to connect to
     * @param inputPort
     *            the index of the target's input port to connect to.
     */
    void connect(Vertex<?, T, ?> target, int inputPort);
    
	/**
	 * Was connect() called on this connector?
	 * 
	 * @return true if connected
	 */
    boolean isConnected();

	/**
     * Inserts a {@code Peek} oplet between an output port and its
     * connections. This method may be called multiple times to insert multiple
     * peeks. Each tuple submitted to this output port will be seen by all peeks
     * in order of their insertion, starting with the first peek inserted.
     *
     * @param <N> Peek oplet type
     * @param oplet
     *            Oplet to insert.
     * @return {@code output}
     */
	<N extends Peek<T>> Connector<T> peek(N oplet);

    /**
     * Adds the specified tags to the connector.  Adding the same tag 
     * multiple times will not change the result beyond the initial 
     * application. An unconnected connector can be tagged.
     * 
     * @param values
     *            Tag values.
     */
    void tag(String... values);

    /**
     * Returns the set of tags associated with this connector.
     * 
     * @return set of tag values.
     */
    Set<String> getTags();
    
    /**
     * Set the alias for the connector.
     * <p>
     * The alias must be unique within the org.apache.edgent.org.apache.edgent.topology.
     * The alias may be used in various contexts:
     * </p>
     * <ul>
     * <li>Runtime control services for the Connector (stream/outputport)
     * are registered with this alias.</li>
     * </ul>
     * 
     * @param alias the alias
     * @throws IllegalStateException if the an alias has already been set
     */
    void alias(String alias);
    
    /**
     * Returns the alias for the connector if any.
     * @return the alias. null if one has not be set.
     */
    String getAlias();

}
