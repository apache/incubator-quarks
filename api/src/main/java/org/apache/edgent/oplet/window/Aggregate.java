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
package org.apache.edgent.oplet.window;

import static org.apache.edgent.function.Functions.closeFunction;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.Pipe;
import org.apache.edgent.window.Window;

/**
 * Aggregate a window.
 * Window contents are aggregated by a
 * {@link BiFunction aggregator function}
 * passing the list of tuples in the window and
 * the partition key. The returned value
 * is submitted to the sole output port
 * if it is not {@code null}. 
 *
 * @param <T> Type of the input tuples.
 * @param <U> Type of the output tuples.
 * @param <K> Type of the partition key.
 */
public class Aggregate<T,U,K> extends Pipe<T, U> {
    private static final long serialVersionUID = 1L;
    private final Window<T,K, ? extends List<T>> window;
    /**
     * The aggregator provided by the user.
     */
    private final BiFunction<List<T>,K, U> aggregator;
    
    public Aggregate(Window<T,K, ? extends List<T>> window, BiFunction<List<T>,K, U> aggregator){
        this.aggregator = aggregator;
        BiConsumer<List<T>, K> partProcessor = (tuples, key) -> {
            U aggregateTuple = aggregator.apply(tuples, key);
            if (aggregateTuple != null)
                submit(aggregateTuple);
            };
            
        window.registerPartitionProcessor(partProcessor);
        this.window=window;
    }
    
    @Override
    public void initialize(OpletContext<T,U> context) {
        super.initialize(context);
        window.registerScheduledExecutorService(this.getOpletContext().getService(ScheduledExecutorService.class));
    }
    
    @Override
    public void accept(T tuple) {
        window.insert(tuple);   
    }

    @Override
    public void close() throws Exception {
        closeFunction(aggregator);
    }

}
