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

import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * a publisher for RabbitMQ connector
 */
public class RabbitmqPublisher<T> implements Consumer<T>, AutoCloseable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RabbitmqPublisher.class);

    private final RabbitmqConnector connector;
    private final Function<T, byte[]> msgFn;
    private final String queue;
    private String id;

    public RabbitmqPublisher(RabbitmqConnector connector, String queue, Function<T, byte[]> msgFn) {
        this.connector = connector;
        this.queue = queue;
        this.msgFn = msgFn;
    }

    @Override
    public synchronized void close() throws Exception {
        logger.info("{} is closing.", id());
        connector.close();
        logger.info("{} is closed.", id());
    }

    @Override
    public void accept(T value) {
        byte[] msg = msgFn.apply(value);
        try {
            connector.channel().basicPublish("", queue, null, msg);
        } catch (IOException e) {
            logger.error("publish exception : {}", e);
            throw new RuntimeException(e);
        }
    }

    public String id() {
        if (id == null) {
            // include our short object Id
            id = connector.id() + " PUB " + toString().substring(toString().indexOf('@') + 1);
        }

        return id;
    }
}
