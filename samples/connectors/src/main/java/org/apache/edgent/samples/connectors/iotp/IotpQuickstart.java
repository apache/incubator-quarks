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

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.IotpDevice;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;

/**
 * IBM Watson IoT Platform Quickstart sample.
 * Submits a JSON device event every second using the
 * same format as the Quickstart device simulator,
 * with keys {@code temp}, {@code humidity}  and {@code objectTemp}
 * and random values.
 * <P>
 * The device type is {@code iotsamples-edgent} and a random
 * device identifier is generated. Both are printed out when
 * the application starts.
 * </P>
 * A URL is also printed that allows viewing of the data
 * as it received by the Quickstart service.
 *
 * <p>See {@code scripts/connectors/iotp/README} for information about running the sample.
 */
public class IotpQuickstart {

    public static void main(String[] args) {

        DirectProvider tp = new DirectProvider();
        Topology topology = tp.newTopology("IotpQuickstart");
        
        // Declare a connection to IoTF Quickstart service
        String deviceId = "qs" + Long.toHexString(new Random().nextLong());
        IotDevice device = IotpDevice.quickstart(topology, deviceId);
        
        System.out.println("Quickstart device type:" + IotpDevice.QUICKSTART_DEVICE_TYPE);
        System.out.println("Quickstart device id  :" + deviceId);
        System.out.println("https://quickstart.internetofthings.ibmcloud.com/#/device/"
             + deviceId);
             
        Random r = new Random();
        TStream<double[]> raw = topology.poll(() -> {
            double[]  v = new double[3];
            
            v[0] = r.nextGaussian() * 10.0 + 40.0;
            v[1] = r.nextGaussian() * 10.0 + 50.0;
            v[2] = r.nextGaussian() * 10.0 + 60.0;
            
            return v;
        }, 1, TimeUnit.SECONDS);
        
        TStream<JsonObject> json = raw.map(v -> {
            JsonObject j = new JsonObject();
            j.addProperty("temp", v[0]);
            j.addProperty("humidity", v[1]);
            j.addProperty("objectTemp", v[2]);
            return j;
        });
        
        device.events(json, "sensors", QoS.FIRE_AND_FORGET);

        tp.submit(topology);
    }
}
