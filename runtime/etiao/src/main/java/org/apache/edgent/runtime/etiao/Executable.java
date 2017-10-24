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
package org.apache.edgent.runtime.etiao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.graph.Graph;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.runtime.etiao.graph.DirectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes and provides runtime services to the executable graph 
 * elements (oplets and functions).
 */
public class Executable implements RuntimeServices {

    private EtiaoJob job; // instantiated when topology is submitted
    private final ServiceContainer containerServices;
    private final ThreadFactory controlThreads;
    private final BiConsumer<Object, Throwable> completionHandler;
    private final ThreadFactoryTracker userThreads;
    private final TrackingScheduledExecutor controlScheduler;
    private final TrackingScheduledExecutor userScheduler;
    private Throwable lastError;
    private static final Logger logger = LoggerFactory.getLogger(Executable.class);

    /**
     * Services specific to this job.
     */
    private final ServiceContainer jobServices  = new ServiceContainer();

    private List<Invocation<? extends Oplet<?, ?>, ?, ?>> invocations = new ArrayList<>();

    /**
     * Creates a new {@code Executable} for the specified job.
     * @param name the name of the executable
     * @param containerServices runtime services provided by the container
     */
    public Executable(String name, ServiceContainer containerServices) {
        this(name, containerServices, null);
    }

    /**
     * Creates a new {@code Executable} for the specified topology name, which uses the 
     * given thread factory to create new threads for oplet execution.
     * 
     * @param name the name of the executable
     * @param containerServices runtime services provided by the container
     * @param threads thread factory for executing the oplets
     */
    public Executable(String name, ServiceContainer containerServices,  ThreadFactory threads) {
        this.containerServices = containerServices;
        this.controlThreads = (threads != null) ? threads : Executors.defaultThreadFactory();
        this.completionHandler = new BiConsumer<Object, Throwable>() {
            private static final long serialVersionUID = 1L;

            /**
             * Handler invoked by userThreads, userScheduler, and controlScheduler,
             * upon handling an uncaught exception from a user task or when they 
             * have completed all the tasks.
             * 
             * @param t The uncaught exception; null when called because all 
             *      tasks have completed.  
             */
            @Override
            public void accept(Object source, Throwable t) {
                if (job == null)
                    throw new IllegalStateException("A job has not been instantiated");

                if (t != null) {
                    Executable.this.setLastError(t);
                    job.updateHealth(t);
                    cleanup();
                }
                else if (job.getCurrentState() == Job.State.RUNNING &&
                        (source == userScheduler || source == userThreads) &&
                        !hasActiveTasks()) {
                    logger.info("No more active user tasks");
                }
                notifyCompleter();
            }  
        };
        this.userThreads = new ThreadFactoryTracker(name, controlThreads, completionHandler);
        this.controlScheduler = TrackingScheduledExecutor.newScheduler(controlThreads, completionHandler);
        this.userScheduler = TrackingScheduledExecutor.newScheduler(userThreads, completionHandler);
    }

    private ThreadFactory getThreads() {
        return userThreads;
    }

    /**
     * Returns the {@code ScheduledExecutorService} used for running 
     * executable graph elements.
     * 
     * @return the scheduler
     */
    public ScheduledExecutorService getScheduler() {
        return userScheduler;
    }
    
    /**
     * Acts as a service provider for executable elements in the graph, first
     * looking for a service specific to this job, and then one from the 
     * container.
     */
    @Override
    public <T> T getService(Class<T> serviceClass) {
        T service = jobServices.getService(serviceClass);
        if (service != null)
            return service;
                    
        return containerServices.getService(serviceClass);
    }

    /**
     * Creates a new {@code Invocation} associated with the specified oplet.
     *
     * @param <T> Oplet type
     * @param <I> Tuple type of input streams
     * @param <O> Tuple type of output streams
     * @param oplet the oplet
     * @param inputs the invocation's inputs
     * @param outputs the invocation's outputs
     * @return a new invocation for the given oplet
     */
    public <T extends Oplet<I, O>, I, O> Invocation<T, I, O> addOpletInvocation(T oplet, int inputs, int outputs) {
        Invocation<T, I, O> invocation = new Invocation<>(
        		Invocation.ID_PREFIX + invocations.size(), oplet, inputs, outputs);
        invocations.add(invocation);
        return invocation;
    }

    /**
     * Initializes the invocations.
     */
    public void initialize() {
        jobServices.addService(ThreadFactory.class, getThreads());
        jobServices.addService(ScheduledExecutorService.class, getScheduler());
        invokeAction(invocation -> invocation.initialize(job, this));
    }

    /**
     * Starts all the invocations.
     */
    public void start() {
        invokeAction(invocation -> invocation.start());
    }

    /**
     * Shuts down the user scheduler and thread factory, close all 
     * invocations, then shutdown the control scheduler.
     */
    public void close() {
        getScheduler().shutdownNow();
        userThreads.shutdownNow();
        
        invokeAction(invocation -> {
            try {
                invocation.close();
            }
            catch (Throwable t) {
                logger.debug("Exception caught while closing invocation {}: {}", invocation.getId(), t);
            } finally {
                jobServices.cleanOplet(job.getId(), invocation.getId());
                job.getContainerServices().cleanOplet(job.getId(), invocation.getId());
            }
        });

        notifyCompleter();
        List<Runnable> unfinished = controlScheduler.shutdownNow();
        if (!unfinished.isEmpty()) {
            logger.warn("Scheduler could not finish {} tasks", unfinished.size());
        }
    }
    
    private static long getTimeoutValue(long timeout, TimeUnit units) {
        // try to protect the tests from timing out prematurely
        // in the face of overloaded/slow build/test servers.
        if (Boolean.getBoolean("edgent.build.ci")) {
            // could do something like base the decision of the current value of timeout and/or units
            return timeout * 10;
        }
        return timeout;
    }

    private void invokeAction(Consumer<Invocation<?, ?, ?>> action) {
        ExecutorCompletionService<Boolean> completer = new ExecutorCompletionService<>(controlScheduler);
        for (Invocation<?, ?, ?> invocation : invocations) {
            completer.submit(() -> {
                action.accept(invocation);
                return true;
            });
        }

        long getFutureTimeout = 10;
        TimeUnit getFutureTimeoutUnits = TimeUnit.SECONDS;
        getFutureTimeout = getTimeoutValue(getFutureTimeout, getFutureTimeoutUnits);
        
        int remainingTasks = invocations.size();
        while (remainingTasks > 0) {
            try {
                Future<Boolean> completed = completer.poll(getFutureTimeout, getFutureTimeoutUnits);
                if (completed == null) {
                    // TODO during close log exception and wait on the next task to complete
                    throw new RuntimeException(new TimeoutException(
                            String.format("%d%s timeout", getFutureTimeout, getFutureTimeoutUnits.toString())));
                }
                else {
                    try {
                        completed.get();
                    }
                    catch (ExecutionException | InterruptedException | CancellationException e) {
                        logger.error("Exception caught while invoking action: {}", e);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Exception caught while waiting for future to complete", e);
            }
            remainingTasks--;
        }

        job.onActionComplete();
    }

    /**
     * Cleanup after failure.
     */
    private void cleanup() {
        userScheduler.shutdown();
        userThreads.shutdown();
    }

    /**
     * Check whether there are user tasks still active.
     * @return {@code true} if at least a user task is still active.
     */
    public boolean hasActiveTasks() {
        return userScheduler.hasActiveTasks() || 
               userThreads.hasActiveNonDaemonThreads();
    }

    public synchronized Throwable getLastError() {
        return lastError;
    }

    private synchronized void setLastError(Throwable lastError) {
        this.lastError = lastError;
    }

    public Job createJob(Graph graph, String topologyName, String jobName) {
        this.job = new EtiaoJob((DirectGraph)graph, topologyName, jobName, 
                containerServices);
        return this.job;
    }

    /**
     * The thread that is waiting for completion of the Executable's 
     * asynchronous work, may be null.
     */
    private Thread completer;
    private boolean completerNotify;

    /**
     * Waits for outstanding user threads or tasks.
     * 
     * @throws ExecutionException if the job execution threw an exception.  
     *      Wraps the latest uncaught Exception thrown by a background 
     *      activity.
     * @throws InterruptedException if the current thread was interrupted 
     *      while waiting
     * @return true if the {@code Executable} has completed, false if the if 
     *      the wait timed out.
     */
    final boolean complete(long timeoutMillis) 
            throws InterruptedException, ExecutionException {

        long totalWait = timeoutMillis;
        if (totalWait <= 0)
            totalWait = 1000;

        synchronized (this) {
            completer = Thread.currentThread();
        }

        final long start = System.currentTimeMillis();
        try {
            while ((System.currentTimeMillis() - start) < totalWait) {                
                if (Thread.interrupted())  // Clears interrupted status
                    throw new InterruptedException();
                
                // Check for errors from background activities
                Throwable t = getLastError();
                if (t != null) {
                    throw executionException(t);
                }
                
                if (!hasActiveTasks()) {
                    break;
                }
                
                // Wait for notification that something interesting to us has
                // terminated.
                synchronized (completer) {
                    if (!completerNotify) {
                        try {
                            completer.wait(totalWait);                        
                        } catch (InterruptedException e) {
                            if (!completerNotify) {
                                // Interrupted, but not by a notification
                                throw e;
                            }
                        }
                    }
                    completerNotify = false;
                }
            }
        } finally {
            synchronized (this) {
                completer = null;
            }
        }
        return ((System.currentTimeMillis() - start) < totalWait);
    }
    
    private void notifyCompleter() {
        Thread completer;
        synchronized (this) {
            completer = this.completer;
        }
        if (completer == null)
            return;
        
        synchronized (completer) {
            completerNotify = true;
            completer.notifyAll();
        }
    }
    
    static ExecutionException executionException(Throwable t) {
        return (t instanceof ExecutionException) ? 
                (ExecutionException) t : new ExecutionException(t);
    }
}
