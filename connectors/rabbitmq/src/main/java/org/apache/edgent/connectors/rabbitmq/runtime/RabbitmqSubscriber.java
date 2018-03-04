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

package org.apache.edgent.connectors.rabbitmq.runtime;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * a subscriber for RabbitMQ connector
 */
public class RabbitmqSubscriber<T> implements Consumer<Consumer<T>>, AutoCloseable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RabbitmqSubscriber.class);

    private final RabbitmqConnector connector;
    private Function<byte[], T> toTupleFn;
    private Consumer<T> eventSubmitter;
    private ExecutorService executor;
    private String queue;
    private String id;

    public RabbitmqSubscriber(RabbitmqConnector connector, String queue, Function<byte[], T> toTupleFn) {
        this.connector = connector;
        this.queue = queue;
        this.toTupleFn = toTupleFn;
    }

    @Override
    public synchronized void close() throws Exception {
        logger.info("{} is closing.", id());
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        connector.close();
        logger.info("{} is closed", id());
    }

    @Override
    public void accept(Consumer<T> eventSubmitter) {
        this.eventSubmitter = eventSubmitter;

        executor = Executors.newFixedThreadPool(1);

        executor.submit(() -> {
            boolean autoAck = false;
            try {
                connector.channel().basicConsume(queue, autoAck,
                    new DefaultConsumer(connector.channel()) {
                        @Override
                        public void handleDelivery(String consumerTag,
                                                   Envelope envelope,
                                                   AMQP.BasicProperties properties,
                                                   byte[] body)
                            throws IOException {
                            long deliveryTag = envelope.getDeliveryTag();

                            acceptCallback(body);

                            connector.channel().basicAck(deliveryTag, false);
                        }
                    });
            } catch (IOException e) {
                logger.error("Consumer exception : {}", e);
            }
        });

    }

    private void acceptCallback(byte[] msg) {
        T tuple = toTupleFn.apply(msg);
        eventSubmitter.accept(tuple);
    }

    public String id() {
        if (id == null) {
            // include our short object Id
            id = connector.id() + " SUB " + toString().substring(toString().indexOf('@') + 1);
        }

        return id;
    }

}
