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
package org.apache.edgent.samples.connectors.iotp;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.app.ApplicationClient;
import com.ibm.iotf.client.app.Command;
import com.ibm.iotf.client.app.Event;
import com.ibm.iotf.client.app.EventCallback;

/**
 * An IBM Watson IoT Platform ApplicationClient that publishes device cmds 
 * and subscribes to device events for
 * {@link IotpDeviceSample} and {@link IotpGWDeviceSample}.
 * <p>
 * Usage: {@code [useGW] <app-cfg-path> # see scripts/connectors/iotp/iotp-app-client.cfg}
 * <p>
 * This connects to your IBM Watson IoT Platform service
 * as the Application defined in a application config file.
 * The file format is the standard one for IBM Watson IoT Platform.
 * <p>
 * Note, the config file also contains some additional information for this application.
 *
 * <p>See {@code scripts/connectors/iotp/README} for information about a
 * prototype application configuration file and running the application.
 */
public class IotpAppClient {
  
  private static final String usage = "[useGW] <app-cfg-path> # see scripts/connectors/iotp/iotp-app-client.cfg";
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0)
      throw new Exception("Usage: " + usage);
    List<String> argList = Arrays.asList(args);
    boolean useGW = argList.contains("useGW");
    String deviceCfgPath = argList.get(argList.size() - 1);

    Properties cfgProps = new Properties();
    cfgProps.load(new FileReader(new File(deviceCfgPath)));
    
    String iotpOrg = getProperty(cfgProps, "Organization-ID", "org");
    String iotpAppId = getProperty(cfgProps, "id");
    String iotpApiKey = getProperty(cfgProps, "API-Key", "auth-key");
    System.out.println("org:     " + iotpOrg);
    System.out.println("id:      " + iotpAppId);
    System.out.println("ApiKey:  " + iotpApiKey);

    String iotpDevType = cfgProps.getProperty("deviceType");
    String iotpDevId = cfgProps.getProperty("deviceId");
    if (useGW) {
      iotpDevType = cfgProps.getProperty("gwDeviceType");
      iotpDevId = cfgProps.getProperty("gwDeviceId");
    }
    System.out.println("deviceType: " + iotpDevType);
    System.out.println("deviceId:   " + iotpDevId);

    ApplicationClient client = new ApplicationClient(cfgProps);
    
    client.connect();
    
    boolean sendCmd = true;
    if (sendCmd) {
      sendCmd(client, iotpDevType, iotpDevId);
      if (useGW) {
        sendCmd(client, cfgProps.getProperty("cn-dev1-type"), cfgProps.getProperty("cn-dev1-id"));
      }
    }
    
    boolean subscribeToEvents = true;
    if (subscribeToEvents) {
      System.out.println("Subscribing to events...");
      client.subscribeToDeviceEvents();
      client.setEventCallback(new EventCallback() {

        @Override
        public void processCommand(Command cmd) {
          // TODO Auto-generated method stub
          
        }

        @SuppressWarnings("deprecation")
        @Override
        public void processEvent(Event event) {
          System.out.println(
              String.format("Received event: %s %s:%s %s %s", event.getEvent(),
                  event.getDeviceType(), event.getDeviceId(),
                  event.getFormat(),
                  event.getPayload()));
        }
        
      });
      Thread.sleep(Integer.MAX_VALUE);
    }
    
    client.disconnect();
  }
  
  private static int msgNum = 0;
  private static void sendCmd(ApplicationClient client, String iotpDevType, String iotpDevId) throws Exception {
    String command = "cmdId-1";
    JsonObject jo = new JsonObject();
    jo.addProperty("msgNum", ++msgNum);
    jo.addProperty("deviceTypeAndId", iotpDevType + "/" + iotpDevId);
    jo.addProperty("cmdId", command);
    jo.addProperty("str", "a-string");
    jo.addProperty("num", 12345);
    JsonObject data = jo;
    
    System.out.println("Sending "+iotpDevType+"/"+iotpDevId+" command: "+command+" data: "+data);
    
    boolean ok = client.publishCommand(iotpDevType, iotpDevId, command, data);
    
    System.out.println("Sent: " + (ok ? "OK" : "NOT-OK"));
  }
  
  private static String getProperty(Properties props, String... keys) {
    for (String key : keys) {
      String val = props.getProperty(key);
      if (val != null)
        return val;
    }
    return null;
  }

}
