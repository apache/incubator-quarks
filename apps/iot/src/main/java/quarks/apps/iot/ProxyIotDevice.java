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
package quarks.apps.iot;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;

import quarks.connectors.iot.IotDevice;
import quarks.connectors.pubsub.PublishSubscribe;
import quarks.function.Function;
import quarks.function.UnaryOperator;
import quarks.topology.TSink;
import quarks.topology.TStream;
import quarks.topology.Topology;

/**
 * Proxy IotDevice that uses publish-subscribe
 * and IotDevicePubSub application to communicate
 * with a single IotDevice connected to a message hub.
 */
class ProxyIotDevice implements IotDevice {
    
	private IotDevicePubSub app;
  
    private final Topology topology;
    
    /**
     * Create a proxy IotDevice
     * @param app IotDevicePubSub application hosting the actual IotDevice.
     * @param topology Topology of the subscribing application.
     */
    ProxyIotDevice(IotDevicePubSub app, Topology topology) {
        this.app = app;
        this.topology = topology;
    }

    @Override
    public final Topology topology() {
        return topology;
    }

    /**
     * Publishes events derived from {@code stream} using the
     * topic {@link IotDevicePubSub#EVENTS} as a JsonObject
     * containing eventId, event, and qos keys.
     */
    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
        
        stream = stream.map(event ->
         {
             JsonObject publishedEvent = new JsonObject();
             
             publishedEvent.addProperty("eventId", eventId.apply(event));
             publishedEvent.add("event", payload.apply(event));
             publishedEvent.addProperty("qos", qos.apply(event));
                         
             return publishedEvent;
         });
        
        return PublishSubscribe.publish(stream, IotDevicePubSub.EVENTS, JsonObject.class);
    }

    /**
     * Publishes events derived from {@code stream} using the
     * topic {@link IotDevicePubSub#EVENTS} as a JsonObject
     * containing eventId, event, and qos keys.
     */
    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {

        stream = stream.map(event ->
        {
            JsonObject publishedEvent = new JsonObject();
            
            publishedEvent.addProperty("eventId", eventId);
            publishedEvent.add("event", event);
            publishedEvent.addProperty("qos", qos);
                        
            return publishedEvent;
        });
        
        return PublishSubscribe.publish(stream, IotDevicePubSub.EVENTS, JsonObject.class);
    }
    
    /**
     * Subscribes to commands.
     * Doesn't yet support subscribing to all commands.
     */
    @Override
    public TStream<JsonObject> commands(String... commandIdentifiers) {
        
        final String firstTopic = app.subscribeToCommand(commandIdentifiers[0]);
        TStream<JsonObject> commandsStream = PublishSubscribe.subscribe(this, firstTopic, JsonObject.class);
        
        if (commandIdentifiers.length > 1) {
            Set<TStream<JsonObject>> additionalStreams = new HashSet<>();
            for (int i = 1; i < commandIdentifiers.length; i++) {
                String topic = app.subscribeToCommand(commandIdentifiers[i]);
                additionalStreams.add(PublishSubscribe.subscribe(this, topic, JsonObject.class));
            }
            commandsStream = commandsStream.union(additionalStreams);
        }
     
        return commandsStream;
    }

}
