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
package quarks.test.apps.runtime;

import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import quarks.apps.runtime.MonitorApp;
import quarks.execution.DirectSubmitter;
import quarks.execution.Job;
import quarks.execution.services.ControlService;
import quarks.execution.services.ServiceContainer;
import quarks.providers.development.DevelopmentProvider;
import quarks.providers.direct.DirectProvider;
import quarks.runtime.appservice.AppService;
import quarks.runtime.jmxcontrol.JMXControlService;
import quarks.runtime.jobregistry.JobRegistry;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.services.ApplicationService;

public class MonitorAppTest {

    public final static String MONITORED_APP_NAME_1 = "MonitoredApp_1";
    public final static String MONITORED_APP_NAME_2 = "MonitoredApp_2";

    @Test
    public void testMonitorApp() throws Exception {
        DirectProvider provider = new DirectProvider();
        startProvider(provider);

        // Start monitor app
        MonitorApp app = new MonitorApp(provider, provider, "Quarks.MonitorApplication");
        Job monitor = app.submit();

        // Declare and register user apps which need monitoring
        registerMonitoredApplicationOne(provider);
        registerMonitoredApplicationTwo(provider);

        // Start monitored apps
        startMonitoredApplications(provider);
        
        // Run for a while, assert the monitor app is still running healthy
        Thread.sleep(5000);
        assertTrue(
                Job.State.RUNNING.equals(monitor.getCurrentState()) &&
                Job.State.RUNNING.equals(monitor.getNextState()) &&
                Job.Health.HEALTHY.equals(monitor.getHealth()));
    }

    static void startProvider(DirectProvider provider) 
            throws InterruptedException, ExecutionException {
        
        provider.getServices().addService(ControlService.class,
                new JMXControlService(DevelopmentProvider.JMX_DOMAIN, new Hashtable<>()));
        AppService.createAndRegister(provider, provider);
        JobRegistry.createAndRegister(provider.getServices());        
    }

    /**
     * Fails every 2 seconds (20 tuples * 100 millis)
     */
    static void registerMonitoredApplicationOne(DirectSubmitter<Topology, Job> submitter) {
        ApplicationService appService = submitter.getServices().getService(ApplicationService.class);
        appService.registerTopology(MONITORED_APP_NAME_1, (topology, config) -> {
                
                Random r = new Random();
                TStream<Double> d  = topology.poll(() -> r.nextGaussian(), 100, TimeUnit.MILLISECONDS);
                
                final AtomicInteger count = new AtomicInteger(0);
                d = d.filter(tuple -> {
                    int tupleCount = count.incrementAndGet();
                    if (tupleCount == 20) {
                        throw new IllegalStateException("Injected error");
                    }
                    return true; 
                });
                
                d.sink(tuple -> System.out.print("."));
            });
    }

    /**
     * Fails every 1.5 seconds (10 tuples * 150 millis)
     */
    static void registerMonitoredApplicationTwo(DirectSubmitter<Topology, Job> submitter) {
        ApplicationService appService = submitter.getServices().getService(ApplicationService.class);
        appService.registerTopology(MONITORED_APP_NAME_2, (topology, config) -> {
                
                Random r = new Random();
                TStream<Double> d  = topology.poll(() -> r.nextGaussian(), 150, TimeUnit.MILLISECONDS);
                
                final AtomicInteger count = new AtomicInteger(0);
                d = d.filter(tuple -> {
                    int tupleCount = count.incrementAndGet();
                    if (tupleCount == 10) {
                        throw new IllegalStateException("Injected error");
                    }
                    return true; 
                });
                
                d.sink(tuple -> System.out.print("#"));
            });
    }

    static void startMonitoredApplications(DirectSubmitter<Topology, Job> submitter) {
        ServiceContainer services = submitter.getServices();
        ApplicationService appService = services.getService(ApplicationService.class);
        ControlService controlService = services.getService(ControlService.class);

        // Submit all applications registered with the ApplicationService
        for (String name: appService.getApplicationNames()) {
            MonitorApp.submitApplication(name, controlService);
        }
    }
}
