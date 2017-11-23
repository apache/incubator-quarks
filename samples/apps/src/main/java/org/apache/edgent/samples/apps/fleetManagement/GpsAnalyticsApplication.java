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
package org.apache.edgent.samples.apps.fleetManagement;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.samples.utils.sensor.GpsSensor;
import org.apache.edgent.samples.utils.sensor.SimulatedGpsSensor;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;

/**
 * GPS analytics
 * <p>
 * Source is a stream of GPS sensor data {@link GpsSensor}
 * <p>
 * Here's an outline of the topology
 * <ul>
 * <li>Log GPS coordinates by publishing to IotF. The data may be used by a
 * server application to display the vehicle on a map.</li>
 * <li>Filter to detect speeds above a threshold and publish alert IotF</li>
 * <li>Filter for GPS coordinates that are outside of a defined Geofence
 * boundary</li>
 * <li>Windowing to detect hard driving: hard braking or hard acceleration and
 * publish alert to IotF</li>
 * </ul>
 */
public class GpsAnalyticsApplication {

    private final FleetManagementAnalyticsClientApplication app;
    private final Topology topology;

    // TODO: make these configurable properties
    boolean trackGpsLocation = true;
    boolean trackSpeeding = true;
    boolean trackGeofence = true;
    boolean trackHardDriving = true;
    // Hard braking and acceleration thresholds may depend on the vehicle model
    double hardBrakingThreshold_MphPerSec = -8.25;
    double hardAccelerationThreshold_MphPerSec = 7.37;
    String driverId = "driver1";
    String VIN = "123456";
    double maxSpeed_Mph = 70;

    static double MILES_PER_HOUR_TO_METERS_PER_SEC = 0.44704;
    double METERS_PER_HOUR_TO_MILES_PER_SEC = 1 / MILES_PER_HOUR_TO_METERS_PER_SEC;
    // Convert 70 miles per hour to meters to sec
    double MAX_SPEED_METERS_PER_SEC = maxSpeed_Mph * MILES_PER_HOUR_TO_METERS_PER_SEC;
    static double MPS_TO_MPH = 3.6;

    public GpsAnalyticsApplication(Topology t, FleetManagementAnalyticsClientApplication app) {
        this.topology = t;
        this.app = app;
    }

    /**
     * Add the GPS sensor analytics to the topology.
     */
    public void addAnalytics() {

        // Generate source GPS data
        SimulatedGpsSensor g = new SimulatedGpsSensor();
        TStream<GpsSensor> gpsSensor = topology.poll(() -> g.nextGps(), 500, TimeUnit.MILLISECONDS);

        // Publish GPS data to IotF every 1 second
        if (trackGpsLocation) {
            TStream<GpsSensor> logGps = gpsSensor.peek(t -> System.out.println("log GPS: " + t.toString()));
            logGps.tag("logGps");
            // Publish GPS location to IotF
            app.iotDevice().events(JsonGps(logGps), "GPS: " + driverId, QoS.FIRE_AND_FORGET);
        }

        // Filter for actual speeding and publish to IoTF and local file
        if (trackSpeeding) {
            TStream<GpsSensor> speeding = gpsSensor.filter(t -> t.getSpeedMetersPerSec() > MAX_SPEED_METERS_PER_SEC);

            speeding.tag("speeding");
            // Count speeding tuples
            // TODO investigate why publish doesn't appear to work when a
            // counter is set
            // Metrics.counter(speeding);

            speeding.peek(t -> System.out.println("Alert: speeding - " + t.toString()));
            // Write speeding event to IotF
            app.iotDevice().events(JsonSpeed(speeding), "Speeding: " + driverId, QoS.FIRE_AND_FORGET);
        }

        // Filter for Geofence boundary exceptions and publish to IoTF
        if (trackGeofence) {
            TStream<GpsSensor> geofence = gpsSensor
                    .filter(t -> SimulatedGeofence.outsideGeofence(t.getLatitude(), t.getLongitude()));

            geofence.tag("geofence");
            // Count Geofence exceptions
            // TODO investigate why publish doesn't appear to work when a
            // counter is set
            // Metrics.counter(geofence);

            geofence.peek(t -> System.out.println("Alert: geofence - " + t.toString()));
            // Write Geofence exceptions to IotF
            app.iotDevice().events(JsonGeofence(geofence), "Geofence: " + driverId, QoS.FIRE_AND_FORGET);
        }

        /*
         * Hard braking: (speed1 - speed0)/(time1 - time0) <
         * hardBrakingThreshold_KphPerSec Hard acceleration: (speed1 -
         * speed0)/(time1 - time0) > hardAccelerationThreshold_KphPerSec 1 mps =
         * 3.6 kph
         */
        if (trackHardDriving) {
            TStream<GpsSensor> hardDriving = gpsSensor;
            // TODO replace hardcoded "2" in "last(2," to force seeing
            // hardDriving alter
            TWindow<GpsSensor, Object> window = hardDriving.last(2, tuple -> 0);
            TStream<GpsSensor[]> logHardDriving = window.batch((tuples, key) -> {
                GpsSensor[] results = null;
                Object[] tuplesArray = tuples.toArray();

                GpsSensor gps1 = (GpsSensor) tuplesArray[1];
                GpsSensor gps0 = (GpsSensor) tuplesArray[0];
                double speed1 = gps1.getSpeedMetersPerSec();
                double speed0 = gps0.getSpeedMetersPerSec();
                long time1 = gps1.getTime();
                long time0 = gps0.getTime();

                // Check for hard braking or hard acceleration
                // Avoid division by 0
                if (time1 - time0 != 0) {
                    double mphPerSec = (speed1 - speed0) / (time1 - time0) * MPS_TO_MPH;
                    if (mphPerSec < hardBrakingThreshold_MphPerSec || mphPerSec > hardAccelerationThreshold_MphPerSec) {
                        results = new GpsSensor[2];
                        results[0] = gps0;
                        results[1] = gps1;
                    }
                }
                return results;
            }).peek(t -> System.out.println("hardDriving: t0=" + t[0].toString() + " t[1]=" + t[1].toString()))
                    .tag("hardDriving");

            app.iotDevice().events(JsonHardDriving(logHardDriving), "hardDriving: " + driverId, QoS.FIRE_AND_FORGET);
        }
    }

    private TStream<JsonObject> JsonGps(TStream<GpsSensor> gpsSensor) {
        return gpsSensor.map(t -> {
            JsonObject j = new JsonObject();
            j.addProperty("lat", t.getLatitude());
            j.addProperty("long", t.getLongitude());
            j.addProperty("alt", t.geAltitude());
            j.addProperty("mph", t.getSpeedMetersPerSec() * METERS_PER_HOUR_TO_MILES_PER_SEC);
            j.addProperty("course", t.getCourse());
            j.addProperty("time", t.getTime());
            return j;
        });
    }

    private TStream<JsonObject> JsonSpeed(TStream<GpsSensor> gpsSensor) {
        return gpsSensor.map(t -> {
            JsonObject j = new JsonObject();
            j.addProperty("lat", t.getLatitude());
            j.addProperty("long", t.getLongitude());
            j.addProperty("mph", t.getSpeedMetersPerSec() * METERS_PER_HOUR_TO_MILES_PER_SEC);
            j.addProperty("time", t.getTime());
            return j;
        });
    }

    private TStream<JsonObject> JsonGeofence(TStream<GpsSensor> gpsSensor) {
        return gpsSensor.map(t -> {
            JsonObject j = new JsonObject();
            j.addProperty("lat", t.getLatitude());
            j.addProperty("long", t.getLongitude());
            j.addProperty("time", t.getTime());
            return j;
        });
    }

    private TStream<JsonObject> JsonHardDriving(TStream<GpsSensor[]> gpsSensors) {
        return gpsSensors.map(t -> {
            JsonObject j = new JsonObject();
            j.addProperty("lat1", t[0].getLatitude());
            j.addProperty("long1", t[0].getLongitude());
            j.addProperty("time1", t[0].getTime());
            j.addProperty("speed1", t[0].getSpeedMetersPerSec());
            j.addProperty("lat2", t[1].getLatitude());
            j.addProperty("long2", t[1].getLongitude());
            j.addProperty("time2", t[1].getTime());
            j.addProperty("speed2", t[1].getSpeedMetersPerSec());
            j.addProperty("mphPerSec", (t[1].getSpeedMetersPerSec() - t[0].getSpeedMetersPerSec()) * MPS_TO_MPH
                    / (t[1].getTime() - t[0].getTime()));
            return j;
        });
    }
}
