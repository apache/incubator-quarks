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
import quarks.topology.TStream;
import quarks.topology.TopologyElement;

/**
 * Application sharing an {@code IotDevice}
 * through publish-subscribe.
 * <BR>
 * This application allows sharing an {@link IotDevice}
 * across multiple running jobs. This allows a single
 * connection to a back-end message hub to be shared across
 * multiple independent applications, without having to
 * build a single topology.
 * <P>
 * Applications coded to {@link IotDevice}
 * obtain a topology specific {@code IotDevice} using
 * {@link #addIotDevice(TopologyElement)}. This returned
 * device will route events and commands to/from the
 * actual message hub {@code IotDevice} through
 * publish-subscribe.
 * <P>
 * An instance of this application is created by creating
 * a new topology and then creating a {@link IotDevice}
 * specific to the desired message hub. Then the application
 * is created by calling {@link #IotDevicePubSub(IotDevice)}
 * passing the {@code IotDevice}.
 * <BR>
 * Then additional independent applications (topologies) can be created
 * and they create a proxy {@code IotDevice} for their topology using
 * {@link #addIotDevice(TopologyElement)}. This proxy  {@code IotDevice}
 * is then used to send device events and receive device commands in
 * that topology.
 * <BR>
 * Once all the topologies have been declared they can be submitted.
 * </P>
 * <P>
 * Limitations:
 * <UL>
 * <LI>
 * Subscribing to all device commands (passing no arguments to {@link IotDevice#commands(String...)} is
 * not supported by the proxy {@code IotDevice}. 
 * </LI>
 * <LI>
 * All applications that subscribe to device commands must be declared before
 * the instance of this application is submitted.
 * </LI>
 * </UL>
 * </P>
 */
public class IotDevicePubSub {
	
	/**
	 * Events published to topic {@value} are sent as device
	 * events using the actual message hub {@link IotDevice}.
	 * <BR>
	 * it is recommended applications use the {@code IotDevice}
	 * returned by {@link #addIotDevice(TopologyElement)} to
	 * send events rather than publishing streams to this
	 * topic.
	 */
    public static final String EVENTS = "quarks/iot/events";
    
	/**
	 * Device commands are published to {@code quarks/iot/command/commandId}
	 * by this application.
	 * <BR>
	 * it is recommended applications use the {@code IotDevice}
	 * returned by {@link #addIotDevice(TopologyElement)} to
	 * send events rather than subscribing to streams with this
	 * topic prefix.
	 */
    public static final String COMMANDS_PREFIX = "quarks/iot/command/";

	
    private final IotDevice device;
    private final Set<String> publishedCommandTopics = new HashSet<>();
    
    /**
     * Create an instance of this application using {@code device}
     * as the device connection to a message hub.
     * @param device Device to a message hub.
     */
    public IotDevicePubSub(IotDevice device) {
        this.device = device;
        createApplication();
    }

    /**
     * Add a proxy {@code IotDevice} for the topology
     * containing {@code te}.
     * <P>
     * Any events sent through the returned device
     * are sent onto the message hub device through
     * publish-subscribe.
     * <BR>
     * Subscribing to commands using the returned
     * device will subscribe to commands received
     * by the message hub device. 
     * </P>
     * 
     * @param te Topology the returned device is contained in.
     * @return Proxy device.
     */
    public IotDevice addIotDevice(TopologyElement te) {       
        return new ProxyIotDevice(this, te.topology());
    }
    
    /**
     * Create initial application subscribing to
     * events and delivering them to the message
     * hub IotDevice.
     * 
     */
    private void createApplication() {
        
        TStream<JsonObject> events = PublishSubscribe.subscribe(device,
                EVENTS, JsonObject.class);
        
        device.events(events,
                ew -> ew.getAsJsonPrimitive("eventId").getAsString(),
                ew -> ew.getAsJsonObject("event"),
                ew -> ew.getAsJsonPrimitive("qos").getAsInt());
    }
    
    /**
     * Subscribe to device commands.
     * Keep track of which command identifiers are subscribed to
     * so the stream for a command identifier is only created once.
     * 
     * @param commandIdentifier
     * @return Topic that needs to be subscribed to in the
     * subscriber application.
     */
    synchronized String subscribeToCommand(String commandIdentifier) {
        
        String topic = COMMANDS_PREFIX + commandIdentifier;
        
        if (!publishedCommandTopics.contains(topic)) {
            TStream<JsonObject> systemStream = device.commands(commandIdentifier);
            PublishSubscribe.publish(systemStream, topic, JsonObject.class);
        }
        
        return topic;
    }
}
