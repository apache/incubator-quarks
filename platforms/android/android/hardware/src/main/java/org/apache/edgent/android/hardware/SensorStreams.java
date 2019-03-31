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
package org.apache.edgent.android.hardware;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import org.apache.edgent.android.hardware.runtime.SensorSourceSetup;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TopologyElement;

/**
 * Create streams from sensors.
 *
 */
public class SensorStreams {
    
    /**
     * Declare a stream of sensor events.
     * A listener is registered with {@code sensorManager}
     * using {@code SensorManager.SENSOR_DELAY_NORMAL}.
     * Each sensor event will result in a tuple on
     * the returned stream.
     *
     * @param te Topology element for stream's org.apache.edgent.org.apache.edgent.topology
     * @param sensorManager Sensor manager
     * @param sensorTypes Which sensors to listen for. 
     * @return Stream that will contain events from the sensors.
     */
    public static TStream<SensorEvent> sensors(TopologyElement te, SensorManager sensorManager, int ... sensorTypes) {
        Sensor[] sensors = new Sensor[sensorTypes.length];
        
        for (int i = 0; i < sensorTypes.length; i++)
            sensors[i] = sensorManager.getDefaultSensor(sensorTypes[i]);
        
        return sensors(te, sensorManager, sensors);
    }
    
    /**
     * Declare a stream of sensor events.
     * A listener is registered with {@code sensorManager}
     * using {@code SensorManager.SENSOR_DELAY_NORMAL}.
     * Each sensor event will result in a tuple on
     * the returned stream.
     *
     * @param te Topology element for stream's org.apache.edgent.org.apache.edgent.topology
     * @param sensorManager Sensor manager
     * @param sensors Which sensors to listen for. 
     * @return Stream that will contain events from the sensors.
     */
    public static TStream<SensorEvent> sensors(TopologyElement te, SensorManager sensorManager, Sensor ... sensors) {        
        return te.topology().events(
                new SensorSourceSetup(sensorManager, sensors));
    } 
}
