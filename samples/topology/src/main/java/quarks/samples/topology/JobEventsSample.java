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
package quarks.samples.topology;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import quarks.execution.Job;
import quarks.execution.services.JobRegistryService;
import quarks.providers.direct.DirectProvider;
import quarks.runtime.jobregistry.JobEvents;
import quarks.runtime.jobregistry.JobRegistry;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.plumbing.PlumbingStreams;

/**
 * Demonstrates job monitoring using the {@link JobRegistryService} service.
 * <p>
 * The example starts a system monitoring application, then concurrently 
 * submits two jobs.
 * The job monitoring application generates job event tuples when jobs 
 * are added or removed from registry, or when a job gets updated. 
 * Tuples are pushed to a sink, which prints them onto the system output.</p>
 * <p>
 * Note that the original job events stream processing is executed by the
 * JobRegistryService event {@linkplain 
 * JobRegistryService#addListener(quarks.function.BiConsumer) listeners}
 * invoker thread. 
 * It is considered good practice to isolate the event source from the rest 
 * of the graph, in order for the processing of tuples to be executed by a
 * different thread.</p>
 */
public class JobEventsSample {
    private final DirectProvider dp;

    public static void main(String[] args) throws Exception {
        
        JobEventsSample sample = new JobEventsSample();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        // Monitoring app
        sample.startJobMonitorApp();

        // Asynchronously start two applications
        executor.schedule(sample.runMonitoredApp("MonitoredApp1"), 300, TimeUnit.MILLISECONDS);
        executor.schedule(sample.runMonitoredApp("MonitoredApp2"), 3000, TimeUnit.MILLISECONDS);
    }

    JobEventsSample() throws Exception {
        this.dp = new DirectProvider();
        JobRegistry.createAndRegister(dp.getServices());
    }

    /**
     * Declares and submits a monitored application.
     * <p>
     * Note that inline sleeps are introduced to simulate the timing 
     * of a real-life application lifecycle.
     * 
     * @param name application name
     */
    void monitored(String name) throws InterruptedException, ExecutionException {
        // Declare topology
        Topology t = dp.newTopology(name);
        
        Random r = new Random();
        TStream<Double> d  = t.poll(() -> r.nextGaussian(), 100, TimeUnit.MILLISECONDS);
        d.sink(tuple -> System.out.print("."));

        // Submit job after 2 seconds
        Thread.sleep(2000);
        Future<Job> f = dp.submit(t);
        Job job = f.get();
        
        // Run for 5 seconds, then close job
        Thread.sleep(5000);
        job.stateChange(Job.Action.CLOSE);

        // Unregister job after 2 seconds
        Thread.sleep(2000);
        provider().getServices().getService(JobRegistryService.class).removeJob(job.getId());
    }

    /**
     * Monitoring application generates tuples on job registrations, removals, 
     * and on registered job updates.
     */
    Job startJobMonitorApp() throws InterruptedException, ExecutionException {
        Topology topology = dp.newTopology("JobMonitorApp");

        TStream<JsonObject> jobEvents = JobEvents.source(
                topology, 
                (evType, job) -> { return JobEventsSample.wrap(evType, job); });
        
        TStream<JsonObject> isolated = PlumbingStreams.isolate(jobEvents, true);
        isolated.sink(tuple -> {
                System.err.println(tuple.toString());
            });

        Future<Job> f = dp.submit(topology);
        return f.get();
    }
    
    /**
     * Creates a JsonObject wrapping a JobRegistryService event type and 
     * Job info.
     * 
     * @param evType the event type
     * @param job the job
     * @return the wrapped data
     */
    static JsonObject wrap(JobRegistryService.EventType evType, Job job) {
        JsonObject value = new JsonObject();
        value.addProperty("time", (Number)System.currentTimeMillis());
        value.addProperty("event", evType.toString());
        JsonObject obj = new JsonObject();
        obj.addProperty("id", job.getId());
        obj.addProperty("name", job.getName());
        obj.addProperty("state", job.getCurrentState().toString());
        obj.addProperty("nextState", job.getNextState().toString());
        obj.addProperty("health", job.getHealth().toString());
        obj.addProperty("lastError", job.getLastError());
        value.add("job", obj);
        return value;
    }

    private DirectProvider provider() {
        return dp;
    }
    
    private Runnable runMonitoredApp(String name) {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    monitored(name);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };
    }
}
