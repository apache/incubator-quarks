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
package org.apache.edgent.connectors.iot.aws.runtime;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class IotAwsGatewayConnector implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(IotAwsGatewayConnector.class);

    private static final String OPTION_CLIENT_ENDPOINT = "clientEndpoint";
    private static final String OPTION_CLIENT_ID = "clientId";
    private static final String OPTION_KEYSTORE_PASSWORD = "keystorePassword";
    private static final String OPTION_KEYSTORE_FILE = "keystoreFile";
    private static final String OPTION_KEYSTORE_TYPE = "keystoreType";
    private static final String OPTION_PRIVATE_KEY_PASSWORD = "privateKeyPassword";
    private static final String OPTION_AWS_ACCESS_KEY_ID = "awsAccessKeyId";
    private static final String OPTION_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey";
    private static final String OPTION_SESSION_TOKEN = "sessionToken";

    private Properties options;
    private String deviceType;    // for the gateway device
    private String deviceId;      // raw WIoTP deviceId for the gateway device
    private String fqDeviceId;    // for the gateway device

    private AWSIotMqttClient client;

    public IotAwsGatewayConnector(Properties options) {
        this.options = options;
        init();
    }

    public IotAwsGatewayConnector(File optionsFile) {
        try {
            this.options = new Properties();
            options.load(new InputStreamReader(new FileInputStream(optionsFile)));
            init();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create GatewayClient", e);
        }
    }

    private void init() {
        try {
            AWSIotMqttClient client = getClient();
            this.deviceType = client.getGWDeviceType();
            this.deviceId = client.getGWDeviceId();
            this.fqDeviceId = toFqDeviceId(deviceType, deviceId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create GatewayClient", e);
        }
    }

    synchronized AWSIotMqttClient getClient() throws Exception {
        if (client == null) {
            // Initialize a client instance depending on the properties provided.
            String clientEndpoint = (String) options.get(OPTION_CLIENT_ENDPOINT);
            String clientId = (String) options.get(OPTION_CLIENT_ID);

            // Configure an MQTT over TLS connection
            if(options.containsKey(OPTION_PRIVATE_KEY_PASSWORD) && options.containsKey(OPTION_KEYSTORE_FILE)) {
                String keystoreType = KeyStore.getDefaultType();
                if(options.containsKey(OPTION_KEYSTORE_TYPE)) {
                    keystoreType = options.getProperty(OPTION_KEYSTORE_TYPE);
                }
                String keystorePassword = (String) options.get(OPTION_KEYSTORE_PASSWORD);
                FileInputStream keystoreFile = new FileInputStream(options.getProperty(OPTION_KEYSTORE_FILE));

                // Initialize the keystore
                KeyStore keystore = KeyStore.getInstance(keystoreType);
                keystore.load(keystoreFile, (keystorePassword != null) ? keystorePassword.toCharArray() : null);

                String privateKeyPassword = (String) options.get(OPTION_PRIVATE_KEY_PASSWORD);
                client = new AWSIotMqttClient(clientEndpoint, clientId, keystore, privateKeyPassword);
            }

            // Configure an MQTT over Websocket connection.
            else if(options.containsKey(OPTION_AWS_ACCESS_KEY_ID) &&
                    options.containsKey(OPTION_AWS_SECRET_ACCESS_KEY)) {
                String awsAccessKeyId = (String) options.get(OPTION_AWS_ACCESS_KEY_ID);
                String awsSecretAccessKey = (String) options.get(OPTION_AWS_SECRET_ACCESS_KEY);
                String sessionToken = (String) options.get(OPTION_SESSION_TOKEN);
                client = new AWSIotMqttClient(
                    clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken);
            }

            // Well if it's not one of these, give up.
            else {
                throw new IllegalArgumentException("Unable to create GatewayClient. Missing properties.");
            }
        }
        return client;
    }

    synchronized AWSIotMqttClient connect() {
        AWSIotMqttClient client;
        try {
            client = getClient();
            if (client.getConnectionStatus() == AWSIotConnectionStatus.DISCONNECTED) {
                client.connect();
            }
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (client == null) {
            return;
        }

        client.disconnect();
        client = null;
    }

    void publishGWEvent(String eventId, JsonObject event, int qos) {
        AWSIotMqttClient client;
        try {
            client = connect();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        AWSIotMessage
        if (!client.publishGatewayEvent(eventId, event, qos)) {
            logger.error("Publish event failed for eventId {}", eventId);
        }
    }

    void publishDeviceEvent(String fqDeviceId, String eventId, JsonObject event, int qos) {
        String[] devIdToks = splitFqDeviceId(fqDeviceId);
        publishDeviceEvent(devIdToks[0], devIdToks[1], eventId, event, qos);
    }

    void publishDeviceEvent(String deviceType, String deviceId, String eventId, JsonObject event, int qos) {
        AWSIotMqttClient client;
        try {
            client = connect();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        if (!client.publishDeviceEvent(deviceType, deviceId, eventId, event, qos)) {
            logger.error("Publish event failed for eventId {}", eventId);
        }
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getFqDeviceId() {
        return fqDeviceId;
    }

    public String getIotDeviceId(Map<String, String> deviceIdAttrs) {
        Objects.requireNonNull(deviceIdAttrs.get(ATTR_DEVICE_TYPE), ATTR_DEVICE_TYPE);
        Objects.requireNonNull(deviceIdAttrs.get(ATTR_DEVICE_ID), ATTR_DEVICE_ID);

        return toFqDeviceId(deviceIdAttrs.get(ATTR_DEVICE_TYPE), deviceIdAttrs.get(ATTR_DEVICE_ID));
    }

    public static String toFqDeviceId(String deviceType, String deviceId) {
        return String.format("D/%s/%s", deviceType, deviceId);
    }

    public static String[] splitFqDeviceId(String fqDeviceId) {
        String[] tokens = fqDeviceId.split("/");
        if (tokens.length != 3 || !tokens[0].equals("D")) {
            throw new IllegalArgumentException("bad fqDeviceId " + fqDeviceId);
        }
        return new String[] { tokens[1], tokens[2] };
    }

}
