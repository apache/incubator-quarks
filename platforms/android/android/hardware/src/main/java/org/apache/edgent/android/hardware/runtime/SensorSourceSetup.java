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
package org.apache.edgent.android.hardware.runtime;

import org.apache.edgent.function.Consumer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * 
 */
public class SensorSourceSetup implements Consumer<Consumer<SensorEvent>> {
    private static final long serialVersionUID = 1L;
    
    private final SensorManager mSensorManager;
    private final Sensor[] sensors;
    private final int samplingPeriodUs;
    private SensorChangeEvents events;
    

    public SensorSourceSetup(SensorManager mSensorManager, int samplingPeriodUs,
            Sensor ... sensors) {
        this.mSensorManager = mSensorManager;
        this.sensors = sensors;
        this.samplingPeriodUs = samplingPeriodUs;     
    }
    public SensorSourceSetup(SensorManager mSensorManager, Sensor ... sensors) {
        this(mSensorManager , SensorManager.SENSOR_DELAY_NORMAL, sensors);  
    }

    public void accept(Consumer<SensorEvent> submitter) {
        events = new SensorChangeEvents(submitter);
        for (Sensor sensor : sensors)
            mSensorManager.registerListener(events, sensor, samplingPeriodUs);
    }
}
