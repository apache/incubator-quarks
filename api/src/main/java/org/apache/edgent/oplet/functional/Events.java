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
package org.apache.edgent.oplet.functional;


import static org.apache.edgent.function.Functions.closeFunction;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.oplet.core.Source;

/**
 * Generate tuples from events.
 * This oplet implements {@link Consumer} which
 * can be called directly from an event handler,
 * listener or callback. 
 * 
 * @param <T> Data container type for output tuples.
 */
public class Events<T> extends Source<T>implements Consumer<T> {

    private static final long serialVersionUID = 1L;
    private Consumer<Consumer<T>> eventSetup;

    public Events(Consumer<Consumer<T>> eventSetup) {
        this.eventSetup = eventSetup;
    }

    @Override
    public void close() throws Exception {
        closeFunction(eventSetup);
    }

    @Override
    public void start() {
        // Allocate a thread so the job containing this oplet
        // doesn't look "complete" and shutdown.
        Thread endlessEventSource = getOpletContext()
                .getService(ThreadFactory.class)
                .newThread(() -> {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    }
                    catch (InterruptedException e) {
                        // cancelled; we're done.
                    }
                });
        endlessEventSource.setDaemon(false);
        endlessEventSource.start();

        // It's possible for uses to do things like a blocking connect
        // to an external system from eventSetup.accept() so run it as
        // a task to avoid start() / submit-job timeouts.
        getOpletContext().getService(ScheduledExecutorService.class)
                    .submit(() -> eventSetup.accept(this));
    }

    @Override
    public void accept(T tuple) {
        submit(tuple);
    }
}
