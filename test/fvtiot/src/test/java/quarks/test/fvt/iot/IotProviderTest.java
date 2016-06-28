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
package quarks.test.fvt.iot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import quarks.apps.iot.IotDevicePubSub;
import quarks.connectors.iot.Commands;
import quarks.connectors.iot.IotDevice;
import quarks.connectors.pubsub.PublishSubscribe;
import quarks.execution.Job;
import quarks.execution.Job.Action;
import quarks.execution.mbeans.JobMXBean;
import quarks.execution.services.ControlService;
import quarks.execution.services.Controls;
import quarks.providers.iot.IotProvider;
import quarks.runtime.jsoncontrol.JsonControlService;
import quarks.test.apps.iot.EchoIotDevice;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.services.ApplicationService;
import quarks.topology.tester.Condition;

/**
 * Test IotProvider using the EchoIotDevice.
 */
public class IotProviderTest {
    
    /**
     * Basic test we can start applications
     * @throws Exception on failure
     */
    @Test
    public void testIotProviderStartApplications() throws Exception {

        IotProvider provider = new IotProvider(EchoIotDevice::new);
        
        assertSame(provider.getApplicationService(),
                provider.getServices().getService(ApplicationService.class));

        provider.start();

        IotTestApps.registerApplications(provider);

        // Create a Submit AppOne request
        JsonObject submitAppOne = IotAppServiceTest.newSubmitRequest("AppOne");
        
        // Create a test application that listens for the
        // output of AppOne (as a published topic).
        Topology checkAppOne = provider.newTopology();
        TStream<String> appOneOut = PublishSubscribe.subscribe(checkAppOne, "appOne", String.class);
        Condition<List<String>> appOnecontents = checkAppOne.getTester().streamContents(appOneOut, "APP1_A", "APP1_B", "APP1_C");
        
        // Run the test in the background as we need to start other apps
        // for it to complete.
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> appOneChecker = service.submit(() -> checkAppOne.getTester().complete(provider, new JsonObject(), appOnecontents, 5, TimeUnit.SECONDS));

        // Create an application that sends a device event
        // with the submit job command, and this will be echoed
        // back as the command that Quarks will detect and pick
        // up to start the application.
        Topology submitter = provider.newTopology();
        TStream<JsonObject> cmds = submitter.of(submitAppOne);
        IotDevice publishedDevice = IotDevicePubSub.addIotDevice(submitter);
        publishedDevice.events(cmds, Commands.CONTROL_SERVICE, 0);
        provider.submit(submitter).get();
        
        // Now AppOne is being submitted so wait for the
        // checker app to receive all the tuples
        // submitted by app one.
        
        appOneChecker.get();
        assertTrue(appOnecontents.getResult().toString(), appOnecontents.valid());
    }
    
    /**
     * Basic test we can stop applications
     * @throws Exception on failure
     */
    @Test
    public void testIotProviderCloseApplicationDirect() throws Exception {

        IotProvider provider = new IotProvider(EchoIotDevice::new);
        
        assertSame(provider.getApplicationService(),
                provider.getServices().getService(ApplicationService.class));

        provider.start();

        IotTestApps.registerApplications(provider);

        // Create a Submit AppOne request
        JsonObject submitAppOne = IotAppServiceTest.newSubmitRequest("AppOne");
        
        // Create an application that sends a device event
        // with the submit job command, and this will be echoed
        // back as the command that Quarks will detect and pick
        // up to start the application.
        Topology submitter = provider.newTopology();
        TStream<JsonObject> cmds = submitter.of(submitAppOne);
        IotDevice publishedDevice = IotDevicePubSub.addIotDevice(submitter);
        publishedDevice.events(cmds, Commands.CONTROL_SERVICE, 0);
        Job appStarter = provider.submit(submitter).get();
        
        ControlService cs = provider.getServices().getService(ControlService.class);
        assertTrue(cs instanceof JsonControlService);
        JsonControlService jsc = (JsonControlService) cs;
        
        JobMXBean jobMbean;
        do {
            Thread.sleep(100);
            jobMbean = cs.getControl(JobMXBean.TYPE, "AppOne", JobMXBean.class);
        } while (jobMbean == null);
        assertEquals("AppOne", jobMbean.getName());
        
        // Now close the job
        JsonObject closeJob = new JsonObject();     
        closeJob.addProperty(JsonControlService.TYPE_KEY, JobMXBean.TYPE);
        closeJob.addProperty(JsonControlService.ALIAS_KEY, "AppOne");      
        JsonArray args = new JsonArray();
        args.add(new JsonPrimitive(Action.CLOSE.name()));
        closeJob.addProperty(JsonControlService.OP_KEY, "stateChange");
        closeJob.add(JsonControlService.ARGS_KEY, args); 
        
        assertEquals(Job.State.RUNNING, appStarter.getCurrentState());
        assertEquals(Job.State.RUNNING, jobMbean.getCurrentState());

        jsc.controlRequest(closeJob);

        // Wait for the job to complete
        for (int i = 0; i < 30; i++) {
            Thread.sleep(100);
            if (jobMbean.getCurrentState() == Job.State.CLOSED)
                break;
        }
        assertEquals(Job.State.CLOSED, jobMbean.getCurrentState());

        // Wait for the associated control to be released
        Thread.sleep(1000 * (Controls.JOB_HOLD_AFTER_CLOSE_SECS + 1));
        jobMbean = cs.getControl(JobMXBean.TYPE, "AppOne", JobMXBean.class);
        assertNull(jobMbean);
        appStarter.stateChange(Action.CLOSE);
    }
}
