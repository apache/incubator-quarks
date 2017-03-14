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

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.execution.utils.ExecutionMgmt;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.runtime.jobregistry.JobEvents;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
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
 * 
 * @see JobEvents#source(Topology, org.apache.edgent.function.BiFunction)
 */
public class JobMonitorApp {
    private static final Logger logger = LoggerFactory.getLogger(JobMonitorApp.class);
  
    /** Job monitoring application name.  {@value} */
    public static final String APP_NAME = SYSTEM_APP_PREFIX + "JobMonitorApp";
    
    /**
     * Create and registers a {@link JobMonitorApp2} with the ApplicationService
     * registered in the given service container.
     * 
     * @param services provides access to service registrations
     */
    public static void createAndRegister(ServiceContainer services) {
      JobMonitorApp jm = new JobMonitorApp();
      ApplicationService appSvc = services.getService(ApplicationService.class);
      appSvc.registerTopology(JobMonitorApp.APP_NAME, (top,cfg) -> jm.buildTopology(top, cfg)); 
    }
    
    private JobMonitorApp() {
    }
    
    /**
     * Populates the topology with:
     * <pre>
     * JobEvents source --&gt; Filter (health == unhealthy) --&gt; Restart application
     * </pre>
     * @param t Topology
     */
    private void buildTopology(Topology t, JsonObject cfg) {
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

						// TODO EDGENT-395 restart with its prior submission config
            ExecutionMgmt.closeJob(applicationName, controlService);
            ExecutionMgmt.submitApplication(applicationName, null, controlService);
        }
    }
}
