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

package org.apache.edgent.connectors.iotp.runtime;

import java.io.File;
import java.io.Serializable;
import java.util.Properties;

import org.apache.edgent.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.api.APIClient.ContentType;
import com.ibm.iotf.client.device.Command;
import com.ibm.iotf.client.device.DeviceClient;

/**
 * Device connector for IoTf.
 */
public class IotpConnector implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(IotpConnector.class);

    private Properties options;
    private File optionsFile;
    private transient DeviceClient client;
    private boolean disconnectOnClose = true;

    /**
     * Create a new connector to the specified MQTT server.
     *
     * @param options connector options
     */
    public IotpConnector(Properties options) {
        this.options = options;
    }

    public IotpConnector(File optionsFile) {
        this.optionsFile = optionsFile;
    }

    public IotpConnector(DeviceClient iotpDeviceClient) {
        this.client = iotpDeviceClient;
        this.disconnectOnClose = false;
    }

    synchronized DeviceClient connect() {
        DeviceClient client;
        try {
            client = getClient();
            if (!client.isConnected())
                client.connect();
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized DeviceClient getClient() throws Exception {
        if (client == null) {
            if (options == null)
                options = DeviceClient.parsePropertiesFile(optionsFile);

            client = new DeviceClient(options);
        }
        return client;
    }

    synchronized void subscribeCommands(Consumer<Command> tupleSubmitter) throws Exception {
        DeviceClient client = getClient();
        
        client.setCommandCallback(cmd -> {
            tupleSubmitter.accept(cmd);
        });
        
        connect();
    }

    void publishEvent(String eventId, JsonObject event, int qos) {
        DeviceClient client;
        try {
            client = connect();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        boolean success = client.publishEvent(eventId, event, qos);
        if (!success) {
          // TODO log
        }
    }

    void publishHttpEvent(String eventId, JsonObject event) {
        try {
            DeviceClient client = getClient();
            client.api().publishDeviceEventOverHTTP(eventId, event, ContentType.json);
        } catch (Exception e) {
            // throw new RuntimeException(e);
            // If the publish throws, a RuntimeException will cause
            // everything to unwind and the app/topology can terminate.
            // See the commentary/impl of MqttPublisher.accept().
            // See EDGENT-382
            logger.error("Unable to publish tuple for event " + eventId, e);
        }
    }

    @Override
    public void close() throws Exception {
        if (client == null)
            return;

        if (disconnectOnClose)
          client.disconnect();
        client = null;
    }
}
