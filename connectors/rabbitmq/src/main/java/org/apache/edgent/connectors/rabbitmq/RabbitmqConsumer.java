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

package org.apache.edgent.connectors.rabbitmq;

import org.apache.edgent.connectors.rabbitmq.runtime.RabbitmqConnector;
import org.apache.edgent.connectors.rabbitmq.runtime.RabbitmqSubscriber;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import java.util.Map;

/**
 * {@code RabbitmqConsumer} is a consumer to consume messages from a RabbitMQ messaging broker
 * <p>
 * The connector uses and includes components from the RabbitMQ 3.7.3 release.
 * It has been successfully tested against 3.7.3.
 * For more information about RabbitMQ see <a href="http://www.rabbitmq.com/">http://www.rabbitmq.com/</a>
 * </p>
 * Smaple use:
 * <pre>{@code
 * Map<String, Object> config = new HashMap<>();
 * config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_HOST, "127.0.0.1");
 * config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_PORT, 5672);
 * String queue = "testQueue";
 *
 * Topology t = ...
 *
 * RabbitmqConsumer consumer = new RabbitmqConsumer(t, () -> configMap);
 * TStream<String> receivedStream = consumer.subscribe((byte[] bytes) -> new String(bytes), queue);
 *
 * //...
 * }
 * </pre>
 */
public class RabbitmqConsumer {

    private final RabbitmqConnector connector;
    private final Topology topology;

    /**
     * Create a consumer connector for consuming tuples from a RabbitMQ queue.
     * <p>
     * See the RabbitMQ java client document :
     * <a href="http://www.rabbitmq.com/api-guide.html">http://www.rabbitmq.com/api-guide.html</a>
     * The full config option please see RabbitMQ java client API Reference :
     * < a href="https://rabbitmq.github.io/rabbitmq-java-client/api/current/com/rabbitmq/client/ConnectionFactory.html">ConnectionFactory</>
     * </p>
     * @param topology org.apache.edgent.org.apache.edgent.topology to add to
     * @param config RabbitmqProducer configuration information.
     */
    public RabbitmqConsumer(Topology topology, Supplier<Map<String, Object>> config) {
        this.topology = topology;
        this.connector = new RabbitmqConnector(config);
    }

    /**
     * Subscribe to the specified topics and yield a stream of tuples from the published RabbitMQ records.
     *
     * @param toTupleFn A function that yields a tuple from a byte array,
     * @param queue the specified RabbitMQ queue
     * @param <T> A function that yields a tuple from a
     * @return stream of tuples
     */
    public <T> TStream<T> subscribe(Function<byte[], T> toTupleFn, String queue) {
        return topology.events(new RabbitmqSubscriber<>(connector, queue, toTupleFn));
    }

}
