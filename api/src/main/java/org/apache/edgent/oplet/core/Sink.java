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
import org.apache.edgent.function.Functions;

/**
 * Sink a stream by processing each tuple through
 * a {@link Consumer}.
 * If the {@code sinker} function implements {@code AutoCloseable}
 * then when this oplet is closed {@code sinker.close()} is called.
 *
 * @param <T> Tuple type.
 */
public class Sink<T> extends AbstractOplet<T, Void> {
    
    private Consumer<T> sinker;

    /**
     * Create a  {@code Sink} that discards all tuples.
     * The sink function can be changed using
     * {@link #setSinker(Consumer)}.
     */
    public Sink() {
        setSinker(Functions.discard());
    }
    
    /**
     * Create a {@code Sink} oplet.
     * @param sinker Processing to be performed on each tuple.
     */
    public Sink(Consumer<T> sinker) {
        setSinker(sinker);
    }

    @Override
    public List<Consumer<T>> getInputs() {
        return Collections.singletonList(getSinker());
    }

    @Override
    public void start() {
    }
    
    @Override
    public void close() throws Exception {
        closeFunction(getSinker());
    }
    
    /**
     * Set the sink function.
     * @param sinker Processing to be performed on each tuple.
     */
    protected void setSinker(Consumer<T> sinker) {
        this.sinker = sinker;
    }
    
    /**
     * Get the sink function that processes each tuple.
     * @return function that processes each tuple.
     */
    protected Consumer<T> getSinker() {
        return sinker;
    }
}
