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
package org.apache.edgent.test.fvt.iot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.edgent.apps.iot.IotDevicePubSub;
import org.apache.edgent.connectors.iot.Commands;
import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.pubsub.PublishSubscribe;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.Action;
import org.apache.edgent.execution.mbeans.JobMXBean;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.Controls;
import org.apache.edgent.providers.iot.IotProvider;
import org.apache.edgent.runtime.jsoncontrol.JsonControlService;
import org.apache.edgent.test.apps.iot.EchoIotDevice;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.services.ApplicationService;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
        // back as the command that Edgent will detect and pick
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
        // back as the command that Edgent will detect and pick
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
    
    @Test
    public void testNoPreferences() {
        IotProvider provider1 = new IotProvider(EchoIotDevice::new);
        assertNull(provider1.getServices().getService(Preferences.class));
        
        IotProvider provider2 = new IotProvider(null, EchoIotDevice::new);
        assertNull(provider2.getServices().getService(Preferences.class));
    }
    
    @Test
    public void testPreferences() throws BackingStoreException {
      
        String PP1 = this.getClass().getName() + "-PP1";
        String PP2 = this.getClass().getName() + "-PP2";
        
        Preferences.userNodeForPackage(IotProvider.class).node(PP1).removeNode();     
        Preferences.userNodeForPackage(IotProvider.class).node(PP2).removeNode();     
        
        {
            Preferences pp1S = IotProvider.getPreferences(PP1);
            assertNotNull(pp1S);
            
            IotProvider provider1 = new IotProvider(PP1, EchoIotDevice::new);
            IotProvider provider2 = new IotProvider(PP2, EchoIotDevice::new);

            Preferences pp1 = provider1.getServices().getService(Preferences.class);
            assertNotNull(pp1);
            assertSame(pp1, pp1S);

            Preferences pp2 = provider2.getServices().getService(Preferences.class);
            assertNotNull(pp2);

            assertNotSame(pp1, pp2);

            pp1.put("a", "one");
            assertEquals("one", pp1.get("a", "unset"));

            // Ensure setting of one does not affect the other
            assertEquals("unset", pp2.get("a", "unset"));

            pp2.put("a", "two");
            assertEquals("two", pp2.get("a", "unset"));
            assertEquals("one", pp1.get("a", "unset"));

            pp1.flush();
            pp2.flush();
        }
        
        // Create a new provider with the same name
        // it should pick up the previously values.
        {
            IotProvider provider1N = new IotProvider(PP1, EchoIotDevice::new);
            Preferences pp1N = provider1N.getServices().getService(Preferences.class);
            assertNotNull(pp1N);
            assertEquals("one", pp1N.get("a", "unset"));
        }
        {
            IotProvider provider2N = new IotProvider(PP2, EchoIotDevice::new);
            Preferences pp2N = provider2N.getServices().getService(Preferences.class);
            assertNotNull(pp2N);
            assertEquals("two", pp2N.get("a", "unset"));
        }
        
        // Remove the nodes
        Preferences.userNodeForPackage(IotProvider.class).node(PP1).removeNode();     
        Preferences.userNodeForPackage(IotProvider.class).node(PP2).removeNode();     
    }
}
