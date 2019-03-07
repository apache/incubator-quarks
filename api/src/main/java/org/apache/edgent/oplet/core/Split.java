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

import static org.apache.edgent.function.Functions.closeFunction;

import java.util.Collections;
import java.util.List;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.oplet.OpletContext;

/**
 * Split a stream into multiple streams depending
 * on the result of a splitter function.
 * <BR>
 * For each tuple a function is called:
 * <UL>
 * <LI>If the return is negative the tuple is dropped.</LI>
 * <LI>Otherwise the return value is modded by the number of
 * output ports and the result is the output port index
 * the tuple is submitted to.</LI>
 * </UL>
 *
 * @param <T> Type of the tuple.
 */
public class Split<T> extends AbstractOplet<T, T> implements Consumer<T> {

    private static final long serialVersionUID = 1L;
    private final ToIntFunction<T> splitter;
    private List<? extends Consumer<T>> destinations;
    private int n;
    
    public Split(ToIntFunction<T> splitter) {
        this.splitter = splitter;
    }
    

    @Override
    public void initialize(OpletContext<T, T> context) {
        super.initialize(context);

        destinations = context.getOutputs();
        n = destinations.size();
    }

    @Override
    public void start() {
    }

    @Override
    public List<Consumer<T>> getInputs() {
        return Collections.singletonList(this);
    }

    @Override
    public void accept(T tuple) {
        int s = splitter.applyAsInt(tuple);
        if (s >= 0)
            destinations.get(s % n).accept(tuple);
    }

    @Override
    public void close() throws Exception {
        closeFunction(splitter);
    }
}
