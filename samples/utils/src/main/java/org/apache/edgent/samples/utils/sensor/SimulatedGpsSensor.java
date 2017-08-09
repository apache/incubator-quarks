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
package org.apache.edgent.samples.utils.sensor;

public class SimulatedGpsSensor {
    private int currentIndex;
    private int simulatedSec;
    private int requestedIterations = -1; // -1 means loop through gpsDataArray
                                          // forever
    private int currentIteration = 0;

    public SimulatedGpsSensor() {
        this.currentIndex = -1;
        this.simulatedSec = 1;
    }

    // TODO: Replace hard-coded data to reading in GPS data from file
    // GPS data from IBM Silicon Valley Lab to IBM Almaden Research and back to
    // IBM SVL
    private static double[][] gpsDataArray = { { 37.195647, -121.748087 }, // IBM
                                                                           // Silicon
                                                                           // Valley
                                                                           // Lab
                                                                           // 555
                                                                           // Bailey
                                                                           // Ave
            { 37.18110679999999, -121.7865003 }, // Calero Reservoir McKean
            { 37.19926299999999, -121.82234399999999 }, // Santa Clara Horsemen
                                                        // Association 20350
                                                        // McKean
            { 37.198632, -121.82234399999999 }, // Forestry & Fire Protection
                                                // 20255 McKean
            { 37.19875469999999, -121.82320830000003 }, // St. Anthony Church
                                                        // 20101 McKean
            { 37.2004004, -121.82578030000002 }, // Challenger School 19950
                                                 // McKean
            { 37.199576, -121.82468900000003 }, // Firehouse No.28 19911 McKean
                                                // Rd
            { 37.211053, -121.806949 }, // IBM Almaden Research Harry Road
            // then go back the reverse direction
            { 37.199576, -121.82468900000003 }, // Firehouse No.28 19911 McKean
                                                // Rd
            { 37.2004004, -121.82578030000002 }, // Challenger School 19950
                                                 // McKean
            { 37.19875469999999, -121.82320830000003 }, // St. Anthony Church
                                                        // 20101 McKean
            { 37.198632, -121.82234399999999 }, // Forestry & Fire Protection
                                                // 20255 McKean
            { 37.19926299999999, -121.82234399999999 }, // Santa Clara Horsemen
                                                        // Association 20350
                                                        // McKean
            { 37.18110679999999, -121.7865003 }, // Calero Reservoir McKean
            { 37.195647, -121.748087 }, // IBM Silicon Valley Lab 555 Bailey Ave
    };

    // TODO Re-write kluge code to read from a file
    // Returns a GpsSensor using the hard-coded list, then repeats through the
    // list
    // forever when requestedIterations = -1, or until iterations is reached.
    public GpsSensor nextGps() {

        GpsSensor result = null;
        if (requestedIterations == -1 || currentIteration < requestedIterations) {
            simulatedSec++;

            // Determine the index to use for generating the next GPS coordinate
            // If currentIndex is already at the end of the array, then set
            // currentIndex to start over at the array beginning at index 0
            // Otherwise, increment currentIndex
            if (currentIndex >= gpsDataArray.length - 1) {
                currentIndex = 0;
                currentIteration++;
            } else
                currentIndex++;

            // hardcode other data (altitude, course, time, speed, etc.) and at
            // index 5 for hard braking and acceleration speed
            if (currentIndex == 5)
                result = new GpsSensor(gpsDataArray[currentIndex][0], gpsDataArray[currentIndex][1], 32.1, 42.4,
                        simulatedSec, 38.2);
            else
                result = new GpsSensor(gpsDataArray[currentIndex][0], gpsDataArray[currentIndex][1], 20.1, 29.0,
                        simulatedSec, 28.2);

        }
        return result;
    }
}
