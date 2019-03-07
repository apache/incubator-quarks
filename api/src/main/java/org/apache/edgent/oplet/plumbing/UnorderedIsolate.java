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
package org.apache.edgent.oplet.plumbing;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.edgent.function.Functions;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.Pipe;

/**
 * Isolate upstream processing from downstream
 * processing without guaranteeing tuple order.
 * An executor is used for downstream processing
 * thus tuple order cannot be guaranteed as the
 * scheduler does not guarantee execution order.
 *
 * @param <T> Type of the tuple.
 */
public class UnorderedIsolate<T> extends Pipe<T,T> {
    private static final long serialVersionUID = 1L;
    
    private ScheduledExecutorService executor;
    
    @Override
    public void initialize(OpletContext<T, T> context) {
        super.initialize(context);
        executor = context.getService(ScheduledExecutorService.class);
    }

    @Override
    public void accept(T tuple) {
        executor.execute(Functions.delayedConsume(getDestination(), tuple));      
    }
    
    @Override
    public void close() throws Exception {
    }
}
