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

package quarks.test.apps.iot;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.gson.JsonObject;

import quarks.apps.iot.IotDevicePubSub;
import quarks.connectors.iot.IotDevice;
import quarks.connectors.iot.QoS;
import quarks.connectors.pubsub.service.ProviderPubSub;
import quarks.connectors.pubsub.service.PublishSubscribeService;
import quarks.execution.Job;
import quarks.execution.Job.Action;
import quarks.providers.direct.DirectProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.plumbing.PlumbingStreams;
import quarks.topology.tester.Condition;

public class IotDevicePubSubTest {

    

    @Test
    public void testIotDevicePubSubApp() throws Exception {
        DirectProvider dp = new DirectProvider();

        dp.getServices().addService(PublishSubscribeService.class, new ProviderPubSub());
        
        Topology iot = dp.newTopology("IotPubSub");
        IotDevicePubSub iotApp = new IotDevicePubSub(new EchoIotDevice(iot));
        
        Topology app1 = dp.newTopology("App1");
        
        IotDevice app1Iot = iotApp.addIotDevice(app1);
        
        TStream<String> data = app1.strings("A", "B", "C");
        
        // Without this the tuple can be published and discarded before the
        // subscriber is hooked up.
        data = PlumbingStreams.blockingOneShotDelay(data, 500, TimeUnit.MILLISECONDS);
        
        TStream<JsonObject> events = data.map(
                s -> {JsonObject j = new JsonObject(); j.addProperty("v", s); return j;});       
        app1Iot.events(events, "ps1", QoS.FIRE_AND_FORGET);
        
        TStream<JsonObject> echoedCmds = app1Iot.commands("ec_ps1");
        
        TStream<String> ecs = echoedCmds.map(j -> j.getAsJsonObject(IotDevice.CMD_PAYLOAD).getAsJsonPrimitive("v").getAsString());
        Condition<List<String>> tcEcho = app1.getTester().streamContents(ecs, "A", "B", "C"); // Expect all tuples
        
        Job jIot = dp.submit(iot.topology()).get();
        Job jApp = dp.submit(app1.topology()).get();

        for (int i = 0; i < 50 && !tcEcho.valid(); i++) {
            Thread.sleep(50);
        }

        assertTrue(tcEcho.getResult().toString(), tcEcho.valid());        

        jIot.stateChange(Action.CLOSE);
        jApp.stateChange(Action.CLOSE);
    }
}
