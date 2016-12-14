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

import java.util.Map;

import org.apache.edgent.connectors.kafka.runtime.KafkaConsumerConnector;
import org.apache.edgent.connectors.kafka.runtime.KafkaSubscriber;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * {@code KafkaConsumer} is a connector for creating a stream of tuples
 * by subscribing to Apache Kafka messaging system topics.
 * <p>
 * The connector uses and includes components from the Kafka 0.8.2.2 release.
 * It has been successfully tested against kafka_2.11-0.10.1.0 and kafka_2.11-0.9.0.0 server as well.
 * For more information about Kafka see
 * <a href="http://kafka.apache.org">http://kafka.apache.org</a>
 * <p>
 * Sample use:
 * <pre>{@code
 * String zookeeperConnect = "localhost:2181";
 * String groupId = "myGroupId";
 * String topic = "mySensorReadingsTopic";
 * 
 * Map<String,Object> config = new HashMap<>();
 * config.put("zookeeper.connect", zookeeperConnect);
 * config.put("groupId", groupId);
 * 
 * Topology t = ...
 * KafkaConsumer kafka = new KafkaConsumer(t, () -> config);
 *              
 * // subscribe to a topic where sensor readings are published as JSON,
 * // creating a stream of JSON tuples  
 * TStream<String> sensorReadingsJson =
 *              kafka.subscribe(rec -> rec.value(), topic);
 * 
 * // print the received messages
 * sensorReadingsJson.print();
 * }</pre>
 */
public class KafkaConsumer {
    private final Topology t;
    private final KafkaConsumerConnector connector;
    
    /**
     * A received Kafka record
     *
     * @param <K> key's type
     * @param <V> the value's type
     */
    public interface ConsumerRecord<K,V> {
        String topic();
        int partition();
        /** @return message id in the partition. */
        long offset();
        /** @return null if no key was published. */
        K key();
        V value();
    }
    
    /**
     * A Kafka record with byte[] typed key and value members
     */
    public interface ByteConsumerRecord extends ConsumerRecord<byte[],byte[]> {
    }
    
    /**
     * A Kafka record with String typed key and value members
     */
    public interface StringConsumerRecord extends ConsumerRecord<String,String> {
    }

    // unbaked 8.2.2 KafkaConsumer
//    /**
//     * A <topic,partition> pair. 
//     */
//    public interface TopicPartition {
//        String topic();
//        int partition();
//    }
   
    /**
     * Create a consumer connector for subscribing to Kafka topics
     * and creating tuples from the received messages.
     * <p>
     * See the Apache Kafka documentation for "Old Consumer Configs"
     * configuration properties at <a href="http://kafka.apache.org">http://kafka.apache.org</a>.
     * Configuration property values are strings.
     * <p>
     * The Kafka "Old Consumer" configs are used.  Minimal configuration
     * typically includes:
     * <ul>
     * <li><code>zookeeper.connect</code></li>
     * <li><code>group.id</code></li>
     * </ul>
     *
     * @param t Topology to add to
     * @param config KafkaConsumer configuration information.
     */
    public KafkaConsumer(Topology t, Supplier<Map<String,Object>> config) {
        this.t = t;
        connector = new KafkaConsumerConnector(config);
    }
    
    /**
     * Subscribe to the specified topics and yield a stream of tuples
     * from the published Kafka records.
     * <p>
     * Kafka's consumer group management functionality is used to automatically
     * allocate, and dynamically reallocate, the topic's partitions to this connector. 
     * <p>
     * In line with Kafka's evolving new KafkaConsumer interface, subscribing
     * to a topic advertises a single thread to the server for partition allocation.
     * <p>
     * Currently, subscribe*() can only be called once for a KafkaConsumer
     * instance.  This restriction will be removed once we migrate to Kafka 0.9.0.0.
     * 
     * @param <T> tuple type
     * 
     * @param toTupleFn A function that yields a tuple from a {@code ByteConsumerRecord}
     * @param topics the topics to subscribe to.
     * @return stream of tuples
     * @throws IllegalArgumentException for a duplicate or conflicting subscription
     */
    public <T> TStream<T> subscribeBytes(Function<ByteConsumerRecord,T> toTupleFn, String... topics) {
        return t.events(new KafkaSubscriber<T>(connector, toTupleFn, false, topics));
    }

    /**
     * Subscribe to the specified topics and yield a stream of tuples
     * from the published Kafka records.
     * <p>
     * Kafka's consumer group management functionality is used to automatically
     * allocate, and dynamically reallocate, the topic's partitions to this connector. 
     * <p>
     * In line with Kafka's evolving new KafkaConsumer interface, subscribing
     * to a topic advertises a single thread to the server for partition allocation.
     * <p>
     * Currently, subscribe*() can only be called once for a KafkaConsumer
     * instance.  This restriction will be removed once we migrate to Kafka 0.9.0.0.
     * 
     * @param <T> tuple type
     * 
     * @param toTupleFn A function that yields a tuple from a {@code StringConsumerRecord}
     * @param topics the topics to subscribe to.
     * @return stream of tuples
     * @throws IllegalArgumentException for a duplicate or conflicting subscription
     */
    public <T> TStream<T> subscribe(Function<StringConsumerRecord,T> toTupleFn, String... topics) {
        return t.events(new KafkaSubscriber<T>(connector, toTupleFn, true, topics));
    }

    // The explicit topicPartition style of subscription is part of the
    // Kafka's new KafkaConsumer API and is unbaked as of 8.2.2
//    /**
//     * Subscribe to the specified topic partitions and yield a stream of tuples
//     * from the published Kafka records.
//     * <p>
//     * Kafka's consumer group management functionality is not used. 
//     * <p>
//     * Use of this method is mutually exclusive with 
//     * {@link #subscribe(Function, String...)}
//     *
//     * @param <T> tuple type
//     * 
//     * @param toTupleFn A function that yields a tuple from a {@code Record}
//     * @param topicPartitions the topic partitions to subscribe to.
//     * @return stream of tuples
//     * @throws IllegalArgumentException for a duplicate or conflicting subscription
//     */
//    public <T> TStream<T> subscribeBytes(Function<ByteConsumerRecord,T> toTupleFn, TopicPartition... topicPartitions) {
//        return t.events(new KafkaSubscriber<T>(connector, toTupleFn, false, topicPartitions));
//    }
//    
//    public <T> TStream<T> subscribe(Function<StringConsumerRecord,T> toTupleFn, TopicPartition... topicPartitions) {
//        return t.events(new KafkaSubscriber<T>(connector, toTupleFn, true, topicPartitions));
//    }
}
