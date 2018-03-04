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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.edgent.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_AUTH_NAME;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_AUTH_PASSWORD;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_AUTO_RECOVERY;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_HOST;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_NETWORK_RECOVERY_INTERVAL;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_PORT;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_REQUESTED_CHANNEL_MAX;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_REQUESTED_FRAME_MAX;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_REQUESTED_HEARTBEAT;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_TIMEOUT;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_TOPOLOGY_RECOVERY_ENABLED;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_URI;
import static org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_VIRTUAL_HOST;

/**
 * A connector to an RabbitMQ server.
 */
public class RabbitmqConnector implements AutoCloseable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RabbitmqConnector.class);

    private final Supplier<Map<String, Object>> configFn;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private String id;

    public RabbitmqConnector(Supplier<Map<String, Object>> configFn) {
        this.configFn = configFn;
        initConnection();
    }

    public synchronized Channel channel() {
        if (channel == null) {
            if (connection != null) {
                try {
                    channel = connection.createChannel();
                } catch (IOException e) {
                    logger.error("IOExcetion occurs when create connection channel {}", e);
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    logger.error("Unknown Exception : {}", e);
                }
            } else {
                logger.error("Inner statue inconformity ： the rabbitmq connection is null.");
                throw new RuntimeException("Inner statue inconformity ： the rabbitmq connection is null.");
            }
        }

        return channel;
    }

    @Override
    public synchronized void close() throws Exception {
        if (channel != null) {
            channel.close();
        }

        if (connection != null) {
            connection.close();
        }

        if (connectionFactory != null) {
            connectionFactory = null;
        }
    }

    public String id() {
        if (id == null) {
            // include our short object Id
            id = "RabbitMQ " + toString().substring(toString().indexOf('@') + 1);
        }
        return id;
    }

    private void initConnection() {
        try {
            this.connectionFactory = getConnectionFactory();
            this.connection = connectionFactory.newConnection();
        } catch (Exception e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
    }

    private ConnectionFactory getConnectionFactory() throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Map<String, Object> configMap = configFn.get();

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_URI)) {
            connectionFactory.setUri(configMap.get(RABBITMQ_CONFIG_KEY_URI).toString());
        } else {
            if (!configMap.containsKey(RABBITMQ_CONFIG_KEY_HOST)) {
                throw new RuntimeException("Missed key : " + RABBITMQ_CONFIG_KEY_HOST);
            }

            connectionFactory.setHost(configMap.get(RABBITMQ_CONFIG_KEY_HOST).toString());

            if (!configMap.containsKey(RABBITMQ_CONFIG_KEY_PORT)) {
                throw new RuntimeException("Missed key : " + RABBITMQ_CONFIG_KEY_PORT);
            }

            connectionFactory.setPort(Integer.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_PORT).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_VIRTUAL_HOST)) {
            connectionFactory.setVirtualHost(configMap.get(RABBITMQ_CONFIG_KEY_VIRTUAL_HOST).toString());
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_AUTH_NAME)) {
            connectionFactory.setUsername(configMap.get(RABBITMQ_CONFIG_KEY_AUTH_NAME).toString());
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_AUTH_PASSWORD)) {
            connectionFactory.setPassword(configMap.get(RABBITMQ_CONFIG_KEY_AUTH_PASSWORD).toString());
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_AUTO_RECOVERY)) {
            connectionFactory.setAutomaticRecoveryEnabled(
                Boolean.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_AUTO_RECOVERY).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_TIMEOUT)) {
            connectionFactory.setConnectionTimeout(Integer.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_TIMEOUT).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_NETWORK_RECOVERY_INTERVAL)) {
            connectionFactory.setNetworkRecoveryInterval(
                Integer.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_NETWORK_RECOVERY_INTERVAL).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_REQUESTED_HEARTBEAT)) {
            connectionFactory.setRequestedHeartbeat(
                Integer.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_REQUESTED_HEARTBEAT).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_TOPOLOGY_RECOVERY_ENABLED)) {
            connectionFactory.setTopologyRecoveryEnabled(
                Boolean.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_TOPOLOGY_RECOVERY_ENABLED).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_REQUESTED_CHANNEL_MAX)) {
            connectionFactory.setRequestedChannelMax(
                Integer.parseInt(configMap.get(RABBITMQ_CONFIG_KEY_REQUESTED_CHANNEL_MAX).toString()));
        }

        if (configMap.containsKey(RABBITMQ_CONFIG_KEY_REQUESTED_FRAME_MAX)) {
            connectionFactory.setRequestedChannelMax(
                Integer.valueOf(configMap.get(RABBITMQ_CONFIG_KEY_REQUESTED_FRAME_MAX).toString()));
        }

        return connectionFactory;
    }


}
