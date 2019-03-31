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
package org.apache.edgent.execution;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Actions and states for execution of an Edgent job.
 * 
 * The interface provides the main job lifecycle control, taking on the following 
 * execution state values:
 *
 * <ul>
 * <li><b>CONSTRUCTED</b>  This job has been constructed but the 
 *      nodes are not yet initialized.</li>
 * <li><b>INITIALIZED</b>  This job has been initialized and is 
 *      ready to process data.
 * <li><b>RUNNING</b>  This job is processing data.</li>
 * <li><b>PAUSED</b>  This job is paused.</li>
 * <li><b>CLOSED</b>  This job is closed.</li>
 * </ul>
 * 
 * The interface provides access to two state values:
 * <ul>
 * <li> {@link #getCurrentState() Current} - The current state of execution 
 *      when the job is not executing a state transition; the source state
 *      while the job is making a state transition after the client code 
 *      calls {@link #stateChange(Job.Action)}.</li>
 * <li> {@link #getNextState() Next} - The destination state while the job 
 *      is making a state transition; same as the current state while 
 *      the job state is stable (that is, not making a transition).</LI>
 * </ul>
 * 
 * The interface provides access to the job nodes 
 * {@linkplain #getHealth() health summary}, described by the following values:
 * <ul>
 * <li><b>HEALTHY</b>  All org.apache.edgent.graph nodes in the job are healthy.</li>
 * <li><b>UNHEALTHY</b>  At least one org.apache.edgent.graph node in the job is stopped or
 *      stopping.</li>
 * </ul>
 */
public interface Job {
    /**
     * States of a org.apache.edgent.graph job.
     */
    public enum State {
        /** Initial state, the org.apache.edgent.graph nodes are not yet initialized. */
        CONSTRUCTED, 
        /** All the org.apache.edgent.graph nodes have been initialized. */
        INITIALIZED,
        /** All the org.apache.edgent.graph nodes are processing data. */
        RUNNING, 
        /** All the org.apache.edgent.graph nodes are paused. */
        PAUSED, 
        /** All the org.apache.edgent.graph nodes are closed. */
        CLOSED
    }

    /**
     * Enumeration for the summarized health indicator of the org.apache.edgent.graph nodes.
     */
    public enum Health { 
        /** 
         * All org.apache.edgent.graph nodes in the job are healthy.
         */
        HEALTHY,
        /** 
         * The execution of at least one org.apache.edgent.graph node in the job has stopped
         * because of an abnormal condition.
         */
        UNHEALTHY
    }

    /**
     * Actions which trigger {@link Job.State} transitions.
     */
    public enum Action {
        /** Initialize the job */
        INITIALIZE,
        /** Start the execution. */
        START, 
        /** Pause the execution. */
        PAUSE,
        /** Resume the execution */
        RESUME,
        /** Close the job. */
        CLOSE
    }

    /**
     * Retrieves the current state of this job.
     *
     * @return the current state.
     */
    State getCurrentState();

    /**
     * Retrieves the next execution state when this job makes a state 
     * transition.
     *
     * @return the destination state while in a state transition; 
     *      otherwise the same as {@link #getCurrentState()}.
     */
    State getNextState();

    /**
     * Initiates an execution state change.
     * 
     * @param action which triggers the state change.
     * @throws IllegalArgumentException if the job is not in an appropriate 
     *      state for the requested action, or the action is not supported.
     */
    void stateChange(Action action) throws IllegalArgumentException;
    
    /**
     * Returns the summarized health indicator of the org.apache.edgent.graph nodes.
     * 
     * @return the summarized Job node health.
     */
    Health getHealth();
    
    /**
     * Returns the last error message caught by the current job execution.  
     * @return the last error message or an empty string if no error has 
     *      been caught.
     */
    String getLastError();

    /**
     * Returns the name of this job. The name may be set when the job is 
     * {@linkplain org.apache.edgent.execution.Submitter#submit(java.lang.Object,com.google.gson.JsonObject) submitted}.
     * Implementations may create a job name if one is not specified at submit time.
     *
     * @return the job name.
     */
    String getName();

    /**
     * Returns the identifier of this job.
     * 
     * @return this job identifier.
     */
    String getId();

    /**
     * Waits for any outstanding job work to complete.
     * 
     * @throws ExecutionException if the job execution threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    void complete() throws ExecutionException, InterruptedException;

    /**
     * Waits for at most the specified time for the job to complete.
     * 
     * @param timeout the time to wait
     * @param unit the time unit of the timeout argument
     * 
     * @throws ExecutionException if the job execution threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if the wait timed out
     */
    void complete(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException;
}
