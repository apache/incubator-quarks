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

package org.apache.edgent.connectors.iotp;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.runtime.IotpConnector;
import org.apache.edgent.connectors.iotp.runtime.IotpDeviceCommands;
import org.apache.edgent.connectors.iotp.runtime.IotpDeviceEventsFixed;
import org.apache.edgent.connectors.iotp.runtime.IotpDeviceEventsFunction;
import org.apache.edgent.connectors.iotp.runtime.IotpDeviceHttpEventsFixed;
import org.apache.edgent.connectors.iotp.runtime.IotpDeviceHttpEventsFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.device.Command;
import com.ibm.iotf.client.device.DeviceClient;

/**
 * A Device Connector to IBM Watson IoT Platform (WIoTP).
 * <BR>
 * IBM Watson IoT Platform is a cloud based internet of things
 * scale message hub that provides a device model on top of MQTT.
 * {@code IotpDevice} implements the generic device model {@link IotDevice}
 * and thus can be used as a connector for
 * {@link org.apache.edgent.providers.iot.IotProvider}.
 * <BR>
 * <em>Note IBM Watson IoT Platform was previously known as
 * IBM Internet of Things Foundation.</em>
 * <p>
 * This connector is a thin wrapper over the WIoTP {@code DeviceClient} Java API.
 * The constructor {@code Properties} and {@code optionsFile} contents are those
 * demanded by {@code DeviceClient}.
 * <p>
 * For more complex use cases this connector can be constructed to use
 * a supplied {@code DeviceClient}.  This gives an application complete
 * control over the construction and configuration of the underlying
 * connection to WIoTP.
 * <p>
 * An application that wants to be a WIoTP <i>managed device</i>
 * and an Edgent application must construct a WIoTP {@code ManagedDevice} instance
 * and construct this device connector using it.
 * <P>
 * If the following link's page doesn't render in javadoc, paste it into a browser window.
 * <BR>
 * See the IBM Watson IoT Platform documentation for Developing devices at
 * <a href="https://internetofthings.ibmcloud.com/">https://internetofthings.ibmcloud.com/</a>
 * <p>
 * See <a href="https://github.com/ibm-watson-iot/iot-java/releases">WIoTP Java Client Library Releases</a>
 * for general information.
 * <p>
 * See <a href="https://github.com/ibm-watson-iot/iot-java/tree/master#migration-from-release-015-to-021">WIoTP Migration from release 0.1.5 to 0.2.1</a>
 * for details of changes that occurred to device event payloads
 * and how to revert the behavior if needed.
 * <p>
 * @see <a href="{@docRoot}/org/apache/edgent/connectors/iot/package-summary.html">Edgent generic device model</a>
 * @see org.apache.edgent.samples.connectors.iotp.IotpSensors Sample application
 */
public class IotpDevice implements IotDevice {
    
    /**
     * Device type identifier ({@value}) used when using the Quickstart service.
     * @see #quickstart(Topology, String)
     */
    public static final String QUICKSTART_DEVICE_TYPE = "iotsamples-edgent";

    private final IotpConnector connector;
    private final Topology topology;
    private TStream<Command> commandStream;

    /**
     * Create a connector for the IoT device specified by {@code options}.
     * <BR>
     * These properties must be set in {@code options}.
     * 
     * <UL>
     * <LI>{@code org=}<em>organization identifier</em></LI>
     * <LI>{@code type=}<em>device type</em></LI>
     * <LI>{@code id=}<em>device identifier</em></LI>
     * <LI>{@code auth-method=token}</LI>
     * <LI>{@code auth-token=}<em>authorization token</em></LI>
     * </UL>
     * For example:
     * <pre>
     * <code>
     * Properties options = new Properties();
     * options.setProperty("org", "uguhsp");
     * options.setProperty("type", "iotsample-arduino");
     * options.setProperty("id", "00aabbccde03");
     * options.setProperty("auth-method", "token");
     * options.setProperty("auth-token", "AJfKQV@&amp;bBo@VX6Dcg");
     * 
     * IotDevice iotDevice = new IotpDevice(options);
     * </code>
     * </pre>
     * <p>
     * Connecting to the server occurs when the topology is submitted for
     * execution.
     * </p>
     * <p>
     * See the IBM Watson IoT Platform documentation for additional properties.
     * </p>
     *
     * @param options control options
     * @param topology
     *            the connector's associated {@code Topology}.
     */
    public IotpDevice(Topology topology, Properties options) {
        this.topology = topology;
        this.connector = new IotpConnector(options);
    }

    /**
     * Create a connector for the IoT device specified by {@code optionsFile}.
     * <BR>
     * The format of the file is:
     * <pre>
     * <code>
     * [device]
     * org = <em>organization identifier</em>
     * type = <em>device type</em>
     * id = <em>device identifier</em>
     * auth-method = token
     * auth-token = <em>authorization token</em>
     * </code>
     * </pre>
     * For example:
     * <pre>
     * <code>
     * [device]
     * org = uguhsp
     * type = iotsample-arduino
     * id = 00aabbccde03
     * auth-method = token
     * auth-token = AJfKQV@&amp;bBo@VX6Dcg
     * </code>
     * </pre>
     * <p>
     * Connecting to the server occurs when the topology is submitted for
     * execution.
     * </p>
     * <p>
     * See the IBM Watson IoT Platform documentation for additional properties.
     * </p>
     * @param topology the connector's associated {@code Topology}.
     * @param optionsFile File containing connection information.
     */
    public IotpDevice(Topology topology, File optionsFile) {
        this.topology = topology;
        this.connector = new IotpConnector(optionsFile);
    }
    
    /**
     * Create a connector using the supplied WIoTP {@code DeviceClient}.
     * @param topology the connector's associated {@code Topology}.
     * @param deviceClient a WIoTP device client API object.
     */
    public IotpDevice(Topology topology, DeviceClient deviceClient) {
      this.topology = topology;
      this.connector = new IotpConnector(deviceClient);
    }
    
    /**
     * Create an {@code IotpDevice} connector to the Quickstart service.
     * Quickstart service requires no-sign up to use to allow evaluation
     * but has limitations on functionality, such as only supporting
     * device events and only one message per second.
     * 
     * @param topology the connector's associated {@code Topology}.
     * @param deviceId Device identifier to use for the connection.
     * @return Connector to the Quickstart service.
     * 
     * @see <a href="https://quickstart.internetofthings.ibmcloud.com">Quickstart</a>
     * @see org.apache.edgent.samples.connectors.iotp.IotpQuickstart Quickstart sample application
     */
    public static IotpDevice quickstart(Topology topology, String deviceId) {
        Properties options = new Properties();
        options.setProperty("org", "quickstart");
        options.setProperty("type", QUICKSTART_DEVICE_TYPE);
        options.setProperty("id", deviceId);
        return new IotpDevice(topology, options);
    }

    /**
     * Publish a stream's tuples as device events.
     * <p>
     * Each tuple is published as a device event with the supplied functions
     * providing the event identifier, payload and QoS from the tuple.
     * The event identifier and {@link QoS Quality of Service}
     * can be generated based upon the tuple.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            function to supply the event identifier.
     * @param payload
     *            function to supply the event's payload.
     * @param qos
     *            function to supply the event's delivery {@link QoS Quality of Service}.
     * @return TSink sink element representing termination of this stream.
     * 
     * @see QoS
     */
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload,
            Function<JsonObject, Integer> qos) {
        return stream.sink(new IotpDeviceEventsFunction(connector, eventId, payload, qos));
    }
    
    /**
     * Publish a stream's tuples as device events.
     * <p>
     * Each tuple is published as a device event with fixed event identifier and
     * QoS.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            Event identifier.
     * @param qos
     *            Event's delivery {@link QoS Quality of Service}.
     * @return TSink sink element representing termination of this stream.
     * 
     * @see QoS
     */
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
        return stream.sink(new IotpDeviceEventsFixed(connector, eventId, qos));
    }

    /**
     * Publish a stream's tuples as device events using the WIoTP HTTP protocol.
     * <p>
     * Each tuple is published as a device event with the supplied functions
     * providing the event identifier and payload from the tuple.
     * The event identifier and payload can be generated based upon the tuple.
     * The event is published with the equivalent of {@link QoS#AT_MOST_ONCE}.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            function to supply the event identifier.
     * @param payload
     *            function to supply the event's payload.
     * @return TSink sink element representing termination of this stream.
     */
    public TSink<JsonObject> httpEvents(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
        UnaryOperator<JsonObject> payload) {
      return stream.sink(new IotpDeviceHttpEventsFunction(connector, eventId, payload));
    }
    
    /**
     * Publish a stream's tuples as device events using the WIoTP HTTP protocol.
     * <p>
     * Each tuple is published as a device event with the fixed event identifier.
     * The event is published with the equivalent of {@link QoS#AT_MOST_ONCE}.
     * 
     * @param stream
     *            Stream to be published.
     * @param eventId
     *            Event identifier.
     * @return TSink sink element representing termination of this stream.
     */
    public TSink<JsonObject> httpEvents(TStream<JsonObject> stream, String eventId) {
      return stream.sink(new IotpDeviceHttpEventsFixed(connector, eventId));
    }

    /**
     * Create a stream of device commands as JSON objects.
     * Each command sent to the device matching {@code commands} will result in a tuple
     * on the stream. The JSON object has these keys:
     * <UL>
     * <LI>{@code command} - Command identifier as a String</LI>
     * <LI>{@code tsms} - Timestamp of the command in milliseconds since the 1970/1/1 epoch.</LI>
     * <LI>{@code format} - Format of the command as a String</LI>
     * <LI>{@code payload} - Payload of the command
     * <UL>
     * <LI>If {@code format} is {@code json} then {@code payload} is JSON corresponding to the
     * command specific data.</LI>
     * <LI>Otherwise {@code payload} is String
     * </UL>
     * </LI>
     * </UL>
     * 
     * 
     * @param commands Commands to include. If no commands are provided then the
     * stream will contain all device commands.
     * @return Stream containing device commands.
     */
    public TStream<JsonObject> commands(String... commands) {
        TStream<Command> all = allCommands();
        
        if (commands.length != 0) {
            Set<String> uniqueCommands = new HashSet<>();
            uniqueCommands.addAll(Arrays.asList(commands));
            all = all.filter(cmd -> uniqueCommands.contains(cmd.getCommand()));
        }

        return all.map(cmd -> {
            JsonObject full = new JsonObject();
            full.addProperty(IotDevice.CMD_DEVICE, getDeviceId());
            full.addProperty(IotDevice.CMD_ID, cmd.getCommand());
            full.addProperty(IotDevice.CMD_TS, System.currentTimeMillis());
            full.addProperty(IotDevice.CMD_FORMAT, cmd.getFormat());
            if ("json".equalsIgnoreCase(cmd.getFormat())) {
                JsonParser parser = new JsonParser();
                // iot-java 0.2.2 bug https://github.com/ibm-watson-iot/iot-java/issues/81
                // cmd.getData() returns byte[] instead of JsonObject (or String).
                // Must continue to use the deprecated method until that's fixed.
                // final JsonObject jsonPayload = (JsonObject) cmd.getData();
                // final JsonObject jsonPayload = (JsonObject) parser.parse((String)cmd.getData());
                @SuppressWarnings("deprecation")
                final JsonObject jsonPayload = (JsonObject) parser.parse(cmd.getPayload());
                final JsonObject cmdData;
                // wiotp java client API >= 0.2.1 (other clients earlier?)
                // A json fmt command's msg payload may or may not have "d" wrapping of
                // the actual command data.
                // The wiotp client API doesn't mask that from clients
                // so deal with that here.
                if (jsonPayload.has("d")) {
                    cmdData = jsonPayload.getAsJsonObject("d");
                } else {
                    cmdData = jsonPayload;
                }
                full.add(IotDevice.CMD_PAYLOAD, cmdData);
            } else {
                full.addProperty(IotDevice.CMD_PAYLOAD, cmd.getData().toString());
            }
            return full;
            
        });
    }
    
    private TStream<Command> allCommands() {
        if (commandStream == null)
            commandStream = topology.events(new IotpDeviceCommands(connector));
        return commandStream;
    }

    @Override
    public Topology topology() {
        return topology;
    }

    @Override
    public String getDeviceType() {
      return connector.getDeviceType();
    }

    @Override
    public String getDeviceId() {
      return connector.getFqDeviceId();
    }
    
    @Override
    public String toString() {
      return String.format("IotpDevice %s", getDeviceId()); 
    }
}
