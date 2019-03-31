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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.oplet.OpletContext;

/**
 * FanIn oplet, merges multiple input ports into a single output port.
 * <P>
 * For each tuple received, {@code receiver.apply(T tuple, Integer index)}
 * is called. {@code index} is the tuple's input stream's index, where
 * {@code this} is index 0 followed by {@code others} in their order.
 * {@code receiver} either returns a tuple to emit on the output
 * stream or null.
 * </P> 
 * 
 * @param <T> Tuple type of input streams
 * @param <U> Tuple type of output stream
 */
public class FanIn<T,U> extends AbstractOplet<T, U> {
    private BiFunction<T, Integer, U> receiver;
    private List<Consumer<T>> iportConsumers;
    private Consumer<U> destination;
    
    public FanIn() {
    }
    
    public FanIn(BiFunction<T, Integer, U> receiver) {
      this.receiver = receiver;
    }

    @Override
    public void initialize(OpletContext<T, U> context) {
        super.initialize(context);
        destination = context.getOutputs().get(0);
       
        // Create a consumer for each input port.
        int numIports = getOpletContext().getInputCount();
        if (iportConsumers == null) {
          // each iport invokes the receiver
          iportConsumers = new ArrayList<>(numIports);
          for (int i = 0; i < numIports; i++)
            iportConsumers.add(consumer(i));
          iportConsumers = Collections.unmodifiableList(iportConsumers);
        }
    }
    
    /**
     * Set the receiver function.  Must be called no later than as part
     * of {@link #initialize(OpletContext)}.
     * @param receiver function to receive tuples
     */
    protected void setReceiver(BiFunction<T, Integer, U> receiver) {
      this.receiver = receiver;
    }

    @Override
    public void start() {
    }

    @Override
    public List<? extends Consumer<T>> getInputs() {
      return iportConsumers;
    }

    /**
     * Create a Consumer for the input port that invokes the
     * receiver and submits a generated tuple, if any, to the output.
     * @param iportIndex index of the input port
     * @return the Consumer
     */
    protected Consumer<T> consumer(int iportIndex) {
      return tuple -> { 
        U result = receiver.apply(tuple, iportIndex);
        if (result != null)
          submit(result);
      };
    }

    protected Consumer<U> getDestination() {
        return destination;
    }
    
    /**
     * Submit a tuple to single output.
     * @param tuple Tuple to be submitted.
     */
    protected void submit(U tuple) {
        getDestination().accept(tuple);
    }

    @Override
    public void close() {
    }
}
