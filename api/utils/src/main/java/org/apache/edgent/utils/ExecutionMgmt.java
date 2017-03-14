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
package org.apache.edgent.utils;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.Action;
import org.apache.edgent.execution.mbeans.JobMXBean;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.Controls;
import org.apache.edgent.execution.services.JobRegistryService;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.topology.mbeans.ApplicationServiceMXBean;
import org.apache.edgent.topology.services.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Utilities for managing topology execution.
 * 
 * <p>These utility methods tend to depend on a combination of related but semi-independent
 * items such as {@link ApplicationService} and {@link JobRegistryService} services
 * and {@link ApplicationServiceMXBean} and {@link JobMXBean} control beans.
 */
public class ExecutionMgmt {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionMgmt.class);

  /**
   * Submits an application previously registered with the {@link ApplicationService}.
   * 
   * <p>Uses a {@link ApplicationServiceMXBean} control 
   * registered with the {@code ControlService} registered in the
   * specified {@code services}.
   * 
   * <p>This method requires that a {@link JobRegistryService} be present in
   * the {@code services}.
   * 
   * @param applicationName the name of the application to submit
   * @param config config to pass to 
   *        {@link ApplicationServiceMXBean#submit(String, String) submit()}
   *        May be null.
   * @param services service container
   * @return the {@link Job}
   * 
   * @see #submitApplication(String, JsonObject, ControlService)
   */
  public static Job submitApplication(String applicationName, JsonObject config, ServiceContainer services) {
      String jobId = submitApplication(applicationName, config,
                                      services.getService(ControlService.class));
      return services.getService(JobRegistryService.class).getJob(jobId);
  }

  /**
   * Submits an application previously registered with the {@link ApplicationService}.
   * 
   * <p>Uses a {@link ApplicationServiceMXBean} control 
   * registered with the specified {@code ControlService}.
   * 
   * @param applicationName the name of the application to submit
   * @param config config to pass to 
   *        {@link ApplicationServiceMXBean#submit(String, String) submit()}
   *        May be null.
   * @param controlService the control service
   * @return the Job's id - from {@link Job#getId()}
   */
  public static String submitApplication(String applicationName, JsonObject config, ControlService controlService) {
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
          logger.info("Submitting application {}", applicationName);
          return control.submit(applicationName, config == null ? null : config.toString());
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
                  waitForMillis < 0;
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
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
  }

}
