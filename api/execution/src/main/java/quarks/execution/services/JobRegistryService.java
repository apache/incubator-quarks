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
package quarks.execution.services;

import java.util.Set;

import quarks.execution.Job;
import quarks.function.BiConsumer;

/**
 * Job registry service. 
 * <p>
 * Keeps the list of {@link Job} instances registered by the runtime and 
 * provides the necessary methods to register and remove jobs, access 
 * registered jobs, as well as register listeners which are notified on job 
 * registrations, removals, and updates.
 * The following event types are sent to registered listeners:
 * <ul>
 * <li>An {@link EventType#ADD} event is sent when a job is added.</li>
 * <li>An {@link EventType#REMOVE} event is sent when a job is removed.</li>
 * <li>An {@link EventType#UPDATE} event is sent when a job is updated.</li>
 * </ul>
 * <p>
 * <h3>Event dispatch</h3>
 * If a listener invocation throws an Exception, then the exception
 * will not prevent the remaining listeners from being invoked. However, 
 * if the invocation throws an Error, then it is recommended that 
 * the event dispatch stop.
 */
public interface JobRegistryService {
    /**
     * Job event types.
     */
    enum EventType {
        /** A Job has been added to the registry. */
        ADD,
        /** A Job has been removed from the registry. */
        REMOVE,
        /** A registered Job has been updated. */
        UPDATE
    }

    /**
     * Adds a handler to a collection of listeners that will be notified
     * on job registration and state changes.  Listeners will be notified 
     * in the order in which they are added.
     * <p>
     * A listener is notified of all existing jobs when it is first added.
     *
     * @param listener the listener that will be added
     * @throws IllegalArgumentException if the listener parameter is 
     *      {@code null}
     */
    void addListener(BiConsumer<JobRegistryService.EventType, Job> listener)
            throws IllegalArgumentException;

    /**
     * Removes a handler from this registry's collection of listeners.
     *
     * @param listener the listener that will be removed
     * @return whether or not the listener has been removed
     */
    boolean removeListener(
            BiConsumer<JobRegistryService.EventType, Job> listener);

    /**
     * Returns a set of all the registered job identifiers.
     *
     * @return the identifiers of all the jobs
     */
    Set<String> getJobIds();

    /**
     * Returns a job given its identifier.
     *
     * @return the job or {@code null} if no job is registered with that 
     *      identifier.
     */
    Job getJob(String id);

    /**
     * Adds the specified job.
     *
     * @param job the job to register
     *
     * @throws IllegalArgumentException if a job is null, or if a job with 
     *      the same identifier is already registered
     */
    void addJob(Job job) throws IllegalArgumentException;

    /**
     * Removes the job specified by the given identifier.
     *
     * @param jobId the identifier of the job to remove
     * @return whether or not the job was removed
     */
    boolean removeJob(String jobId);

    /**
     * Notifies listeners that the specified registered job has 
     * been updated.
     *
     * @param job the job
     * @return whether or not the job was found in the registry
     */
    boolean updateJob(Job job);

    // TODO add job retrieval given its name
}
