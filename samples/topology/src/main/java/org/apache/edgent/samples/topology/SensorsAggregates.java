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

package org.apache.edgent.samples.topology;

import static org.apache.edgent.analytics.math3.stat.Statistic.MAX;
import static org.apache.edgent.analytics.math3.stat.Statistic.MEAN;
import static org.apache.edgent.analytics.math3.stat.Statistic.MIN;
import static org.apache.edgent.analytics.math3.stat.Statistic.STDDEV;

import org.apache.edgent.analytics.math3.json.JsonAnalytics;
import org.apache.edgent.console.server.HttpServer;
import org.apache.edgent.providers.development.DevelopmentProvider;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.samples.utils.sensor.SimulatedSensors;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Aggregation of sensor readings.
 * 
 * Demonstrates partitioned aggregation and filtering of simulated sensors
 * that are bursty in nature, so that only intermittently
 * is the data output to {@code System.out}.
 * <P>
 * The two sensors are read as independent streams but combined
 * into a single stream and then aggregated across the last 50 readings
 * using windows. The window is partitioned by the sensor name
 * so that each sensor will have its own independent window.
 * This partitioning is automatic so that the same code would
 * work if readings from one hundred different sensors were
 * on the same stream, is it just driven by a key function.
 * <BR>
 * The windows are then aggregated using Apache Common Math
 * provided statistics and the final stream filtered so
 * that it will only contain values when each sensor 
 * is (independently) out of range.
 * </P>
 *
 * @see SimulatedSensors#burstySensor(Topology, String)
 */
public class SensorsAggregates {
	
    /**
     * Run a topology with two bursty sensors printing them to standard out.
     * @param args command arguments
     * @throws Exception on failure
     */
    public static void main(String[] args) throws Exception {
    	
    	System.out.println("SensorsAggregates: Output will be randomly intermittent, be patient!");

        DirectProvider tp = new DevelopmentProvider();
        
        Topology topology = tp.newTopology("SensorsReadingAggregates");
        
        TStream<JsonObject> sensors = sensorsAB(topology);
        
        sensors.print();

        System.out.println("#### Console URL for the job: "
            + tp.getServices().getService(HttpServer.class).getConsoleUrl());

        tp.submit(topology);
    }
    
    /**
     * Create a stream containing two aggregates from two bursty
     * sensors A and B that only produces output when the sensors
     * (independently) are having a burst period out of their normal range.
     * @param topology Topology to add the sub-graph to.
     * @return Stream containing two aggregates from two bursty
     * sensors A and B
     */
    public static TStream<JsonObject> sensorsAB(Topology topology) {
    	
    	// Simulate two sensors, A and B, both randomly bursty
        TStream<JsonObject> sensorA = SimulatedSensors.burstySensor(topology, "A");
        TStream<JsonObject> sensorB = SimulatedSensors.burstySensor(topology, "B");
        
        // Combine the sensor readings into a single stream
        TStream<JsonObject> sensors = sensorA.union(sensorB);
        
        // Create a window on the stream of the last 50 readings partitioned
        // by sensor name. In this case two independent windows are created (for a and b)
        TWindow<JsonObject,JsonElement> sensorWindow = sensors.last(50, j -> j.get("name"));
        
        // Aggregate the windows calculating the min, max, mean and standard deviation
        // across each window independently.
        sensors = JsonAnalytics.aggregate(sensorWindow, "name", "reading", MIN, MAX, MEAN, STDDEV);
        
        // Filter so that only when the sensor is beyond 2.0 (absolute) is a reading sent.
        sensors = sensors.filter(j -> Math.abs(j.get("reading").getAsJsonObject().get("MEAN").getAsDouble()) > 2.0);
        
        return sensors;

    }
}
