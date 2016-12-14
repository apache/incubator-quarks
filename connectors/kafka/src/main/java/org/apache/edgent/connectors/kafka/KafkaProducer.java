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
package org.apache.edgent.connectors.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.edgent.connectors.kafka.runtime.KafkaProducerConnector;
import org.apache.edgent.connectors.kafka.runtime.KafkaPublisher;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * {@code KafkaProducer} is a connector for publishing a stream of tuples
 * to Apache Kafka messaging system topics.
 * <p>
 * The connector uses and includes components from the Kafka 0.8.2.2 release.
 * It has been successfully tested against kafka_2.11-0.10.1.0 and kafka_2.11-0.9.0.0 server as well.
 * For more information about Kafka see
 * <a href="http://kafka.apache.org">http://kafka.apache.org</a>
 * <p>
 * Sample use:
 * <pre>{@code
 * String bootstrapServers = "localhost:9092";
 * String topic = "mySensorReadingsTopic";
 * 
 * Map<String,Object> config = new HashMap<>();
 * config.put("bootstrap.servers", bootstrapServers);
 * 
 * Topology t = ...
 * KafkaProducer kafka = new KafkaProducer(t, () -> config);
 * 
 * TStream<JsonObject> sensorReadings = t.poll(
 *              () -> getSensorReading(), 5, TimeUnit.SECONDS);
 *              
 * // publish as sensor readings as JSON
 * kafka.publish(sensonReadings, tuple -> tuple.toString(), topic);
 * }</pre>
 */
public class KafkaProducer {
    @SuppressWarnings("unused")
    private final Topology t;
    private final KafkaProducerConnector connector;
   
    /**
     * Create a producer connector for publishing tuples to Kafka topics.s
     * <p>
     * See the Apache Kafka documentation for {@code KafkaProducer}
     * configuration properties at <a href="http://kafka.apache.org">http://kafka.apache.org</a>.
     * Configuration property values are strings.
     * <p>
     * The Kafka "New Producer configs" are used.  Minimal configuration
     * typically includes:
     * <ul>
     * <li><code>bootstrap.servers</code></li>
     * </ul>
     * <p>
     * The full set of producer configuration items are specified in
     * {@code org.apache.kafka.clients.producer.ProducerConfig}
     * 
     * @param t Topology to add to
     * @param config KafkaProducer configuration information.
     */
    public KafkaProducer(Topology t, Supplier<Map<String,Object>> config) {
        this.t = t;
        connector = new KafkaProducerConnector(config);
    }

    /**
     * Publish the stream of tuples as Kafka key/value records
     * to the specified topic partitions.
     * <p>
     * If a valid partition number is specified that partition will be used
     * when sending the message.  If no partition is specified but a key is
     * present a partition will be chosen using a hash of the key.
     * If neither key nor partition is present a partition will be assigned
     * in a round-robin fashion.
     * 
     * @param <T> Tuple type
     * @param stream the stream to publish
     * @param keyFn A function that yields an optional byte[] 
     *        Kafka record's key from the tuple.
     *        Specify null or return a null value for no key.
     * @param valueFn A function that yields the byte[]
     *        Kafka record's value from the tuple.
     * @param topicFn A function that yields the topic from the tuple.
     * @param partitionFn A function that yields the optional topic
     *        partition specification from the tuple.
     *        Specify null or return a null value for no partition specification.
     * @return {@link TSink}
     */
    public <T> TSink<T> publishBytes(TStream<T> stream, Function<T,byte[]> keyFn, Function<T,byte[]> valueFn, Function<T,String> topicFn, Function<T,Integer> partitionFn) {
        return stream.sink(new KafkaPublisher<T>(connector, keyFn, valueFn, topicFn, partitionFn));
    }
    
    /**
     * Publish the stream of tuples as Kafka key/value records
     * to the specified partitions of the specified topics.
     * <p>
     * This is a convenience method for {@code String} typed key/value
     * conversion functions.
     * <p>
     * @param <T> Tuple type
     * @param stream the stream to publish
     * @param keyFn A function that yields an optional String 
     *        Kafka record's key from the tuple.
     *        Specify null or return a null value for no key.
     * @param valueFn A function that yields the String for the
     *        Kafka record's value from the tuple.
     * @param topicFn A function that yields the topic from the tuple.
     * @param partitionFn A function that yields the optional topic
     *        partition specification from the tuple.
     *        Specify null or return a null value for no partition specification.
     * @return {@link TSink}
     * @see #publishBytes(TStream, Function, Function, Function, Function)
     */
    public <T> TSink<T> publish(TStream<T> stream, Function<T,String> keyFn, Function<T,String> valueFn, Function<T,String> topicFn, Function<T,Integer> partitionFn) {
        Function<T,byte[]> keyFn2 = null;
        if (keyFn != null) {
            keyFn2 = tuple -> { String key = keyFn.apply(tuple);
                                return key==null
                                        ? null
                                        : key.getBytes(StandardCharsets.UTF_8);
                              };
        }
        return publishBytes(stream, keyFn2,
                tuple -> valueFn.apply(tuple).getBytes(StandardCharsets.UTF_8), 
                topicFn, partitionFn);
    }

    /**
     * Publish the stream of tuples as Kafka key/value records
     * to the specified partitions of the specified topics.
     * <p>
     * This is a convenience method for a String stream published
     * as a Kafka record with no key and
     * a value consisting of the String tuple serialized as UTF-8,
     * and publishing round-robin to a fixed topic's partitions.
     * 
     * @param stream the stream to publish
     * @param topic The topic to publish to
     * @return {@link TSink}
     * @see #publish(TStream, Function, Function, Function, Function)
     */
    public TSink<String> publish(TStream<String> stream, String topic) {
        return publish(stream, null, tuple -> tuple, tuple -> topic, null);
    }
}
