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
package org.apache.edgent.connectors.mqtt;

import java.nio.charset.StandardCharsets;

import org.apache.edgent.connectors.mqtt.runtime.MqttConnector;
import org.apache.edgent.connectors.mqtt.runtime.MqttPublisher;
import org.apache.edgent.connectors.mqtt.runtime.MqttSubscriber;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * {@code MqttStreams} is a connector to a MQTT messaging broker
 * for publishing and subscribing to topics.
 * <p>
 * For more information about MQTT see <a href="http://mqtt.org">http://mqtt.org</a>
 * <p>
 * The connector exposes all MQTT capabilities:
 * <ul>
 * <li>multiple server URLs using tcp or ssl</li>
 * <li>connection ClientId control</li>
 * <li>connection username and password based authentication</li>
 * <li>TODO SSL connection client authentication Q:expose just key/trust store path/pw or whole setSSLProperties capability?  Related: expose setSocketFactory?</li>
 * <li>connection clean session control</li>
 * <li>connection keepalive control</li>
 * <li>operation/action timeout control</li>
 * <li>client last will and testament</li>
 * <li>connection in-flight message persistence control</li>
 * <li>per-published message control of topic, quality of service, payload and retain value</li>
 * <li>subscription topicFilter and maximum quality of service control</li>
 * <li>TODO multiple subscription topicFilters</li>
 * <li>access to received message's topic and payload.
 *     Omitting until there's a clear demand for it: the received msg's isRetained, qos and isDuplicate values.
 * <li>TODO dynamic server URLs control, operation timeout, keepalive control</li>
 * <li>TODO dynamic subscription list control</li>
 * <li>robust connection management / reconnected</li>
 * <li>TODO fix: client's aren't gracefully disconnecting (their close() isn't getting called) issue#64</li>
 * </ul>
 * <p>
 * Sample use:
 * <pre>{@code
 * Topology t = ...;
 * String url = "tcp://localhost:1883";
 * MqttStreams mqtt = new MqttStreams(t, url); 
 * TStream<String> s = top.constants("one", "two", "three");
 * mqtt.publish(s, "myTopic", 0);
 * TStream<String> rcvd = mqtt.subscribe("someTopic", 0);
 * rcvd.print();
 * }</pre>
 * <P>
 * {@code publish()} can be called zero or more times.
 * {@code subscribe()} can be called at most once.
 * <p>
 * See {@link MqttConfig} for all configuration items.
 * <p>
 * Messages are delivered with the specified Quality of Service.
 * TODO adjust the QoS-1 and 2 descriptions based on the fact that we only
 * supply a MemoryPersistence class under the covers.
 * <ul>
 * <li>Quality of Service 0 - a message should be delivered at most once
 *     (zero or one times) - "fire and forget".
 *     This is the fastest QoS but should only be used for messages which
 *     are not valuable.</li>
 * <li>Quality of Service 1 - a message should be delivered at least once
 *     (zero or more times).
 *     The message will be acknowledged across the network.</li>
 * <li>Quality of Service 2 = a message should be delivered once.  
 *     Delivery is subject to two-phase acknowledgment across the network.</li>
 * </ul>
 * For {@code subscribe()}, the QoS is the maximum QoS used for a message.
 * If a message was published with a QoS less then the subscribe's QoS,
 * the message will be received with the published QoS,
 * otherwise it will be received with the subscribe's QoS.
 */
public class MqttStreams {

    private final MqttConnector connector;
    private final Topology topology;
    private int subscribeCnt;

    /**
     * Create a connector to the specified server.
     * <p>
     * A convenience function.
     * Connecting to the server occurs after the
     * org.apache.edgent.org.apache.edgent.topology is submitted for execution.
     * 
     * @param topology the connector's associated {@code Topology}.
     * @param url URL of MQTT server.
     * @param clientId the connector's MQTT clientId. auto-generated if null.
     */
    public MqttStreams(Topology topology, String url, String clientId) {
        this.topology = topology;
        MqttConfig config = new MqttConfig();
        config.setServerURLs(new String[] {url});
        config.setClientId(clientId);
        connector = new MqttConnector(() -> config);
    }

    /**
     * Create a connector with the specified configuration.
     * <p>
     * Connecting to the server occurs after the
     * org.apache.edgent.org.apache.edgent.topology is submitted for execution.
     * 
     * @param topology the Topology to add to
     * @param config {@link MqttConfig} supplier.
     */
    public MqttStreams(Topology topology, Supplier<MqttConfig> config) {
        this.topology = topology;
        
        connector = new MqttConnector(config);
    }

    /**
     * Publish a stream's tuples as MQTT messages. 
     * <p>Each tuple is published as an MQTT message with
     * the supplied functions providing the message topic, payload
     * and QoS. The topic and QoS can be generated based upon the tuple.
     * 
     * @param <T> Tuple type
     * @param stream Stream to be published.
     * @param topic function to supply the message's topic.
     * @param payload function to supply the message's payload.
     * @param qos function to supply the message's delivery Quality of Service.
     * @param retain function to supply the message's retain value
     * @return TSink sink element representing termination of this stream.
     */
    public <T> TSink<T> publish(TStream<T> stream, Function<T, String> topic, Function<T, byte[]> payload,
            Function<T, Integer> qos, Function<T, Boolean> retain) {
        return stream.sink(new MqttPublisher<T>(connector, payload, topic, qos, retain));
    }
    
    /**
     * Publish a {@code TStream<String>} stream's tuples as MQTT messages.
     * <p>
     * A convenience function.
     * The payload of each message is the String tuple's value serialized as UTF-8.
     * 
     * @param stream Stream to be published.
     * @param topic the fixed topic.
     * @param qos the fixed delivery Quality of Service.
     * @param retain the fixed retain value.
     * @return TSink sink element representing termination of this stream.
     */
    public TSink<String> publish(TStream<String> stream, String topic, int qos, boolean retain) {
        return publish(stream, tuple -> topic, tuple -> tuple.getBytes(StandardCharsets.UTF_8), tuple -> qos, tuple -> retain);
    }

    /**
     * Subscribe to the MQTT topic(s) and create a stream of tuples of type {@code T}.
     * @param <T> Tuple type
     * @param topicFilter the topic(s) to subscribe to.
     * @param qos the maximum Quality of Service to use.
     * @param message2Tuple function to convert {@code (topic, payload)} to
     *      a tuple of type {@code T}
     * @return {@code TStream<T>}
     */
    public <T> TStream<T> subscribe(String topicFilter, int qos, BiFunction<String, byte[], T> message2Tuple) {
        addSubscribe();
        return topology().events(new MqttSubscriber<T>(connector, topicFilter, qos, message2Tuple));
    }

    /**
     * Subscribe to the MQTT topic(s) and create a {@code TStream<String>}.
     * <p>
     * A convenience function.
     * Each message's payload is expected/required to be a UTF-8 encoded string.
     * Only the converted payload is present the generated tuple.
     *
     * @param <T> Tuple type
     * @param topicFilter the topic(s) to subscribe to.
     * @param qos the maximum Quality of Service to use.
     * @return {@code TStream<String>}
     * @see #publish(TStream, String, int, boolean)
     */
    public <T> TStream<String> subscribe(String topicFilter, int qos) {
        addSubscribe();
        return topology().events(new MqttSubscriber<String>(connector, topicFilter, qos,
                            (topic, payload) -> new String(payload, StandardCharsets.UTF_8)));
    }
    
    private void addSubscribe() {
        // This is in recognition of our current code that doesn't support >1 subscribe().
        // The issue is MqttClient only supports a single callback and right now
        // each MqttSubscriber registers itself... so only the last one wins,
        // and the last subscribe() rcvs all of the msgs and the others rcv none.
        // Workaround: use multiple connectors.
        if (++subscribeCnt > 1)
            throw new IllegalStateException("An MqttStreams instance supports at most one subscribe()");
    }

    /**
     * Get the {@link org.apache.edgent.topology.Topology} the connector is associated with.
     * @return the value
     */
    public Topology topology() {
        return topology;
    }
}
