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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.apache.edgent.function.Consumer;

/**
 * Sensor event listener that submits sensor
 * change events as tuples using a Consumer.
 * 
 */
public class SensorChangeEvents implements SensorEventListener {
    private final Consumer<SensorEvent> eventSubmitter;
    
    /**
     * @param eventSubmitter How events are submitted to a stream.
     */
    public SensorChangeEvents(Consumer<SensorEvent> eventSubmitter) {
        this.eventSubmitter = eventSubmitter;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        eventSubmitter.accept(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
