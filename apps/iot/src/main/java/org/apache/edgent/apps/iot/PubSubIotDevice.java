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
package org.apache.edgent.apps.iot;

import static org.apache.edgent.apps.iot.IotDevicePubSub.COMMANDS_TOPIC;
import static org.apache.edgent.apps.iot.IotDevicePubSub.EVENTS_TOPIC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.pubsub.PublishSubscribe;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;


/**
 * Pub-sub IotDevice that uses publish-subscribe and IotDevicePubSub application
 * to communicate with a single IotDevice connected to a message hub.
 */
class PubSubIotDevice implements IotDevice {

    private final Topology topology;

    /**
     * Create a proxy IotDevice
     * 
     * @param app
     *            IotDevicePubSub application hosting the actual IotDevice.
     * @param topology
     *            Topology of the subscribing application.
     */
    PubSubIotDevice(Topology topology) {
        this.topology = topology;
    }

    @Override
    public final Topology topology() {
        return topology;
    }

    /**
     * Publishes events derived from {@code stream} using the topic
     * {@link EVENTS} as a JsonObject containing eventId, event,
     * and qos keys.
     */
    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {

        stream = stream.map(event -> {
            JsonObject publishedEvent = new JsonObject();

            publishedEvent.addProperty("eventId", eventId.apply(event));
            publishedEvent.add("event", payload.apply(event));
            publishedEvent.addProperty("qos", qos.apply(event));

            return publishedEvent;
        });

        return PublishSubscribe.publish(stream, IotDevicePubSub.EVENTS_TOPIC, JsonObject.class);
    }

    /**
     * Publishes events derived from {@code stream} using the topic
     * {@link IotDevicePubSub#EVENTS} as a JsonObject containing eventId, event,
     * and qos keys.
     */
    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {

        stream = stream.map(event -> {
            JsonObject publishedEvent = new JsonObject();

            publishedEvent.addProperty("eventId", eventId);
            publishedEvent.add("event", event);
            publishedEvent.addProperty("qos", qos);

            return publishedEvent;
        });

        return PublishSubscribe.publish(stream, EVENTS_TOPIC, JsonObject.class);
    }

    /**
     * Subscribes to commands.
     */
    @Override
    public TStream<JsonObject> commands(String... commandIdentifiers) {

        TStream<JsonObject> commandsStream = PublishSubscribe.subscribe(this, COMMANDS_TOPIC, JsonObject.class);
        
        if (commandIdentifiers.length > 0) {
            Set<String> cmdIds = new HashSet<>(Arrays.asList(commandIdentifiers));
            commandsStream = commandsStream.filter(
                    cmd -> cmdIds.contains(cmd.get(CMD_ID).getAsString()));
        }

        return commandsStream;
    }

    @Override
    public String getDeviceType() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getDeviceId() {
      // TODO Auto-generated method stub
      return null;
    }

}
