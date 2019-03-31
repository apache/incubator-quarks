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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.Pipe;

/**
 * Isolate upstream processing from downstream
 * processing guaranteeing tuple order.
 * Input tuples are placed at the tail of a queue
 * and dedicated thread removes them from the
 * head and is used for downstream processing.
 *
 * @param <T> Type of the tuple.
 */
public class Isolate<T> extends Pipe<T,T> {
    private static final long serialVersionUID = 1L;
    
    private Thread thread;
    private final LinkedBlockingQueue<T> tuples;
    
    /**
     * Create a new Isolate oplet.
     * <BR>
     * Same as Isolate(Integer.MAX_VALUE).
     */
    public Isolate() {
      this(Integer.MAX_VALUE);
    }
    
    /**
     * Create a new Isolate oplet.
     * @param queueCapacity size of the queue between the input stream
     *          and the output stream.
     *          {@link #accept(Object) accept} blocks when the queue is full.
     */
    public Isolate(int queueCapacity) {
      tuples = new LinkedBlockingQueue<>(queueCapacity);
    }
    
    @Override
    public void initialize(OpletContext<T, T> context) {
        super.initialize(context);
        thread = context.getService(ThreadFactory.class).newThread(() -> run());
    }
   
    @Override
    public void start() {
        super.start();
        thread.start();
    }

    @Override
    public void accept(T tuple) {
        try {
            tuples.put(tuple);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }      
    }

    private void run() {
        while (!Thread.interrupted()) {
            try {
                submit(tuples.take());
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    @Override
    public void close() throws Exception {
    }
    
}
