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

import static org.apache.edgent.topology.services.ApplicationService.SYSTEM_APP_PREFIX;

import org.apache.edgent.connectors.iot.Events;
import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.pubsub.PublishSubscribe;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TopologyElement;

import com.google.gson.JsonObject;

/**
 * Application sharing an {@code IotDevice} through publish-subscribe. <BR>
 * This application shares an {@link IotDevice} across multiple running
 * jobs. This allows a single connection to a back-end message hub to be shared
 * across multiple independent applications, without having to build a single
 * org.apache.edgent.org.apache.edgent.topology.
 * <P>
 * Applications coded to {@link IotDevice} obtain a org.apache.edgent.org.apache.edgent.topology specific
 * {@code IotDevice} using {@link #addIotDevice(TopologyElement)}. This returned
 * device will route events and commands to/from the actual message hub
 * {@code IotDevice} through publish-subscribe.
 * <P>
 * An instance of this application is created by first creating a new org.apache.edgent.org.apache.edgent.topology and
 * then creating a {@link IotDevice} specific to the desired message hub. Then
 * the application is created by calling {@link #createApplication(IotDevice)}
 * passing the {@code IotDevice}. <BR>
 * Then additional independent applications (topologies) can be created and they
 * create a proxy {@code IotDevice} for their org.apache.edgent.org.apache.edgent.topology using
 * {@link #addIotDevice(TopologyElement)}. This proxy {@code IotDevice} is then
 * used to send device events and receive device commands in that org.apache.edgent.org.apache.edgent.topology. <BR>
 * Once all the topologies have been declared they can be submitted.
 * </P>
 * <P>
 * At startup this application sends a single device event with
 * identifier {@link Events#IOT_START}. This performs two functions:
 * </P>
 * <UL>
 * <LI>Initiates a connection to the message hub.</LI>
 * <LI>Allows external applications to be notified (by subscribing to device events)
 * when an Edgent provider starts.</LI>
 * </UL>
 * 
 * @see PublishSubscribe
 */
public class IotDevicePubSub {
    
    /**
     * IotDevicePubSub application name.
     */
    public static final String APP_NAME = SYSTEM_APP_PREFIX + "IotDevicePubSub";
    
    /**
     * Events published to topic {@value} are sent as device events using the
     * actual message hub {@link IotDevice}. <BR>
     * it is recommended applications use the {@code IotDevice} returned by
     * {@link #addIotDevice(TopologyElement)} to send events rather than
     * publishing streams to this topic.
     */
    public static final String EVENTS_TOPIC = PublishSubscribe.RESERVED_TOPIC_PREFIX + "iot/events";
    
    /**
     * Device commands are published to {@value} by
     * this application. <BR>
     * it is recommended applications use the {@code IotDevice} returned by
     * {@link #addIotDevice(TopologyElement)} to receive commands rather than
     * subscribing to streams with this topic prefix.
     */
    public static final String COMMANDS_TOPIC = PublishSubscribe.RESERVED_TOPIC_PREFIX + "iot/commands";

    /**
     * Create an instance of this application using {@code device} as the device
     * connection to a message hub.
     * 
     * @param device the IotDevice
     */
    public static void createApplication(IotDevice device) {

        TStream<JsonObject> events = PublishSubscribe.subscribe(device, EVENTS_TOPIC, JsonObject.class);

        device.events(events, ew -> ew.getAsJsonPrimitive("eventId").getAsString(), ew -> ew.getAsJsonObject("event"),
                ew -> ew.getAsJsonPrimitive("qos").getAsInt());
        
        PublishSubscribe.publish(device.commands(), COMMANDS_TOPIC, JsonObject.class);
        
        // Publish a single event at startup
        TStream<JsonObject> start = device.topology().of(new JsonObject());
        device.events(start, Events.IOT_START, QoS.AT_MOST_ONCE);
    }

    /**
     * Add a proxy {@code IotDevice} for the org.apache.edgent.org.apache.edgent.topology containing {@code te}.
     * <P>
     * Any events sent through the returned device are sent onto the message hub
     * device through publish-subscribe. <BR>
     * Subscribing to commands using the returned device will subscribe to
     * commands received by the message hub device.
     * </P>
     * 
     * @param te
     *            Topology the returned device is contained in.
     * @return Proxy device.
     */
    public static IotDevice addIotDevice(TopologyElement te) {
        return new PubSubIotDevice(te.topology());
    }
}
