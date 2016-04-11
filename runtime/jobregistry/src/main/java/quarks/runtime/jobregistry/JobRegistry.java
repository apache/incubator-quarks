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
package quarks.runtime.jobregistry;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quarks.execution.Job;
import quarks.execution.services.JobRegistryService;
import quarks.execution.services.ServiceContainer;
import quarks.function.BiConsumer;

/**
 * Maintains a set of registered jobs and a set of listeners.
 * Notifies listeners on job additions, deletions and updates.
 */
public class JobRegistry implements JobRegistryService {

    /**
     * Creates and registers a {@link JobRegistry} with the given service 
     * container.
     * 
     * @param services provides access to service registration
     * @return service instance.
     */
    public static JobRegistryService createAndRegister(ServiceContainer services) {
        JobRegistryService service = new JobRegistry();
        services.addService(JobRegistryService.class, service);
        return service;        
    }

    private final ConcurrentMap<String /*JobId*/, Job> jobs;
    private final Broadcaster<JobRegistryService.EventType, Job> listeners;
    private static final Logger logger = LoggerFactory.getLogger(JobRegistry.class);

    /**
     * Creates a new {@link JobRegistry}.
     */
    public JobRegistry() {
        this.jobs = new ConcurrentHashMap<String, Job>();
        this.listeners = new Broadcaster<JobRegistryService.EventType, Job>();
    }

    @Override
    public void addListener(BiConsumer<JobRegistryService.EventType, Job> listener) {
        listeners.add(listener);
        
        synchronized (jobs) {
            for (Job job : jobs.values())
                listener.accept(JobRegistryService.EventType.ADD, job);
        }
    }

    @Override
    public boolean removeListener(BiConsumer<JobRegistryService.EventType, Job> listener) {
        return listeners.remove(listener);
    }

    @Override
    public Set<String> getJobIds() {
        return Collections.unmodifiableSet(jobs.keySet());
    }

    @Override
    public Job getJob(String id) {
        return jobs.get(id);
    }

    @Override
    public boolean removeJob(String jobId) {
        final Job removed = jobs.remove(jobId);
        if (removed != null) {
            listeners.onEvent(JobRegistryService.EventType.REMOVE, removed);
            return true;
        }
        return false;
    }

    @Override
    public void addJob(Job job) throws IllegalArgumentException {
        final Job existing = jobs.putIfAbsent(job.getId(), job);
        if (existing == null) {
            listeners.onEvent(JobRegistryService.EventType.ADD, job);
        } else {
            throw new IllegalArgumentException("A job with Id " + job.getId()
                + " already exists");
        }
    }

    @Override
    public boolean updateJob(Job job) {
        if (jobs.containsValue(job)) {
            listeners.onEvent(JobRegistryService.EventType.UPDATE, job);
            return true;
        }
        return false;
    }

    private static class Broadcaster<T, O> {
        private final List<BiConsumer<T, O>> listeners;

        Broadcaster() {
            this.listeners = new CopyOnWriteArrayList<BiConsumer<T, O>>();
        }

        void add(BiConsumer<T, O> listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Null listener") ;
            }
            listeners.add(listener);
        }

        boolean remove(BiConsumer<T, O> listener) {
            if (listener == null)
                return false;
            return listeners.remove(listener);
        }

        private void onEvent(T event, O job) {
            for (BiConsumer<T, O> listener : listeners) {
                try {
                    listener.accept(event, job);
                } catch (Exception e) {
                    logger.error("Exception caught while invoking listener:" + e);
                }
            }
        }
    }
}
