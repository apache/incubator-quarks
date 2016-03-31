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
package quarks.apps.runtime;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import quarks.execution.DirectSubmitter;
import quarks.execution.Job;
import quarks.execution.services.ControlService;
import quarks.execution.services.RuntimeServices;
import quarks.execution.services.job.JobRegistryService;
import quarks.function.Consumer;
import quarks.function.Supplier;
import quarks.runtime.jobregistry.JobEvents;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.TopologyProvider;
import quarks.topology.mbeans.ApplicationServiceMXBean;
import quarks.topology.plumbing.PlumbingStreams;
import quarks.topology.services.ApplicationService;

/**
 * Job monitoring application.
 * <p>
 * The application listens on JobRegistry events and resubmits jobs for which 
 * an event has been emitted because the job is unhealthy. The monitored 
 * applications must be registered with an {@code ApplicationService} 
 * prior to submission, otherwise the monitor application cannot restart 
 * them.</p>
 * <p> 
 * The monitoring application must be submitted within a context which 
 * provides the following services:
 * <ul>
 * <li>ApplicationService - an {@code ApplicationServiceMXBean} control 
 * registered by this service is used to resubmit failed applications.</li>
 * <li>ControlService - the application queries this service for an 
 * {@code ApplicationServiceMXBean} control, which is then used for 
 * restarting failed applications.</li>
 * <li>JobRegistryService - generates job monitoring events. </li>
 * </ul>
 * </p>
 */
public class MonitorApp {
    private final TopologyProvider provider;
    private final DirectSubmitter<Topology, Job> submitter;
    private final Topology topology;
    private static final Logger logger = LoggerFactory.getLogger(MonitorApp.class);

    /**
     * Constructs a {@code MonitorApp} with the specified name in the 
     * context of the specified provider.
     * 
     * @param provider the topology provider
     * @param submitter a {@code DirectSubmitter} which provides required 
     *      services and submits the application
     * @param name the application name
     * 
     * @throws IllegalArgumentException if the submitter does not provide 
     *      access to the required services
     */
    public MonitorApp(TopologyProvider provider, 
            DirectSubmitter<Topology, Job> submitter, String name) {

        this.provider = provider;
        this.submitter = submitter;
        validateSubmitter();
        this.topology = declareTopology(name);
    }
    
    /**
     * Submits the application topology.
     * 
     * @return the job.
     * @throws InterruptedException
     * @throws ExecutionException
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
            Set<ApplicationServiceMXBean> controls = 
                    controlService.getControls(ApplicationServiceMXBean.class);
            if (controls.isEmpty()) {
                throw new IllegalStateException(
                        "Could not find a registered control with the following interface: " + 
                        ApplicationServiceMXBean.class.getName());                
            }
            for (ApplicationServiceMXBean control : controls)
// TODO add ability to submit with the initial application configuration
                control.submit(applicationName, null);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Declares the following topology:
     * <pre>
     * JobEvents source --> Filter (health == unhealthy) --> Restart application
     * </pre>
     * 
     * @param name the topology name
     * @return the application topology
     */
    protected Topology declareTopology(String name) {
        Topology t = provider.newTopology(name);
        TStream<JsonObject> jobEvents = JobEvents.source(
                t, 
                (evType, job) -> { return MonitorAppEvent.toJsonObject(evType, job); }
                );
        jobEvents = PlumbingStreams.isolate(jobEvents, true);

        jobEvents = jobEvents.filter(
                value -> {
                    logger.trace("Filter: {}", value);

                    try {
                        JsonObject job = MonitorAppEvent.getJob(value);
                        return (Job.Health.UNHEALTHY.name().equals(
                                MonitorAppEvent.getJobHealth(job)));
                    } catch (IllegalArgumentException e) {
                        logger.info("Invalid event filtered out, cause: {}", e.getMessage());
                        return false;
                    }
                 });

        jobEvents.sink(new JobRestarter(t.getRuntimeServiceSupplier()));
        return t;
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
            JsonObject job = MonitorAppEvent.getJob(value);
            String applicationName = MonitorAppEvent.getJobName(job);

            logger.info("Will restart monitored application {}, cause: {}", applicationName, value);
            submitApplication(MonitorAppEvent.getJobName(job), controlService);
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
