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
package org.apache.edgent.samples.scenarios.iotp.range.sensor;

import static org.apache.edgent.analytics.math3.stat.Statistic.MAX;
import static org.apache.edgent.analytics.math3.stat.Statistic.MEAN;
import static org.apache.edgent.analytics.math3.stat.Statistic.MIN;
import static org.apache.edgent.analytics.math3.stat.Statistic.STDDEV;

import java.io.File;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.analytics.math3.json.JsonAnalytics;
import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.IotpDevice;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.providers.direct.DirectTopology;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

public class IotpRangeSensor {
    private static final Pin echoPin = RaspiPin.GPIO_05; // PI4J custom
                                                         // numbering (pin 18 on
                                                         // RPi2)
    private static final Pin trigPin = RaspiPin.GPIO_04; // PI4J custom
                                                         // numbering (pin 16 on
                                                         // RPi2)
    private static final Pin ledPin = RaspiPin.GPIO_01; // PI4J custom numbering
                                                        // (pin 12 on RPi2)

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Proper Usage is:\n   " + "   java program device.cfg sensorIsSimulated LEDIsSimulated\n"
                    + "Example: \n"
                    + "   java -cp $EDGENT/build/distributions/java8/samples/lib/edgent.samples.scenarios.jar org.apache.edgent.samples.scenarios.iotp.range.sensor.IotpRangeSensor device.cfg false true");
            System.exit(0);
        }

        String deviceCfg = args[0];
        Boolean simulatedRange = Boolean.parseBoolean(args[1]);
        Boolean simulatedLED = Boolean.parseBoolean(args[2]);

        DirectProvider tp = new DirectProvider();
        DirectTopology topology = tp.newTopology("IotpRangeSensor");

        IotDevice device = getIotDevice(deviceCfg, topology);

        // HC-SR04 Range sensor for this device.
        rangeSensor(device, simulatedRange, true);

        // In addition create a heart beat event to
        // ensure there is some immediate output and
        // the connection to IoTF happens as soon as possible.
        TStream<Date> hb = topology.poll(() -> new Date(), 1, TimeUnit.MINUTES);

        // Convert to JSON
        TStream<JsonObject> hbj = hb.map(d -> {
            JsonObject j = new JsonObject();
            j.addProperty("when", d.toString());
            j.addProperty("hearbeat", d.getTime());
            return j;
        });
        hbj.print();
        device.events(hbj, "heartbeat", QoS.FIRE_AND_FORGET);

        // Subscribe to commands of id "display" for this
        // device and print them to standard out
        TStream<String> statusMsgs = displayMessages(device);
        statusMsgs.print();

        // Flash an LED for 1second when we receive commands from IoTF
        if (!simulatedLED) {
            LED led = new LED(ledPin);
            statusMsgs.sink(j -> led.flash(1000));
        } else {
            statusMsgs.sink(j -> System.out.println("*******Simulated LED Flash!*******"));
        }

        tp.submit(topology);
    }

    /*
     * Returns an IotDevice based on the device config parameter. If the type is
     * "quickstart" then we also output the URL to view the data.
     */
    private static IotDevice getIotDevice(String deviceCfg, DirectTopology topology) {
        IotDevice device;

        if (deviceCfg.equalsIgnoreCase("quickstart")) {
            // Declare a connection to IoTF Quickstart service
            String deviceId = "qs" + Long.toHexString(new Random().nextLong());
            device = IotpDevice.quickstart(topology, deviceId);

            System.out.println("Quickstart device type:" + IotpDevice.QUICKSTART_DEVICE_TYPE);
            System.out.println("Quickstart device id  :" + deviceId);
            System.out.println("https://quickstart.internetofthings.ibmcloud.com/#/device/" + deviceId);
        } else {
            // Declare a connection to IoTF
            device = new IotpDevice(topology, new File(deviceCfg));
        }

        return device;
    }

    /**
     * Connect to an HC-SR04 Range Sensor
     * 
     * @param device
     *            IoTF device
     * @param print
     *            True if the data submitted as events should also be printed to
     *            standard out.
     * @param simulated
     *            boolean flag
     */
    public static void rangeSensor(IotDevice device, boolean simulated, boolean print) {

        Supplier<Double> sensor;

        if (simulated) {
            sensor = new SimulatedRangeSensor();
        } else {
            sensor = new RangeSensor(echoPin, trigPin);
        }

        TStream<Double> distanceReadings = device.topology().poll(sensor, 1, TimeUnit.SECONDS);
        distanceReadings.print();

        // filter out bad readings that are out of the sensor's 4m range
        distanceReadings = distanceReadings.filter(j -> j < 400.0);

        TStream<JsonObject> sensorJSON = distanceReadings.map(v -> {
            JsonObject j = new JsonObject();
            j.addProperty("name", "rangeSensor");
            j.addProperty("reading", v);
            return j;
        });

        // Create a window on the stream of the last 10 readings partitioned
        // by sensor name. In this case we only have one range sensor so there
        // will be one partition.
        TWindow<JsonObject, JsonElement> sensorWindow = sensorJSON.last(10, j -> j.get("name"));

        // Aggregate the windows calculating the min, max, mean and standard
        // deviation
        // across each window independently.
        sensorJSON = JsonAnalytics.aggregate(sensorWindow, "name", "reading", MIN, MAX, MEAN, STDDEV);

        // Filter so that only when the mean sensor reading is that an object is
        // closer than 30cm send data.
        sensorJSON = sensorJSON
                .filter(j -> Math.abs(j.get("reading").getAsJsonObject().get("MEAN").getAsDouble()) < 30.0);

        if (print)
            sensorJSON.print();

        // Send the device streams as IoTF device events
        // with event identifier "sensors".
        device.events(sensorJSON, "sensors", QoS.FIRE_AND_FORGET);
    }

    /**
     * Subscribe to IoTF device commands with identifier {@code display}.
     * Subscribing to device commands returns a stream of JSON objects that
     * include a timestamp ({@code tsms}), command identifier ({@code command})
     * and payload ({@code payload}). Payload is the application specific
     * portion of the command. <BR>
     * In this case the payload is expected to be a JSON object containing a
     * {@code msg} key with a string display message. <BR>
     * The returned stream consists of the display message string extracted from
     * the JSON payload.
     * <P>
     * Note to receive commands a analytic application must exist that generates
     * them through IBM Watson IoT Platform.
     * </P>
     *
     * @param device IoTF device
     * @return JSON object includes tsms(timestamp) and payload.msg(status)
     *
     * @see IotDevice#commands(String...)
     */
    public static TStream<String> displayMessages(IotDevice device) {
        // Subscribe to commands of id "status" for this device
        TStream<JsonObject> statusMsgs = device.commands("display");

        // The returned JSON object includes several fields
        // tsms - Timestamp in milliseconds (this is generic to a command)
        // payload.msg - Status message (this is specific to this application)

        // Map to a String object containing the message
        return statusMsgs.map(j -> j.getAsJsonObject("payload").getAsJsonPrimitive("msg").getAsString());
    }
}
