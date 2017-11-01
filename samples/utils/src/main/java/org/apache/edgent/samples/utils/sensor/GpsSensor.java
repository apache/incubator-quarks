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

/**
 * A GPS Sensor Reading
 *
 * TODO rename to GpsSensorReading
 */
public class GpsSensor {

    private double latitude;
    private double longitude;
    private double altitude;
    private double speedMetersPerSec; // meters per sec
    private long time;
    private double course;

    public GpsSensor(double latitude, double longitude, double altitude, double speedMetersPerSec, long time,
            double course) {

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speedMetersPerSec = speedMetersPerSec;
        this.time = time;
        this.course = course;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double geAltitude() {
        return altitude;
    }

    public double getSpeedMetersPerSec() {
        return speedMetersPerSec;
    }

    public long getTime() {
        return time;
    }

    public double getCourse() {
        return course;
    }

    @Override
    public String toString() {
        return latitude + ", " + longitude + ", " + altitude + ", " + speedMetersPerSec + ", " + time + ", " + course;

    }
}
