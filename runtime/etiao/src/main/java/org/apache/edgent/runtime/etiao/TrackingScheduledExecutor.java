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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.edgent.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends a {@code ScheduledThreadPoolExecutor} with the ability to track 
 * scheduled tasks and cancel them in case a task completes abruptly due to 
 * an exception.
 * 
 * When all the tasks have completed, due to normal termination, or cancelled
 * due to an exception, the executor invokes a completion handler.
 */
public final class TrackingScheduledExecutor extends ScheduledThreadPoolExecutor {
    private final BiConsumer<Object, Throwable> completer;
    private static final Logger logger = LoggerFactory.getLogger(TrackingScheduledExecutor.class);

    /**
     * Creates an {@code TrackingScheduledExecutor} using the supplied thread 
     * factory and a completion handler.
     * 
     * @param threadFactory the thread factory to use
     * @param completionHandler handler invoked when all task have completed, 
     *      due to normal termination, exception, or cancellation.
     * @return a new (@code TrackingScheduledExecutor) instance.
     */
    public static TrackingScheduledExecutor newScheduler(
            ThreadFactory threadFactory, BiConsumer<Object, Throwable> completionHandler) {

        TrackingScheduledExecutor stpe = new TrackingScheduledExecutor(
                Runtime.getRuntime().availableProcessors() * 4, threadFactory, completionHandler);
        stpe.setKeepAliveTime(1, TimeUnit.SECONDS);
        stpe.allowCoreThreadTimeOut(true);
        return stpe;
    }

    private TrackingScheduledExecutor(int corePoolSize, ThreadFactory tf, 
            BiConsumer<Object, Throwable> completer) {
        super(corePoolSize, tf);
        this.completer = completer;
    }

    /**
     * Invoked by the super class after each task execution.
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            t = unwrapFutureThrowable((Future<?>) r);
        }
        if (t != null) {
            getLogger().error("Thread: " + Thread.currentThread().getName() +
                    ": task terminated with exception : ", t);
            cleanup();
            completer.accept(this, t);
        }
    }
    
    /**
     * asyncTasks contains all the Future for tasks that have been started 
     * and not completed. For non-periodic tasks we remove the future from 
     * asyncTasks once it is completed. Periodic tasks remain in asyncTasks 
     * until they have been cancelled.
     */
    private final Set<RunnableScheduledFuture<?>> asyncTasks = Collections
            .synchronizedSet(new HashSet<RunnableScheduledFuture<?>>());

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable,
            RunnableScheduledFuture<V> task) {
        return trackTask(task);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> c,
            RunnableScheduledFuture<V> task) {
        return trackTask(task);
    }

    private void cleanup() {
        cancelAllAsyncTasks(true);
    }

    private int cancelAllAsyncTasks(boolean mayInterruptIfRunning) {
        int notCanceled = 0;
        synchronized (asyncTasks) {
            // hmm have gotten CMEs here with testMultiTopologyPollWithError.
            // This seems to follow the required access pattern for synchronized collection iterator.
            // But obviously something's amiss.  There seem to be only a few other
            // asyncTasks modifiers:
            //    trackTask() - add
            //    hasActiveTasks() - iterates while synchronized and can remove
            //    removeTrack() - remove
            // Just to make things iron clad, synch the add and remove too
            for (RunnableScheduledFuture<?> task : asyncTasks) {
                if (!task.cancel(mayInterruptIfRunning))
                    notCanceled++;
            }
            // remove tasks which are done
            hasActiveTasks();
        }
        return notCanceled;
    }

    /**
     * Track an executed task so that we can determine through the complete
     * method when all background activity is complete.
     */
    private <V> RunnableScheduledFuture<V> trackTask(RunnableScheduledFuture<V> task) {
        task = new TrackedFuture<V>(task);
        synchronized(asyncTasks) { asyncTasks.add(task); } // see cancelAllAsyncTasks
        return task;
    }

    /**
     * Determines whether there are tasks which have started and not completed.
     * 
     * As a side effect, this method removes all tasks which are done but are
     * still in the tracking list.
     * 
     * @return {@code true} is active tasks exist.
     */
    public boolean hasActiveTasks() {
        boolean doesHaveTasks = false; 
        synchronized (asyncTasks) {
            if (asyncTasks.isEmpty())
                return false;
            
            Iterator<RunnableScheduledFuture<?>> i = asyncTasks.iterator();
            while (i.hasNext()) {
                RunnableScheduledFuture<?> task = i.next();
                if (task.isDone())
                     i.remove();
                else
                    doesHaveTasks = true;
            }
        }
        return doesHaveTasks;
    }

    /**
     * Get the reason of a task's abnormal completion. Callers may cancel and
     * reschedule tasks, so a task completed by cancellation is not an error.
     */
    private final Throwable unwrapFutureThrowable(Future<?> ft) {
        if (ft.isDone() && !ft.isCancelled()) {
            try {
                ft.get();
            } catch (ExecutionException ee) {
                return ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        return null;
    }

    private Logger getLogger() {
        return logger;
    }

    /**
     * Track a non-periodic future so that we are aware of all background
     * activity. Simply wrap the RunnableScheduledFuture that our executor
     * returns ensuring that the future is removed from the list of tracked
     * tasks once it is complete.
     */
    final class TrackedFuture<V> implements RunnableScheduledFuture<V> {

        private final RunnableScheduledFuture<V> realTask;

        TrackedFuture(RunnableScheduledFuture<V> realTask) {
            this.realTask = realTask;
        }

        @Override
        public void run() {
            try {
                realTask.run();
            } finally {
                if (!isPeriodic())
                    removeTrack();
            }
        }

        /**
         * Remove tracking of the task and notify the completer if
         * the scheduler seems to have no work.
         */
        private void removeTrack() {
            synchronized(asyncTasks) { asyncTasks.remove(this); } // see cancelAllAsyncTasks

            // Notify the completer if the following is true:
            // no asyncTasks (user tasks) pending, or the executor's task 
            // queue is empty and the number of active threads includes 
            // only the current one.
            if (asyncTasks.isEmpty() ||
                    (getActiveCount() <= 1 && getQueue().isEmpty())) {
                completer.accept(TrackingScheduledExecutor.this, null);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean wasCancelled = realTask.cancel(mayInterruptIfRunning);
            if (wasCancelled)
                removeTrack();
            return wasCancelled;
        }

        @Override
        public boolean isCancelled() {
            return realTask.isCancelled();
        }

        @Override
        public boolean isDone() {
            return realTask.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return realTask.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return realTask.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return realTask.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return realTask.compareTo(o);
        }

        @Override
        public boolean isPeriodic() {
            return realTask.isPeriodic();
        }
    }
}
