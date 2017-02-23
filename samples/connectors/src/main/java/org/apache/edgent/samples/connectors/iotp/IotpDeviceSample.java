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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.IotpDevice;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;
import com.ibm.iotf.devicemgmt.DeviceData;
import com.ibm.iotf.devicemgmt.device.ManagedDevice;

/**
 * Similar to IotpQuickstart2 but for a real/non-quickstart WIoTP account
 * for a registered device
 * AND it subscribes to/prints device cmds.
 * <P>
 * Use IotpAppClient or any other technique to generate cmds. 
 * e.g., mosquitto_{pub,sub} cmds are printed below.
 * <P>
 * This sample demonstrates:
 * <UL>
 * <LI>Using the IotpDevice connector</LI>
 * <LI>Initializing the IotpDevice connector using the WIoTP API objects</LI>
 * <LI>Publishing and subscribing to device events and commands</LI>
 * </UL>
 * <p>
 * This connects to your IBM Watson IoT Platform service
 * as the Device defined in a device config file.
 * The file format is the standard one for IBM Watson IoT Platform.
 * <p>
 * Note, the config file also contains some additional information for this application.
 * A sample iot-device-sample.cfg is in the scripts/connectors/iotp directory.
 */
public class IotpDeviceSample {
    private static final String usage = "[useDeviceClient|useManagedDevice] [useHttp] <device-cfg-path>";

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
          throw new Exception("Usage: " + usage);
        List<String> argList = Arrays.asList(args);
        boolean useDeviceClient = argList.contains("useDeviceClient");
        boolean useManagedDevice = argList.contains("useManagedDevice");
        boolean useInternalDeviceClient = !(useDeviceClient || useManagedDevice);
        boolean useHttp = argList.contains("useHttp");
        String deviceCfgPath = argList.get(argList.size() - 1);

        DirectProvider tp = new DirectProvider();
        Topology topology = tp.newTopology("IotpDeviceSample");
        
        Properties cfgProps = new Properties();
        cfgProps.load(new FileReader(new File(deviceCfgPath)));
        
        String iotpOrg = getProperty(cfgProps, "Organization-ID", "org");
        String iotpDevType = getProperty(cfgProps, "Device-Type", "type");
        String iotpDevId = getProperty(cfgProps, "Device-ID", "id");
        System.out.println("org:  " + iotpOrg);
        System.out.println("DeviceType: " + iotpDevType);
        System.out.println("DeviceId:   " + iotpDevId);
        
        System.out.println("device clientId:  " + "d:"+iotpOrg+":"+iotpDevType+":"+iotpDevId);
        System.out.println("WIoTP host: " + iotpOrg+".messaging.internetofthings.ibmcloud.com");
        System.out.println("evt topic: " + "iot-2/type/"+iotpDevType+"/id/"+iotpDevId+"/evt/+/fmt/json");
        System.out.println("cmd topic: " + "iot-2/type/"+iotpDevType+"/id/"+iotpDevId+"/cmd/+/fmt/json");
        System.out.println("mosquitto_pub -u <api-auth-key> -P <api-quth-token> -h "+iotpOrg+".messaging.internetofthings.ibmcloud.com -p 1883 -i a:"+iotpOrg+":appId1 -t iot-2/type/"+iotpDevType+"/id/"+iotpDevId+"/cmd/cmd-1/fmt/json -m '{}'");
        System.out.println("mosquitto_sub -d -u <api-auth-key> -P <api-quth-token> -h "+iotpOrg+".messaging.internetofthings.ibmcloud.com -p 1883 -i a:"+iotpOrg+":appId2 -t iot-2/type/+/id/+/evt/+/fmt/+");
        
        IotpDevice device;
        if (useInternalDeviceClient) {
          System.out.println("Using internal DeviceClient");
          device = new IotpDevice(topology, cfgProps);
        }
        else if (useDeviceClient) {
          System.out.println("Using WIoTP DeviceClient");
          device = new IotpDevice(topology, new DeviceClient(cfgProps));
        }
        else if (useManagedDevice) {
          System.out.println("Using WIoTP ManagedDevice");
          DeviceData deviceData = new DeviceData.Builder().build();
          device = new IotpDevice(topology, new ManagedDevice(cfgProps, deviceData));
        }
        else
          throw new Exception("woops");
             
        Random r = new Random();
        TStream<double[]> raw = topology.poll(() -> {
            double[]  v = new double[3];
            
            v[0] = r.nextGaussian() * 10.0 + 40.0;
            v[1] = r.nextGaussian() * 10.0 + 50.0;
            v[2] = r.nextGaussian() * 10.0 + 60.0;
            
            return v;
        }, 3, TimeUnit.SECONDS);
        
        TStream<JsonObject> json = raw.map(v -> {
            JsonObject j = new JsonObject();
            j.addProperty("temp", v[0]);
            j.addProperty("humidity", v[1]);
            j.addProperty("objectTemp", v[2]);
            return j;
        });
        
        if (!useHttp) {
          device.events(json, "sensors", QoS.FIRE_AND_FORGET);
        }
        else {
          System.out.println("Publishing events using HTTP");
          device.httpEvents(json, "sensors");
        }
        
        device.commands().sink(jo -> System.out.println("Received cmd: " + jo));

        tp.submit(topology);
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
