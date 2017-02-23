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

import static org.apache.edgent.connectors.iotp.IotpGateway.ATTR_DEVICE_ID;
import static org.apache.edgent.connectors.iotp.IotpGateway.ATTR_DEVICE_TYPE;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.edgent.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;
import com.ibm.iotf.client.gateway.Command;
import com.ibm.iotf.client.gateway.GatewayCallback;
import com.ibm.iotf.client.gateway.GatewayClient;
import com.ibm.iotf.client.gateway.Notification;

/**
 * Gateway Device connector for IoTf.
 */
public class IotpGWConnector implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(IotpGWConnector.class);

    private Properties options;
    private File optionsFile;
    private transient GatewayClient client;
    private boolean disconnectOnClose = true;
    private boolean isInitialConnect = true;
    private GatewayCallback externalCallbackHandler;
    private String deviceType;    // for the gateway device
    private String deviceId;      // raw WIoTP deviceId for the gateway device
    private String fqDeviceId;    // for the gateway device

    /**
     * Create a new connector to the specified MQTT server.
     *
     * @param options connector options
     */
    public IotpGWConnector(Properties options) {
        this.options = options;
        init();
    }

    public IotpGWConnector(File optionsFile) {
        this.optionsFile = optionsFile;
        init();
    }

    public IotpGWConnector(GatewayClient iotpGatewayDeviceClient) {
        this.client = iotpGatewayDeviceClient;
        this.disconnectOnClose = false;
        init();
    }
    
    private void init() {
      try {
        GatewayClient client = getClient();
        this.deviceType = client.getGWDeviceType();
        this.deviceId = client.getGWDeviceId();
        this.fqDeviceId = toFqDeviceId(deviceType, deviceId);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to create GatewayClient", e);
      }
    }

    synchronized GatewayClient connect() {
        GatewayClient client;
        try {
            client = getClient();
            if (!client.isConnected()) {
                client.connect();
            }
            if (isInitialConnect) {
              // We need this for a passed in GatewayClient that was already
              // connected, not just when we initiate the connect...
              // 
              // GatewayClient pre-subscribes to cmds for the GW device
              // but not for its connected devices so do that now.
              //
              // N.B. in the face of overlapping subscriptions,
              // our GatewayCallback.processCommand(), established
              // by our subscribeCommands(), gets called multiple times - once for
              // each matching subscription.  They are separate Command instances
              // for the "duplicate" cmds so we can't filter them out.
              // The net result is that the same cmd gets added to a stream
              // multiple times.
              //
              // In combination with the GatewayClient's auto-subscription
              // of the GW device's cmds, our desire to receive cmds for
              // all of the GW's connected devices sets up this overlapping
              // subscriptions condition.  
              // i.e., simply adding a "all deviceType and all deviceIDs" subscription
              // results in duplicate GW device cmd callbacks/tuples. 
              //
              // Unsubscribe the GW device auto-subscription to avoid the dups.
              
              client.unsubscribeFromDeviceCommands(client.getGWDeviceType(), client.getGWDeviceId());
              client.subscribeToDeviceCommands("+", "+");
              
              isInitialConnect = false;
            }
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized GatewayClient getClient() throws Exception {
        if (client == null) {
            if (options == null)
                options = DeviceClient.parsePropertiesFile(optionsFile);

            client = new GatewayClient(options);
        }
        return client;
    }
    
    public synchronized GatewayCallback setExternalCallbackHandler(GatewayCallback handler) {
      GatewayCallback prev = externalCallbackHandler;
      externalCallbackHandler = handler;
      return prev;
    }
    
    synchronized void subscribeCommands(Consumer<Command> tupleSubmitter) throws Exception {
        GatewayClient client = getClient();
        
        // N.B. See commentary in connect() above re "dup cmds".

       client.setGatewayCallback(new GatewayCallback() {

          @Override
          public void processCommand(Command cmd) {
            if (externalCallbackHandler != null) {
              externalCallbackHandler.processCommand(cmd);
            }
            
            tupleSubmitter.accept(cmd);
          }

          @Override
          public void processNotification(Notification notification) {
            if (externalCallbackHandler != null) {
              externalCallbackHandler.processNotification(notification);
            }
            
            // Edgent doesn't currently handle notifications.
          }
          
        });
        
        connect();
    }

    void publishGWEvent(String eventId, JsonObject event, int qos) {
        GatewayClient client;
        try {
            client = connect();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        if (!client.publishGatewayEvent(eventId, event, qos)) {
          logger.error("Publish event failed for eventId {}", eventId);
        }
    }

    void publishDeviceEvent(String fqDeviceId, String eventId, JsonObject event, int qos) {
      String[] devIdToks = splitFqDeviceId(fqDeviceId);
      publishDeviceEvent(devIdToks[0], devIdToks[1], eventId, event, qos);
    }

    void publishDeviceEvent(String deviceType, String deviceId, String eventId, JsonObject event, int qos) {
        GatewayClient client;
        try {
            client = connect();
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        if (!client.publishDeviceEvent(deviceType, deviceId, eventId, event, qos)) {
          logger.error("Publish event failed for eventId {}", eventId);
        }
    }

//    void publishHttpDeviceEvent(String eventId, JsonObject event) {
//        try {
//            APIClient api = getClient().api();
//            if (!api.publishDeviceEventOverHTTP(eventId, event, ContentType.json)) {
//              logger.error("HTTP publish event failed for eventId {}", eventId);
//            }
//        } catch (Exception e) {
//            // throw new RuntimeException(e);
//            // If the publish throws, a RuntimeException will cause
//            // everything to unwind and the app/topology can terminate.
//            // See the commentary/impl of MqttPublisher.accept().
//            // See EDGENT-382
//            logger.error("Unable to publish event for eventId {}", eventId, e);
//        }
//    }

    @Override
    public void close() throws Exception {
        if (client == null)
            return;

        if (disconnectOnClose)
          client.disconnect();
        client = null;
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
