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
package quarks.runtime.etiao.mbeans;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import quarks.execution.Job;
import quarks.execution.Job.Action;
import quarks.execution.mbeans.JobMXBean;
import quarks.execution.services.ControlService;
import quarks.execution.services.Controls;
import quarks.runtime.etiao.EtiaoJob;
import quarks.runtime.etiao.graph.model.GraphType;

/**
 * Implementation of a control interface for the {@code EtiaoJob}.
 */
public class EtiaoJobBean implements JobMXBean {
    private final EtiaoJob job;
    private ControlService controlService;
    private String controlId;
    private static final Logger logger = LoggerFactory.getLogger(EtiaoJobBean.class);
    
    /**
     * Factory method which creates an {@code EtiaoJobBean} instance to
     * control the specified {@code EtiaoJob} and registers it with the 
     * specified {@code ControlService}.
     * 
     * @param cs the control service
     * @param job the controlled job
     * @return a registered bean instance
     */
    public static EtiaoJobBean registerControl(ControlService cs, EtiaoJob job) {
        EtiaoJobBean bean = new EtiaoJobBean(job);
        bean.registerControl(cs);
        return bean;        
    }

    private EtiaoJobBean(EtiaoJob job) {
        this.job = job;
    }

    public String getControlId() {
        return controlId;
    }

    public boolean wasRegistered() {
        return controlId != null;
    }

    @Override
    public String getId() {
        return job.getId();
    }

    @Override
    public String getName() {
        return job.getName();
    }

    @Override
    public Job.State getCurrentState() {
        return job.getCurrentState();
    }

    @Override
    public Job.State getNextState() {
        return job.getNextState();
    }

    @Override
    public String graphSnapshot() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(new GraphType(job.graph()));
    }

    @Override
    public Job.Health getHealth() {
        return job.getHealth();
    }

    @Override
    public String getLastError() {
        return job.getLastError();
    }

    @Override
    public void stateChange(Action action) {
        job.stateChange(action);
        if (wasRegistered() && action == Action.CLOSE) {
            unregisterControlAsync();
        }
    }

    private void registerControl(ControlService cs) {
        if (cs == null)
            throw new IllegalArgumentException("ControlService must not be null");

        logger.trace("Registering control for job id {}, job name {}", job.getId(), job.getName());

        this.controlService = cs;
        JobMXBean oldControl = cs.getControl(JobMXBean.TYPE, job.getName(), JobMXBean.class);

        if (oldControl != null) {
            String oldControlId = cs.getControlId(JobMXBean.TYPE, job.getName(), JobMXBean.class);
            if (oldControlId != null) {
                if (isJobClosed(oldControl)) {
                    cs.unregister(oldControlId);
                    logger.debug("Old control id {} for CLOSED job name {} was unregistered", 
                            oldControlId, job.getName());
                }
                else {
                    throw new IllegalStateException(
                        "Cannot register job control for alias " + 
                        job.getName() + " because a job control with id " + oldControlId + 
                        " for the same alias already exists and is not CLOSED");
                }
            }
        }
        this.controlId = cs.registerControl(JobMXBean.TYPE, job.getId(), 
                job.getName(), JobMXBean.class, this);
        logger.debug("Control for job id {}, job name {} was registered with id {}", 
                job.getId(), job.getName(), controlId);
    }

    private void unregisterControlAsync() {
        if (controlService == null)
            throw new IllegalStateException(
                    "The ControlService of a registered bean must not be null");

        getThread(new Runnable() {
            @Override
            public void run() {
                unregisterControl();
            }
        }).start();
    }

    private void unregisterControl() {
        if (!wasRegistered())
            return;

        long startTime = System.currentTimeMillis();
        try {
            try {
                job.completeClosing(Controls.JOB_HOLD_AFTER_CLOSE_SECS, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                String cause = e.getCause() != null ? e.getCause().getMessage() : "unknown";
                logger.info("Error {} during completion of job {} caused by {}", 
                        e.getMessage(), job.getName(), cause);
                logger.debug("Error during completion of job " + job.getName(), e);
            } catch (TimeoutException e) {
                logger.info("Timed out after {} milliseconds waiting for job {} to complete", 
                        (System.currentTimeMillis() - startTime), job.getName());                        
            }
            long remaining = startTime + Controls.JOB_HOLD_AFTER_CLOSE_SECS * 1000 - System.currentTimeMillis();
            if (remaining < 0)
                remaining = 0;
            else
                logger.trace("Job completed, waiting {} milliseconds before unregistering control {}", 
                        remaining, controlId);

            Thread.sleep(remaining);
        } catch (InterruptedException e) {
            // Swallow the exception and unregister the control 
        }
        finally {
            controlService.unregister(controlId);
            logger.trace("Control {} unregistered", controlId);
        }
    }

    private Thread getThread(Runnable r) {
        ThreadFactory threads = Executors.defaultThreadFactory();
        return threads.newThread(r);
    }
    
    private boolean isJobClosed(JobMXBean job) {
        return job.getCurrentState() == Job.State.CLOSED &&
                job.getNextState() == Job.State.CLOSED;
    }
}
