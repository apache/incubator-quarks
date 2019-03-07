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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.IotGateway;
import org.apache.edgent.connectors.iotp.runtime.IotpGWCommands;
import org.apache.edgent.connectors.iotp.runtime.IotpGWConnector;
import org.apache.edgent.connectors.iotp.runtime.IotpGWDeviceEventsFixed;
import org.apache.edgent.connectors.iotp.runtime.IotpGWDeviceEventsFunction;
import org.apache.edgent.connectors.iotp.runtime.IotpGWEventsFixed;
import org.apache.edgent.connectors.iotp.runtime.IotpGWEventsFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.gateway.Command;
import com.ibm.iotf.client.gateway.GatewayCallback;
import com.ibm.iotf.client.gateway.GatewayClient;

/**
 * An IoT Gateway Device Connector to IBM Watson IoT Platform (WIoTP).
 * <p>
 * <b>This class is incubating and is subject to change.</b>
 * <p>
 * This connector is a thin wrapper over the WIoTP {@code GatewayClient} Java API.
 * The constructor {@code Properties} and {@code optionsFile} contents are those
 * demanded by {@code GatewayClient}.
 * <p>
 * See {@link IotpDevice} for common WIoTP documentation references.
 * <p>
 * {@code IotpGateway} establishes its own WIoTP {@link GatewayCallback}
 * handler in its embedded or the passed in WIoTP {@link GatewayClient}.
 * An application can use 
 * {@link #setExternalCallbackHandler(GatewayCallback) setExternalCallbackHandler}
 * to also receive and process callbacks.
 * <p>
 * Limitations:
 * <ul>
 * <li>{@code IotProvider} and {@code PublishSubscribeService} do not yet support
 * the gateway model.  An IotProvider may be initialized with an IotpGateway
 * but the PublishSubscribeService mechanism will only publish events and receive
 * commands for the gateway device, not any of its connected devices.
 * </li>
 * </ul>
 */
public class IotpGateway implements IotGateway {

  private final IotpGWConnector connector;
  private final Topology topology;
  private TStream<Command> commandStream;

  /**
   * Create a connector for the IoT gateway device specified by {@code options}.
   * <BR>
   * These properties must be set in {@code options}.
   * 
   * <UL>
   * <LI>{@code org=}<em>organization identifier</em></LI>
   * <LI>{@code type=}<em>gateway device type</em></LI>
   * <LI>{@code id=}<em>gateway device identifier</em></LI>
   * <LI>{@code auth-method=token}</LI>
   * <LI>{@code auth-token=}<em>authorization token</em></LI>
   * </UL>
   * For example:
   * <pre>
   * <code>
   * Properties options = new Properties();
   * options.setProperty("org", "uguhsp");
   * options.setProperty("type", "iotsample-gateway");
   * options.setProperty("id", "00aabbccde03");
   * options.setProperty("auth-method", "token");
   * options.setProperty("auth-token", "AJfKQV@&amp;bBo@VX6Dcg");
   * 
   * IotDevice iotDevice = new IotpDevice(options);
   * </code>
   * </pre>
   * <p>
   * Connecting to the server occurs when the org.apache.edgent.org.apache.edgent.topology is submitted for
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
  public IotpGateway(Topology topology, Properties options) {
      this.topology = topology;
      this.connector = new IotpGWConnector(options);
  }

  /**
   * Create a connector for the IoT gateway device specified by {@code optionsFile}.
   * <BR>
   * The format of the file is:
   * <pre>
   * <code>
   * [device]
   * org = <em>organization identifier</em>
   * type = <em>gateway device type</em>
   * id = <em>gateway device identifier</em>
   * auth-method = token
   * auth-token = <em>authorization token</em>
   * </code>
   * </pre>
   * For example:
   * <pre>
   * <code>
   * [device]
   * org = uguhsp
   * type = iotsample-gateway
   * id = 00aabbccde03
   * auth-method = token
   * auth-token = AJfKQV@&amp;bBo@VX6Dcg
   * </code>
   * </pre>
   * <p>
   * Connecting to the server occurs when the org.apache.edgent.org.apache.edgent.topology is submitted for
   * execution.
   * </p>
   * <p>
   * See the IBM Watson IoT Platform documentation for additional properties.
   * </p>
   * @param topology the connector's associated {@code Topology}.
   * @param optionsFile File containing connection information.
   */
  public IotpGateway(Topology topology, File optionsFile) {
      this.topology = topology;
      this.connector = new IotpGWConnector(optionsFile);
  }
  
  /**
   * Create a connector using the supplied WIoTP {@code DeviceClient}.
   * @param topology the connector's associated {@code Topology}.
   * @param gatewayClient a WIoTP device client API object.
   */
  public IotpGateway(Topology topology, GatewayClient gatewayClient) {
    this.topology = topology;
    this.connector = new IotpGWConnector(gatewayClient);
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
  public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
      UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
    return stream.sink(new IotpGWEventsFunction(connector, eventId, payload, qos));
  }

  @Override
  public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
    return stream.sink(new IotpGWEventsFixed(connector, eventId, qos));
  }

  @Override
  public TStream<JsonObject> commands(String... commands) {
    return commandsForDevice(Collections.singleton(connector.getFqDeviceId()), commands);
  }
  
  private TStream<Command> allCommands() {
      if (commandStream == null)
          commandStream = topology.events(new IotpGWCommands(connector));
      return commandStream;
  }

  @Override
  public Topology topology() {
    return topology;
  }

  /**
   * WIoTP Device Type identifier key.
   * Key is {@value}.
   * 
   * @see #getIotDevice(Map)
   */
  public static final String ATTR_DEVICE_TYPE = "deviceType";

  /**
   * WIoTP Device Id identifier key.
   * Key is {@value}.
   * 
   * @see #getIotDevice(Map)
   */
  public static final String ATTR_DEVICE_ID = "deviceId";

  /**
   * {@inheritDoc}
   * <p>
   * The device's WIoTP deviceType and deviceId must be supplied
   * using the {@link #ATTR_DEVICE_TYPE} and {@link #ATTR_DEVICE_ID}
   * keys respectively.
   */
  @Override
  public String getIotDeviceId(Map<String, String> deviceIdAttrs) {
    return connector.getIotDeviceId(deviceIdAttrs);
  }

  /**
   * {@inheritDoc}
   * <p>
   * See {@link #getIotDeviceId(Map)} for the required attribute keys.
   */
  @Override
  public IotDevice getIotDevice(Map<String, String> deviceIdAttrs) {
    return getIotDevice(getIotDeviceId(deviceIdAttrs));
  }

  @Override
  public IotDevice getIotDevice(String fqDeviceId) {
    return new IotpGWDevice(this, connector, topology, fqDeviceId);
  }

  @Override
  public TSink<JsonObject> eventsForDevice(Function<JsonObject, String> fqDeviceId,
      TStream<JsonObject> stream, Function<JsonObject, String> eventId,
      UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
    return stream.sink(new IotpGWDeviceEventsFunction(connector, fqDeviceId, eventId, payload, qos));
  }

  @Override
  public TSink<JsonObject> eventsForDevice(String fqDeviceId, TStream<JsonObject> stream,
      String eventId, int qos) {
    return stream.sink(new IotpGWDeviceEventsFixed(connector, fqDeviceId, eventId, qos));
  }

  @Override
  public TStream<JsonObject> commandsForDevice(Set<String> fqDeviceIds, String... commands) {
    TStream<Command> all = allCommands();

    if (fqDeviceIds.size() != 0) {
      // support "all devices of type T" - fqDeviceId of typeId and "*" for the simple deviceId
      boolean allDevicesOfType = fqDeviceIds.size() == 1
          && IotpGWConnector.splitFqDeviceId(fqDeviceIds.iterator().next())[1].equals("*");
      
      all = all.filter(cmd -> {
        String fqDeviceId = IotpGWConnector.toFqDeviceId(cmd.getDeviceType(), 
              allDevicesOfType ? "*" : cmd.getDeviceId());
        return fqDeviceIds.contains(fqDeviceId);
      });
    }
    
    if (commands.length != 0) {
        Set<String> uniqueCommands = new HashSet<>();
        uniqueCommands.addAll(Arrays.asList(commands));
        all = all.filter(cmd -> uniqueCommands.contains(cmd.getCommand()));
    }

    return all.map(cmd -> {
        JsonObject full = new JsonObject();
        full.addProperty(IotDevice.CMD_DEVICE, 
            IotpGWConnector.toFqDeviceId(cmd.getDeviceType(), cmd.getDeviceId()));
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

  @Override
  public TStream<JsonObject> commandsForDevice(String fqDeviceId, String... commands) {
    return commandsForDevice(Collections.singleton(fqDeviceId), commands);
  }

  @Override
  public TStream<JsonObject> commandsForType(String deviceTypeId, String... commands) {
    return commandsForDevice(
        Collections.singleton(IotpGWConnector.toFqDeviceId(deviceTypeId, "*")), commands);
  }

  /**
   * Set an external WIoTP {@link GatewayCallback} handler.
   * 
   * @param handler the handler to call. May be null.
   * @return the previously set handler. May be null.
   */
  public GatewayCallback setExternalCallbackHandler(GatewayCallback handler) {
    return connector.setExternalCallbackHandler(handler);
  }
  
  @Override
  public String toString() {
    return String.format("IotpGateway %s", getDeviceId()); 
  }

}
