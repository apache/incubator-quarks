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
import org.apache.edgent.connectors.rabbitmq.runtime.RabbitmqPublisher;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import java.util.Map;

/**
 * {@code RabbitmqProducer} is a producer to produce messages to a RabbitMQ messaging broker
 * <p>
 * The connector uses and includes components from the RabbitMQ 3.7.3 release.
 * It has been successfully tested against 3.7.3.
 * For more information about RabbitMQ see <a href="http://www.rabbitmq.com/">http://www.rabbitmq.com/</a>
 * </p>
 * Sample use:
 * <pre>{@code
 * Map<String, Object> config = new HashMap<>();
 * config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_HOST, "127.0.0.1");
 * config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_PORT, 5672);
 * String queue = "testQueue";
 *
 * Topology t = newTopology("testSimple");
 * RabbitmqProducer producer = new RabbitmqProducer(t, () -> config);
 *
 * //TStream<String> stream = ...
 *
 * TSink<String> sink = producer.publish(stream, queue, (String s) -> s.getBytes());
 * }
 * </pre>
 */
public class RabbitmqProducer {

    private final RabbitmqConnector connector;
    private final Topology topology;

    /**
     * Create a producer connector for publishing tuples to a RabbitMQ queue.
     * <p>
     * See the RabbitMQ java client document :
     * <a href="http://www.rabbitmq.com/api-guide.html">http://www.rabbitmq.com/api-guide.html</a>
     * The full config option please see RabbitMQ java client API Reference :
     * < a href="https://rabbitmq.github.io/rabbitmq-java-client/api/current/com/rabbitmq/client/ConnectionFactory.html">ConnectionFactory</>
     * </p>
     * @param topology topology to add to
     * @param config RabbitmqProducer configuration information.
     */
    public RabbitmqProducer(Topology topology, Supplier<Map<String, Object>> config) {
        this.topology = topology;
        this.connector = new RabbitmqConnector(config);
    }

    /**
     * Publish the stream of tuples to the specified queue.
     * @param stream The stream to publish
     * @param queue The specified queue of RabbitMQ
     * @param msgFn A function that yields the byte[] records from the tuple
     * @param <T> Tuple type
     * @return {@link TSink}
     */
    public <T> TSink<T> publish(TStream<T> stream, String queue, Function<T, byte[]> msgFn) {
        return stream.sink(new RabbitmqPublisher<>(connector, queue, msgFn));
    }

    /**
     * Publish the stream of tuples to the specified queue.
     * @param stream The stream to publish
     * @param queue The specified queue of RabbitMQ
     * @param msg The string message to publish
     * @return
     */
    public TSink<String> publish(TStream<String> stream, String queue, String msg) {
        return publish(stream, queue, (String m) -> msg.getBytes());
    }

}
