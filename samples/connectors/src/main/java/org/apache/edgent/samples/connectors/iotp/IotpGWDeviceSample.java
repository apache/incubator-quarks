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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.IotpGateway;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.gateway.GatewayClient;
import com.ibm.iotf.devicemgmt.DeviceData;
import com.ibm.iotf.devicemgmt.gateway.ManagedGateway;

/**
 * Similar to IotpDeviceSample but behaving as a registered IoT Gateway device.
 * <P>
 * Use {@link IotpAppClient} to print published events and generate a command
 * (start this app before running IotpAppClient with the "useGW" option). 
 * <P>
 * This sample demonstrates:
 * <UL>
 * <LI>Using the IotpGateway connector</LI>
 * <LI>Initializing the IotpGateway connector using the WIoTP API objects</LI>
 * <LI>Publishing and subscribing to Gateway device events and commands</LI>
 * <LI>Publishing and subscribing to connected device events and commands</LI>
 * </UL>
 * <p>
 * This connects to your IBM Watson IoT Platform service
 * as the Gateway defined in a gateway config file.
 * The file format is the standard one for IBM Watson IoT Platform.
 *
 * <p>See {@code scripts/connectors/iotp/README} for information about a
 * prototype gateway device configuration file and running the sample.
 */
public class IotpGWDeviceSample {
  
    private static final String usage = "[useGatewayClient|useManagedGateway] [useHttp] <device-cfg-path>";

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
          throw new Exception("Usage: " + usage);
        List<String> argList = Arrays.asList(args);
        boolean useGatewayClient = argList.contains("useGatewayClient");
        boolean useManagedGateway = argList.contains("useManagedGateway");
        boolean useInternalGatewayClient = !(useGatewayClient || useManagedGateway);
        boolean useHttp = argList.contains("useHttp");
        String deviceCfgPath = argList.get(argList.size() - 1);

        DirectProvider tp = new DirectProvider();
        Topology topology = tp.newTopology("IotpGWDeviceSample");
        
        Properties cfgProps = new Properties();
        cfgProps.load(new FileReader(new File(deviceCfgPath)));
        
        String iotpOrg = getProperty(cfgProps, "Organization-ID", "org");
        String iotpGWDevType = getProperty(cfgProps, "Gateway-Type", "Device-Type", "type");
        String iotpGWDevId = getProperty(cfgProps, "Gateway-ID", "Device-ID", "id");
        String iotpCnDev1Type = cfgProps.getProperty("cn-dev1-type");
        String iotpCnDev1Id = cfgProps.getProperty("cn-dev1-id");
        
        System.out.println("orgId:  " + iotpOrg);
        System.out.println("GWDeviceType: " + iotpGWDevType);
        System.out.println("GWDeviceId:   " + iotpGWDevId);
        System.out.println("cn-dev1 DeviceType: " + iotpCnDev1Type);
        System.out.println("cn-dev1 DeviceId:   " + iotpCnDev1Id);
        
        // System.out.println("GW mosquitto_pub -u <api-auth-key> -P <api-auth-token> -h "+iotpOrg+".messaging.internetofthings.ibmcloud.com -p 1883 -i a:"+iotpOrg+":appId1 -t iot-2/type/"+iotpGWDevType+"/id/"+iotpGWDevId+"/cmd/cmd-1/fmt/json -m '{}'");
        // System.out.println("GW mosquitto_sub -d -u <api-auth-key> -P <api-auth-token> -h "+iotpOrg+".messaging.internetofthings.ibmcloud.com -p 1883 -i a:"+iotpOrg+":appId2 -t iot-2/type/+/id/+/evt/+/fmt/+");
        // System.out.println("cn-dev1 mosquitto_pub -u <api-auth-key> -P <api-quth-token> -h "+iotpOrg+".messaging.internetofthings.ibmcloud.com -p 1883 -i a:"+iotpOrg+":appId1 -t iot-2/type/"+iotpCnDev1Type+"/id/"+iotpCnDev1Id+"/cmd/cmd-1/fmt/json -m '{}'");

        IotpGateway gwDevice;
        if (useInternalGatewayClient) {
          System.out.println("Using internal GatewayClient");
          gwDevice = new IotpGateway(topology, cfgProps);
        }
        else if (useGatewayClient) {
          System.out.println("Using WIoTP GatewayClient");
          gwDevice = new IotpGateway(topology, new GatewayClient(cfgProps));
        }
        else if (useManagedGateway) {
          System.out.println("Using WIoTP ManagedGateway");
          DeviceData deviceData = new DeviceData.Builder().build();
          gwDevice = new IotpGateway(topology, new ManagedGateway(cfgProps, deviceData));
        }
        else
          throw new IllegalStateException("woops");

        Map<String,String> devAttrMap = new HashMap<>();
        devAttrMap.put(IotpGateway.ATTR_DEVICE_TYPE, iotpCnDev1Type);
        devAttrMap.put(IotpGateway.ATTR_DEVICE_ID, iotpCnDev1Id);
        
        String cnDev1FqDeviceId = gwDevice.getIotDeviceId(devAttrMap);
        IotDevice cnDev1Device = gwDevice.getIotDevice(cnDev1FqDeviceId);
        
        System.out.println("GW fqDeviceId: " + gwDevice.getDeviceId());
        System.out.println("cn-dev1 fqDeviceId:  " + cnDev1FqDeviceId);
        System.out.println("IotDevice cn-dev1 fqDeviceId:  " + cnDev1Device.getDeviceId());
             
        Random r = new Random();
        TStream<double[]> raw = topology.poll(() -> {
            double[]  v = new double[3];
            
            v[0] = r.nextGaussian() * 10.0 + 40.0;
            v[1] = r.nextGaussian() * 10.0 + 50.0;
            v[2] = r.nextGaussian() * 10.0 + 60.0;
            
            return v;
        }, 3, TimeUnit.SECONDS);
        
        // Create a stream of Gateway device events
        TStream<JsonObject> gwJson = raw.map(v -> {
          JsonObject jo2 = new JsonObject();
          jo2.addProperty("gw-fqDeviceId", gwDevice.getDeviceId());
          jo2.addProperty("temp", v[0]);
          return jo2;
        });
        
        // Create a stream of a connected device's events
        TStream<JsonObject> cnDev1Json = raw.map(v -> {
          JsonObject jo2 = new JsonObject();
          jo2.addProperty("cnDev1-fqDeviceId", cnDev1Device.getDeviceId());
          jo2.addProperty("humidity", v[1]);
          return jo2;
        });

        if (!useHttp) {
          gwDevice.events(gwJson, "gw-device", QoS.FIRE_AND_FORGET);
          gwDevice.eventsForDevice(cnDev1FqDeviceId, cnDev1Json, "gw-events-for-cnDev1", QoS.FIRE_AND_FORGET);
          cnDev1Device.events(cnDev1Json, "cnDev1-events", QoS.FIRE_AND_FORGET);
        }
        else {
          System.out.println("Publishing events using HTTP");
          throw new IllegalStateException("GW httpEvents is NYI");
          // gwDevice.httpEvents(json, "sensors");
          // gwDevice.httpEventsForDevice(cnDev1FqDeviceId, cnDev1Json, "gw-events-for-cnDev1");
        }

        // subscribe to / report cmds for the GW and all its connected devices
        gwDevice.commandsForDevice(Collections.emptySet()).sink(jo -> System.out.println("Received all-cmds cmd: " + jo));
        
        // subscribe to / report just GW device cmds
        gwDevice.commands().sink(jo -> System.out.println("Received gwDevice cmd: " + jo));
        
        // subscribe to / report just cnDev1 device cmds
        gwDevice.commandsForDevice(cnDev1FqDeviceId).sink(jo -> System.out.println("Received gwDevice-for-cnDev1 cmd: " + jo));
        cnDev1Device.commands().sink(jo -> System.out.println("Received cnDev1 cmd: " + jo));
        
        // subscribe to / report just cmds for a specific device type
        gwDevice.commandsForType(iotpGWDevType).sink(jo -> System.out.println("Received for-type-gwDeviceType cmd: " + jo));
        gwDevice.commandsForType(iotpCnDev1Type).sink(jo -> System.out.println("Received for-type-cnDev1DeviceType cmd: " + jo));

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
