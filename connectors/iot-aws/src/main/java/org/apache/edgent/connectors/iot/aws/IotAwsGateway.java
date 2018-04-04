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
package org.apache.edgent.connectors.iot.aws;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.IotGateway;
import org.apache.edgent.connectors.iot.aws.help.Command;
import org.apache.edgent.connectors.iot.aws.runtime.IotAwsGatewayConnector;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import java.io.File;
import java.util.*;

public class IotAwsGateway implements IotGateway {

    private final IotAwsGatewayConnector connector;
    private final Topology topology;
    private TStream<Command> commandStream;

    public IotAwsGateway(Topology topology, Properties options) {
        this.topology = topology;
        this.connector = new IotAwsGatewayConnector(options);
    }

    public IotAwsGateway(Topology topology, File optionsFile) {
        this.topology = topology;
        this.connector = new IotAwsGatewayConnector(optionsFile);
    }

    @Override
    public String getDeviceType() {
        return connector.getDeviceType();
    }

    @Override
    public String getDeviceId() {
        return connector.getDeviceId();
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId, UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
        return null;
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
        return null;
    }

    @Override
    public TStream<JsonObject> commands(String... commands) {
        return null;
    }

    private TStream<Command> allCommands() {
        if (commandStream == null)
            commandStream = topology.events(new IotAwsGatewayCommands(connector));
        return commandStream;
    }

    @Override
    public Topology topology() {
        return topology;
    }

    @Override
    public String getIotDeviceId(Map<String, String> deviceIdAttrs) {
        return connector.getIotDeviceId(deviceIdAttrs);
    }

    @Override
    public IotDevice getIotDevice(Map<String, String> deviceIdAttrs) {
        return getIotDevice(getIotDeviceId(deviceIdAttrs));
    }

    @Override
    public IotDevice getIotDevice(String deviceId) {
        return new IotAwsGatewayDevice(this, connector, topology, deviceId);
    }

    @Override
    public TSink<JsonObject> eventsForDevice(Function<JsonObject, String> deviceId, TStream<JsonObject> stream, Function<JsonObject, String> eventId, UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
        return stream.sink(new IotAwsGatewayDeviceEventsFunction(connector, deviceId, eventId, payload, qos));
    }

    @Override
    public TSink<JsonObject> eventsForDevice(String deviceId, TStream<JsonObject> stream, String eventId, int qos) {
        return stream.sink(new IotAwsGatewayDeviceEventsFixed(connector, deviceId, eventId, qos));
    }

    @Override
    public TStream<JsonObject> commandsForDevice(Set<String> deviceIds, String... commands) {
        TStream<Command> all = allCommands();

        if (deviceIds.size() != 0) {
            // support "all devices of type T" - fqDeviceId of typeId and "*" for the simple deviceId
            boolean allDevicesOfType = deviceIds.size() == 1
                && IotAwsGatewayConnector.splitDeviceId(deviceIds.iterator().next())[1].equals("*");

            all = all.filter(cmd -> {
                String fqDeviceId = IotAwsGatewayConnector.toDeviceId(cmd.getDeviceType(),
                    allDevicesOfType ? "*" : cmd.getDeviceId());
                return deviceIds.contains(fqDeviceId);
            });
        }

        if (commands.length != 0) {
            Set<String> uniqueCommands = new HashSet<>(Arrays.asList(commands));
            all = all.filter(cmd -> uniqueCommands.contains(cmd.getCommand()));
        }

        return all.map(cmd -> {
            JsonObject full = new JsonObject();
            full.addProperty(IotDevice.CMD_DEVICE,
                IotAwsGatewayConnector.toDeviceId(cmd.getDeviceType(), cmd.getDeviceId()));
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
        });    }

    @Override
    public TStream<JsonObject> commandsForDevice(String deviceId, String... commands) {
        return commandsForDevice(Collections.singleton(deviceId), commands);
    }

    @Override
    public TStream<JsonObject> commandsForType(String deviceTypeId, String... commands) {
        return commandsForDevice(
            Collections.singleton(IotAwsGatewayConnector.toDeviceId(deviceTypeId, "*")), commands);
    }

}
