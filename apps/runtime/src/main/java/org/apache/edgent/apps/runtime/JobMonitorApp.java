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
package org.apache.edgent.apps.runtime;

import static org.apache.edgent.topology.services.ApplicationService.SYSTEM_APP_PREFIX;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.edgent.execution.DirectSubmitter;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.Action;
import org.apache.edgent.execution.mbeans.JobMXBean;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.Controls;
import org.apache.edgent.execution.services.JobRegistryService;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.runtime.jobregistry.JobEvents;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.TopologyProvider;
import org.apache.edgent.topology.mbeans.ApplicationServiceMXBean;
import org.apache.edgent.topology.services.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Job monitoring application.
 * <P>
 * The application listens on JobRegistry events and resubmits jobs for which 
 * an event has been emitted because the job is unhealthy. The monitored 
 * applications must be registered with an {@code ApplicationService} 
 * prior to submission, otherwise the monitor application cannot restart 
 * them.
 * </P>
 * <P>
 * The monitoring application must be submitted within a context which 
 * provides the following services:
 * </P>
 * <ul>
 * <li>ApplicationService - an {@code ApplicationServiceMXBean} control 
 * registered by this service is used to resubmit failed applications.</li>
 * <li>ControlService - the application queries this service for an 
 * {@code ApplicationServiceMXBean} control, which is then used for 
 * restarting failed applications.</li>
 * <li>JobRegistryService - generates job monitoring events. </li>
 * </ul>
 */
public class JobMonitorApp {
    /**
     * Job monitoring application name.
     */
    public static final String APP_NAME = SYSTEM_APP_PREFIX + "JobMonitorApp";

    
    private final TopologyProvider provider;
    private final DirectSubmitter<Topology, Job> submitter;
    private final Topology topology;
    private static final Logger logger = LoggerFactory.getLogger(JobMonitorApp.class);

    /**
     * Constructs a {@code JobMonitorApp} with the specified name in the 
     * context of the specified provider.
     * 
     * @param provider the org.apache.edgent.org.apache.edgent.topology provider
     * @param submitter a {@code DirectSubmitter} which provides required 
     *      services and submits the application
     * @param name the application name
     * 
     * @throws IllegalArgumentException if the submitter does not provide 
     *      access to the required services
     */
    public JobMonitorApp(TopologyProvider provider, 
            DirectSubmitter<Topology, Job> submitter, String name) {

        this.provider = provider;
        this.submitter = submitter;
        validateSubmitter();
        this.topology = declareTopology(name);
    }
    
    /**
     * Submits the application org.apache.edgent.org.apache.edgent.topology.
     * 
     * @return the job.
     * @throws InterruptedException if the operation was interrupted
     * @throws ExecutionException on task execution exception 
     */
    public Job submit() throws InterruptedException, ExecutionException {
        Future<Job> f = submitter.submit(topology);
        return f.get();
    }

    /**
     * Submits an application using an {@code ApplicationServiceMXBean} control 
     * registered with the specified {@code ControlService}.
     * 
     * @param applicationName the name of the application to submit
     * @param controlService the control service
     */
    public static void submitApplication(String applicationName, ControlService controlService) {
        try {
            ApplicationServiceMXBean control =
                    controlService.getControl(
                            ApplicationServiceMXBean.TYPE,
                            ApplicationService.ALIAS,
                            ApplicationServiceMXBean.class);
            if (control == null) {
                throw new IllegalStateException(
                        "Could not find a registered control with the following interface: " + 
                        ApplicationServiceMXBean.class.getName());                
            }
// TODO add ability to submit with the initial application configuration
            logger.info("Restarting monitored application {}", applicationName);
            control.submit(applicationName, null);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Closes a job using a {@code JobMXBean} control registered with the 
     * specified {@code ControlService}.
     * 
     * @param jobName the name of the job
     * @param controlService the control service
     */
    public static void closeJob(String jobName, ControlService controlService) {
        try {
            JobMXBean jobMbean = controlService.getControl(JobMXBean.TYPE, jobName, JobMXBean.class);
            if (jobMbean == null) {
                throw new IllegalStateException(
                        "Could not find a registered control for job " + jobName + 
                        " with the following interface: " + JobMXBean.class.getName());                
            }
            jobMbean.stateChange(Action.CLOSE);
            logger.debug("Closing job {}", jobName);
            
            // Wait for the job to complete
            long startWaiting = System.currentTimeMillis();
            for (long waitForMillis = Controls.JOB_HOLD_AFTER_CLOSE_SECS * 1000;
                    waitForMillis >= 0;
                    waitForMillis -= 100) {
                if (jobMbean.getCurrentState() == Job.State.CLOSED)
                    break;
                else
                    Thread.sleep(100);
            }
            if (jobMbean.getCurrentState() != Job.State.CLOSED) {
                throw new IllegalStateException(
                        "The unhealthy job " + jobName + " did not close after " + 
                        Controls.JOB_HOLD_AFTER_CLOSE_SECS + " seconds");                
            }
            logger.debug("Job {} state is CLOSED after waiting for {} milliseconds",
                    jobName, System.currentTimeMillis() - startWaiting);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Declares the following org.apache.edgent.org.apache.edgent.topology:
     * <pre>
     * JobEvents source --&gt; Filter (health == unhealthy) --&gt; Restart application
     * </pre>
     * 
     * @param name the org.apache.edgent.org.apache.edgent.topology name
     * @return the application org.apache.edgent.org.apache.edgent.topology
     */
    protected Topology declareTopology(String name) {
        Topology t = provider.newTopology(name);
        
        declareTopology(t);
        
        return t;
    }
    
    /**
     * Populates the following org.apache.edgent.org.apache.edgent.topology:
     * <pre>
     * JobEvents source --&gt; Filter (health == unhealthy) --&gt; Restart application
     * </pre>
     * @param t Topology
     *
     */
    public static void declareTopology(Topology t) {
        TStream<JsonObject> jobEvents = JobEvents.source(
                t, 
                (evType, job) -> { return JobMonitorAppEvent.toJsonObject(evType, job); }
                );

        jobEvents = jobEvents.filter(
                value -> {
                    logger.trace("Filter: {}", value);

                    try {
                        // Only trigger on the initial unhealthy event:
                        //     state:RUNNING nextState:RUNNING UNHEALTHY
                        // Closing the UNHEALTHY job then results in additional UNHEALTHY events
                        // that we need to ignore:
                        //     RUNNING, CLOSED, UNHEALTHY
                        //     CLOSED, CLOSED, UNHEALTHY
                        JsonObject job = JobMonitorAppEvent.getJob(value);
                        return (Job.Health.UNHEALTHY.name().equals(
                                JobMonitorAppEvent.getJobHealth(job))
                            && Job.State.RUNNING.name().equals(
                                JobMonitorAppEvent.getProperty(job, "state"))
                            && Job.State.RUNNING.name().equals(
                                JobMonitorAppEvent.getProperty(job, "nextState")));
                    } catch (IllegalArgumentException e) {
                        logger.info("Invalid event filtered out, cause: {}", e.getMessage());
                        return false;
                    }
                 });

        jobEvents.sink(new JobRestarter(t.getRuntimeServiceSupplier()));
    }

    /**
     * A {@code Consumer} which restarts the application specified by a 
     * JSON object passed to its {@code accept} function. 
     */
    private static class JobRestarter implements Consumer<JsonObject> {
        private static final long serialVersionUID = 1L;
        private final Supplier<RuntimeServices> rts;

        JobRestarter(Supplier<RuntimeServices> rts) {
            this.rts = rts;
        }

        @Override
        public void accept(JsonObject value) {
            ControlService controlService = rts.get().getService(ControlService.class);
            JsonObject job = JobMonitorAppEvent.getJob(value);
            String applicationName = JobMonitorAppEvent.getJobName(job);

            logger.trace("close and restart: {}", value);
            
            closeJob(applicationName, controlService);
            submitApplication(applicationName, controlService);
        }
    }

    private void validateSubmitter() {
        ControlService controlService = submitter.getServices().getService(ControlService.class);
        if (controlService == null) {
            throw new IllegalArgumentException("Could not access service " + ControlService.class.getName());
        }

        ApplicationService appService = submitter.getServices().getService(ApplicationService.class);
        if (appService == null) {
            throw new IllegalArgumentException("Could not access service " + ApplicationService.class.getName());
        }

        JobRegistryService jobRegistryService = submitter.getServices().getService(JobRegistryService.class);
        if (jobRegistryService == null) {
            throw new IllegalArgumentException("Could not access service " + JobRegistryService.class.getName());
        }
    }
}
