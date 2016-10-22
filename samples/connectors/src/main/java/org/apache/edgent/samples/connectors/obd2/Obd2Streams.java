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
package org.apache.edgent.samples.connectors.obd2;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.edgent.analytics.math3.stat.Regression.SLOPE;
import static org.apache.edgent.analytics.math3.stat.Statistic.MAX;
import static org.apache.edgent.samples.connectors.elm327.Cmd.PID;
import static org.apache.edgent.samples.connectors.elm327.Cmd.VALUE;
import static org.apache.edgent.samples.connectors.elm327.Pids01.AIR_INTAKE_TEMP;
import static org.apache.edgent.samples.connectors.elm327.Pids01.ENGINE_COOLANT_TEMP;
import static org.apache.edgent.samples.connectors.elm327.Pids01.RPM;
import static org.apache.edgent.samples.connectors.elm327.Pids01.SPEED;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.analytics.math3.json.JsonAnalytics;
import org.apache.edgent.connectors.serial.SerialDevice;
import org.apache.edgent.samples.connectors.elm327.Elm327Streams;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Sample OBD-II streams.
 *
 */
public class Obd2Streams {

    /**
     * Get a stream of temperature readings which
     * are increasing over the last minute.
     * 
     * Poll temperatures every five seconds and
     * calculate the maximum reading and rate of change
     * (slope) over the last minute, partitioned by parameter
     * {@link org.apache.edgent.samples.connectors.elm327.Cmd#PID pid}. Filter so that only
     * those with a rate of increase greater than
     * or equal to 1 degree C/minute is present on the returned stream.
     * 
     * Temperatures included are
     * {@link org.apache.edgent.samples.connectors.elm327.Pids01#AIR_INTAKE_TEMP AIR_INTAKE_TEMP} and
     * {@link org.apache.edgent.samples.connectors.elm327.Pids01#ENGINE_COOLANT_TEMP ENGINE_COOLANT_TEMP}.
     * 
     * @param device Serial device the ELM327 is connected to.
     * @return Stream that will contain parameters with increasing temperatures.
     */
    public static TStream<JsonObject> increasingTemps(SerialDevice device) {

        TStream<JsonArray> tempsA = Elm327Streams.poll(device, 5, SECONDS,
                AIR_INTAKE_TEMP,
                ENGINE_COOLANT_TEMP);

        TStream<JsonObject> temps = tempsA.flatMap(je -> je).map(je -> je.getAsJsonObject());

        TWindow<JsonObject, JsonElement> window = temps.last(1, MINUTES, j -> j.get(PID));

        TStream<JsonObject> temperatureRate = JsonAnalytics.aggregate(window, PID, VALUE, MAX, SLOPE);

        // Have the stream contain only tuples where
        // the rise in temperatures >= 1 degree C/minute
        temperatureRate = temperatureRate.filter(j -> {
            JsonObject v = getObject(j, "value");
            return v.has("SLOPE") && getDouble(v, "SLOPE") >= 1.0;
        });

        return temperatureRate;
    }
    
    /**
     * Get a stream containing vehicle speed (km/h)
     * and engine revs (rpm).
     * 
     * {@link org.apache.edgent.samples.connectors.elm327.Pids01#SPEED Speed}
     * and {@link org.apache.edgent.samples.connectors.elm327.Pids01#RPM engine revs}
     * are polled every 200ms and returned as a stream
     * containing JSON objects with keys {@code speed}
     * and {@code rpm}.
     * 
     * The two readings may not be exactly consistent with
     * each other as there are fetched sequentially from
     * the ELM327. 
     * 
     * @param device Serial device the ELM327 is connected to.
     * @return Stream that will contain speed and engine revolutions.
     */
    public static TStream<JsonObject> tach(SerialDevice device) {

        TStream<JsonArray> rpmSpeed = Elm327Streams.poll(device, 200, TimeUnit.MILLISECONDS,
                SPEED, RPM);

        TStream<JsonObject> tach = rpmSpeed.map(ja -> {
            JsonObject j = new JsonObject();
            
            double speed = getDouble(ja.get(0), VALUE);
            double rpm = getDouble(ja.get(1), VALUE);
            j.addProperty("speed", speed);
            j.addProperty("rpm", rpm);
                            
            return j;
        });

        return tach;
    }
    
    /**
     * Utility method to simplify accessing a JSON object.
     * @param json JSON object containing the object to be got.
     * @param key Key of the object to be got.
     * @return JSON object with key {@code key} from {@code json}.
     */
    public static JsonObject getObject(JsonObject json, String key) {
        return json.getAsJsonObject(key);
    }

    /**
     * Utility method to simplify accessing a number as a double.
     * @param json JSON object containing the number to be got.
     * @param key Key of the number to be got.
     * @return Number with key {@code key} from {@code json}.
     */
    public static double getDouble(JsonElement json, String key) {
        return json.getAsJsonObject().get(key).getAsDouble();
    }
}
