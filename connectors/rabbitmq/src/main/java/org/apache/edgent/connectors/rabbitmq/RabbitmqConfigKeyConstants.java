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

/**
 * Defines all RabbitMQ config key constant items.
 */
public class RabbitmqConfigKeyConstants {

    /**
     * config key for connection URI, eg: amqp://userName:password@hostName:portNumber/virtualHost
     */
    public static final String RABBITMQ_CONFIG_KEY_URI = "rabbitmq.connection.uri";

    /**
     * config key for RabbitMQ server host
     */
    public static final String RABBITMQ_CONFIG_KEY_HOST = "rabbitmq.connection.host";

    /**
     * config key for RabbitMQ server port, default port is : 5672
     */
    public static final String RABBITMQ_CONFIG_KEY_PORT = "rabbitmq.connection.port";

    /**
     * config key for virtual host which used to split multi-users
     */
    public static final String RABBITMQ_CONFIG_KEY_VIRTUAL_HOST = "rabbitmq.connection.virtualHost";

    /**
     * config key for authorization (user name)
     */
    public static final String RABBITMQ_CONFIG_KEY_AUTH_NAME = "rabbitmq.connection.authUsername";

    /**
     * config key for authorization (password)
     */
    public static final String RABBITMQ_CONFIG_KEY_AUTH_PASSWORD = "rabbitmq.connection.authPassword";

    /**
     * config key for specifying whether enable auto recovery or not.
     */
    public static final String RABBITMQ_CONFIG_KEY_AUTO_RECOVERY = "rabbitmq.connection.autoRecovery";

    /**
     * config key for connection timeout
     */
    public static final String RABBITMQ_CONFIG_KEY_TIMEOUT = "rabbitmq.connection.timeout";

    /**
     * config key for connection network recovery interval
     */
    public static final String RABBITMQ_CONFIG_KEY_NETWORK_RECOVERY_INTERVAL = "rabbitmq.connection.networkRecoveryInterval";

    /**
     * config key for connection requested-heartbeat
     */
    public static final String RABBITMQ_CONFIG_KEY_REQUESTED_HEARTBEAT = "rabbitmq.connection.requestedHeartbeat";

    /**
     * config key for specifying whether enable org.apache.edgent.org.apache.edgent.topology recovery or not.
     */
    public static final String RABBITMQ_CONFIG_KEY_TOPOLOGY_RECOVERY_ENABLED = "rabbitmq.connection.topologyRecoveryEnabled";

    /**
     * config key for connection requested channel max num
     */
    public static final String RABBITMQ_CONFIG_KEY_REQUESTED_CHANNEL_MAX = "rabbitmq.connection.requestedChannelMax";

    /**
     * config key for connection requested frame max
     */
    public static final String RABBITMQ_CONFIG_KEY_REQUESTED_FRAME_MAX = "rabbitmq.connection.requestedFrameMax";

}
