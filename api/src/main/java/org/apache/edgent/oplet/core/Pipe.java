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
package org.apache.edgent.oplet.core;

import java.util.Collections;
import java.util.List;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.oplet.OpletContext;

/**
 * Pipe oplet with a single input and output. 
 *
 * @param <I>
 *            Data container type for input tuples.
 * @param <O>
 *            Data container type for output tuples.
 */
public abstract class Pipe<I, O> extends AbstractOplet<I, O>implements Consumer<I> {
    private static final long serialVersionUID = 1L;

    private Consumer<O> destination;

    @Override
    public void initialize(OpletContext<I, O> context) {
        super.initialize(context);

        destination = context.getOutputs().get(0);
    }

    @Override
    public void start() {
    }

    @Override
    public List<Consumer<I>> getInputs() {
        return Collections.singletonList(this);
    }

    protected Consumer<O> getDestination() {
        return destination;
    }
    
    /**
     * Submit a tuple to single output.
     * @param tuple Tuple to be submitted.
     */
    protected void submit(O tuple) {
        getDestination().accept(tuple);
    }
}
