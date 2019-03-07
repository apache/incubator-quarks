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
package org.apache.edgent.oplet;

import java.util.List;

import org.apache.edgent.function.Consumer;

/**
 * Generic API for an oplet that processes streaming data on 0-N input ports
 * and produces 0-M output streams on its output ports. An input port may be
 * connected with any number of streams from other oplets. An output port may
 * connected to any number of input ports on other oplets.
 *
 * @param <I>
 *            Data container type for input tuples.
 * @param <O>
 *            Data container type for output tuples.
 */
public interface Oplet<I, O> extends AutoCloseable {

    /**
     * Initialize the oplet.
     * 
     * @param context the OpletContext
     * @throws Exception on failure
     */
    void initialize(OpletContext<I, O> context) throws Exception;

    /**
     * Start the oplet. Oplets must not submit any tuples not derived from
     * input tuples until this method is called.
     */
    void start();

    /**
     * Get the input stream data handlers for this oplet. The number of handlers
     * must equal the number of configured input ports. Each tuple
     * arriving on an input port will be sent to the stream handler for that
     * input port.
     * 
     * @return list of consumers
     */
    List<? extends Consumer<I>> getInputs();
}
