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
package org.apache.edgent.execution.mbeans;


import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.Action;


/**
 * Control interface for a job.
 */
public interface JobMXBean {
    /**
     * TYPE is used to identify this bean as a job bean when building the bean's {@code ObjectName}.
     * The value is {@value} 
     */
    String TYPE = "job";

    /**
     * Returns the identifier of the job.
     * 
     * @return the job identifier.
     */
    String getId();

    /**
     * Returns the name of the job.
     *  
     * @return the job name.
     */
    String getName();

    /**
     * Retrieves the current state of the job.
     *
     * @return the current state.
     */
    Job.State getCurrentState();

    /**
     * Retrieves the next execution state when the job makes a state 
     * transition.
     *
     * @return the destination state while in a state transition.
     */
    Job.State getNextState();

    /**
     * Returns the summarized health indicator of the job.  
     * 
     * @return the summarized Job health.
     */
    Job.Health getHealth();

    /**
     * Returns the last error message caught by the current job execution.  
     * @return the last error message or an empty string if no error has 
     *      been caught.
     */
    String getLastError();

    /**
     * Takes a current snapshot of the running org.apache.edgent.graph and returns it in JSON format.
     * <p>
     * <b>The org.apache.edgent.graph snapshot JSON format</b>
     * <p>
     * The top-level object contains two properties: 
     * <ul>
     * <li>{@code vertices}: Array of JSON objects representing the org.apache.edgent.graph vertices.</li>
     * <li>{@code edges}: Array of JSON objects representing the org.apache.edgent.graph edges (an edge joins two vertices).</li>
     * </ul>
     * The vertex object contains the following properties:
     * <ul>
     * <li>{@code id}: Unique identifier within a org.apache.edgent.graph's JSON representation.</li>
     * <li>{@code instance}: The oplet instance from the vertex.</li>
     * </ul>
     * The edge object contains the following properties:
     * <ul>
     * <li>{@code sourceId}: The identifier of the source vertex.</li>
     * <li>{@code sourceOutputPort}: The identifier of the source oplet output port connected to the edge.</li>
     * <li>{@code targetId}: The identifier of the target vertex.</li>
     * <li>{@code targetInputPort}: The identifier of the target oplet input port connected to the edge.</li>
     * </ul>
     *  
     * @return a JSON-formatted string representing the running org.apache.edgent.graph.
     */
    String graphSnapshot();

    /**
     * Initiates an execution state change.
     * 
     * @param action which triggers the state change.
     * @throws IllegalArgumentException if the job is not in an appropriate 
     *      state for the requested action, or the action is not supported.
     */
    void stateChange(Action action);
}
