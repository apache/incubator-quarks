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

public class SimulatedGeofence {
    protected static double GEOFENCE_LATITUDE_MAX = 37.21;
    protected static double GEOFENCE_LATITUDE_MIN = 37.0;
    protected static double GEOFENCE_LONGITUDE_MAX = -121.75;
    protected static double GEOFENCE_LONGITUDE_MIN = -122.0;

    // Simple Geofence test
    public static boolean outsideGeofence(double latitude, double longitude) {

        if (latitude < GEOFENCE_LATITUDE_MIN || latitude > GEOFENCE_LATITUDE_MAX || longitude < GEOFENCE_LONGITUDE_MIN
                || longitude > GEOFENCE_LONGITUDE_MAX)
            return true;
        else
            return false;
    }
}
