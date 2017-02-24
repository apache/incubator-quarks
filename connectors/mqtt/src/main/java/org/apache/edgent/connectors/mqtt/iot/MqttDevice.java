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
package org.apache.edgent.connectors.mqtt.iot;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.mqtt.MqttConfig;
import org.apache.edgent.connectors.mqtt.MqttStreams;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.json.JsonFunctions;

import com.google.gson.JsonObject;

/**
 * An MQTT based Edgent {@link IotDevice} connector.
 * <p>
 * The MQTT {@code IotDevice} is an abstraction on top of
 * the {@link MqttStreams} connector.
 * <p>
 * The connector doesn't presume a particular pattern for 
 * Device MQTT "event" topic and "command" topics though default
 * patterns are provided.
 * <p>
 * Connector configuration Properties fall into two categories:
 * <ul>
 * <li>MQTT Device abstraction properties</li>
 * <li>Base MQTT connector properties - see {@link MqttConfig#fromProperties(Properties)}
 * </ul>
 *
 * <h3>Device properties</h3>
 * <ul>
 * <li>mqttDevice.id - Required. An identifier that uniquely identifies
 *     the device in the device event and device command MQTT topic namespaces.
 *     </li>
 * <li>mqttDevice.topic.prefix - A optional prefix that by default is used when
 *     composing device event and command MQTT topics, and the client's MQTT
 *     clientId.  The default is no prefix.</li>
 * <li>mqttDevice.event.topic.pattern - Optional.  The topic pattern used
 *     for MQTT device event topics.
 *     Defaults to {@code {mqttDevice.topic.prefix}id/{mqttDevice.id}/evt/{EVENTID}/fmt/json}
 *     The pattern must include {EVENTID} and must end with "/fmt/json".
 *     </li>
 * <li>mqttDevice.command.topic.pattern - Optional.  The topic pattern used
 *     for MQTT device command topics.
 *     Defaults to {@code {mqttDevice.topic.prefix}id/{mqttDevice.id}/cmd/{COMMAND}/fmt/json}
 *     The pattern must include {COMMAND} and must end with "/fmt/json".
 *     </li>
 * <li>mqttDevice.command.qos - An optional MQTT QoS value for commands. Defaults to 0.</li>
 * <li>mqttDevice.events.retain - Optional MQTT "retain" behavior for published events.  Defaults to false.</li>
 * <li>mqttDevice.mqtt.clientId - Optional value to use for the MQTT clientId.
 *     Defaults to {mqttDevice.topic.prefix}id/{mqttDevice.id}.</li>
 * </ul>
 * Sample use:
 * <pre>{@code
 *  // assuming a properties file containing at least:
 *  // mqttDevice.id=012345678
 *  // mqtt.serverURLs=tcp://myMqttBrokerHost:1883
 *  
 * String propsPath = <path to properties file>; 
 * Properties properties = new Properties();
 * properties.load(Files.newBufferedReader(new File(propsPath).toPath()));

 * Topology t = new DirectProvider();
 * MqttDevice mqttDevice = new MqttDevice(t, properties);
 * 
 * // publish JSON "status" device event tuples every hour
 * TStream<JsonObject> myStatusEvents = t.poll(myGetStatusAsJson(), 1, TimeUnit.HOURS);
 * mqttDevice.events(myStatusEvents, "status", QoS.FIRE_AND_FORGET);
 * 
 * // handle a device command.  In this example the payload is expected
 * // to be JSON and have a "value" property containing the new threshold. 
 * mqttDevice.command("setSensorThreshold")
 *     .sink(json -> setSensorThreshold(json.get(CMD_PAYLOAD).getAsJsonObject().get("value").getAsString());
 * }</pre>
 */
public class MqttDevice implements IotDevice {
    
    private final Topology topology;
    private final String deviceId;
    private String topicPrefix = "";
    private String clientId = "{mqttDevice.topic.prefix}id/{mqttDevice.id}";
    private String evtTopic = "{mqttDevice.topic.prefix}id/{mqttDevice.id}/evt/{EVENTID}/fmt/json";
    private String cmdTopic = "{mqttDevice.topic.prefix}id/{mqttDevice.id}/cmd/{COMMAND}/fmt/json";
    private int commandQoS = 0;
    private boolean retainEvents = false;
    private final MqttConfig mqttConfig;
    private final MqttStreams connector;
    private TStream<JsonObject> commandStream;
    
    /**
     * Create an MqttDevice connector.
     * <p>
     * All configuration information comes from {@code properties}.
     *  
     * @param topology topology to add the connector to.
     * @param properties connector properties.
     */
    public MqttDevice(Topology topology, Properties properties) {
        this(topology, properties, null);
    }

    /**
     * Create an MqttDevice connector.
     * <p>
     * Uses {@code mattConfig} for the base MQTT connector configuration
     * and uses {@code properties} only for MQTT Device properties.
     * 
     * @param topology topology to add the connector to.
     * @param properties connector properties.  Properties beyond those
     *        noted in the Device properties section above are ignored.
     * @param mqttConfig base MQTT configuration. may be null.
     */
    public MqttDevice(Topology topology, Properties properties, MqttConfig mqttConfig) {
        this.topology = topology;
        this.deviceId = properties.getProperty("mqttDevice.id");
        if (deviceId == null || deviceId.isEmpty())
            throw new IllegalArgumentException("mqttDevice.id");
        String cqos = properties.getProperty("mqttDevice.command.qos", Integer.valueOf(commandQoS).toString());
        commandQoS = Integer.valueOf(cqos); 
        String eretain = properties.getProperty("mqttDevice.events.retain", Boolean.valueOf(retainEvents).toString());
        retainEvents = Boolean.valueOf(eretain);
        topicPrefix = properties.getProperty("mqttDevice.topic.prefix", topicPrefix);
        clientId = properties.getProperty("mqttDevice.mqtt.clientId", clientId);
        evtTopic = properties.getProperty("mqttDevice.event.topic.pattern", evtTopic);
        if (!evtTopic.endsWith("/fmt/json"))
            throw new IllegalArgumentException("mqttDevice.event.topic.pattern");
        cmdTopic = properties.getProperty("mqttDevice.command.topic.pattern", cmdTopic);
        if (!cmdTopic.endsWith("/fmt/json"))
            throw new IllegalArgumentException("mqttDevice.command.topic.pattern");
        initVars();
        if (mqttConfig == null) {
            mqttConfig = MqttConfig.fromProperties(properties);
            mqttConfig.setClientId(clientId);
        }
        this.mqttConfig = mqttConfig;
        this.connector = new MqttStreams(topology, () -> this.mqttConfig);
    }
    
    private void initVars() {
        clientId = clientId
                    .replace("{mqttDevice.topic.prefix}", topicPrefix)
                    .replace("{mqttDevice.id}", deviceId);
        evtTopic = evtTopic
                    .replace("{mqttDevice.topic.prefix}", topicPrefix)
                    .replace("{mqttDevice.id}", deviceId);
        cmdTopic = cmdTopic
                    .replace("{mqttDevice.topic.prefix}", topicPrefix)
                    .replace("{mqttDevice.id}", deviceId);
    }
    
    /**
     * Get the MQTT topic for an device event.
     * @param eventId the event id. 
     *         if null, returns a topic filter for all of the device's events.
     * @return the topic
     */
    public String eventTopic(String eventId) {
        if (eventId == null) {
            eventId = "+";  // retain the trailing fmt/json
        }
        return evtTopic.replace("{EVENTID}", eventId);
    }
    
    /**
     * Get the MQTT topic for a command.
     * @param command the command id.
     *         if null, returns a topic filter for all of the device's commands.
     * @return the topic
     */
    public String commandTopic(String command) {
        if (command == null) {
            command = "+";  // retain the trailing fmt/json
        }
        return cmdTopic.replace("{COMMAND}", command);
    }
    
    /**
     * Get the device's {@link MqttConfig}
     * @return the config
     */
    public MqttConfig getMqttConfig() {
        return mqttConfig;
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
        
        Function<JsonObject, String> topic = jo -> eventTopic(eventId.apply(jo));
        Function<JsonObject,byte[]> payloadFn = 
                jo -> JsonFunctions.asBytes().apply(payload.apply(jo));
        
        return connector.publish(stream, topic, payloadFn, qos, jo -> retainEvents);
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
        return events(stream, jo -> eventId, jo -> jo, jo -> qos);
    }

    @Override
    public TStream<JsonObject> commands(String... commands) {
        TStream<JsonObject> all = allCommands();
        
        if (commands.length != 0) {
            Set<String> uniqueCommands = new HashSet<>();
            uniqueCommands.addAll(Arrays.asList(commands));
            all = all.filter(jo -> uniqueCommands.contains(jo.get(CMD_ID).getAsString()));
        }
        
        return all;
    }
    
    private TStream<JsonObject> allCommands() {
        if (commandStream == null) {
            String topicFilter = commandTopic(null);
            commandStream = connector.subscribe(topicFilter, commandQoS,
                    (topic, payload) -> {
                        JsonObject jo = new JsonObject();
                        jo.addProperty(CMD_DEVICE, deviceId);
                        jo.addProperty(CMD_ID, extractCmd(topic));
                        jo.addProperty(CMD_TS, System.currentTimeMillis());
                        String fmt = extractCmdFmt(topic);
                        jo.addProperty(CMD_FORMAT, fmt);
                        if ("json".equals(fmt)) {
                            jo.add(CMD_PAYLOAD, JsonFunctions.fromBytes().apply(payload));
                        }
                        else {
                            jo.addProperty(CMD_PAYLOAD, new String(payload, StandardCharsets.UTF_8));
                        }
                        return jo;
                    })
                    .tag("allDeviceCmds");
        }
        return commandStream;
    }
    
    private String extractCmd(String topic) {
        String prefix = cmdTopic.substring(0, cmdTopic.indexOf("{COMMAND}"));
        String cmd = topic.substring(prefix.length());
        int endCmd = cmd.indexOf('/');
        if (endCmd != -1)
            cmd = cmd.substring(0, endCmd);
        return cmd;
    }
    
    private String extractCmdFmt(String cmdTopic) {
        return cmdTopic.endsWith("/fmt/json") ? "json" : "string";
    }

    @Override
    public Topology topology() {
        return topology;
    }

    /**
     * {@inheritDoc}
     * <p> 
     * This connector does not support the notion of a device-type
     * as part of its device id model.  An empty string is returned.
     */
    @Override
    public String getDeviceType() {
      // not part of this connector's device identifier model
      return "";
    }

    @Override
    public String getDeviceId() {
      return deviceId;
    }
}
