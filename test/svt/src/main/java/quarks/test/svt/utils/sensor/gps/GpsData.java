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
package quarks.test.svt.utils.sensor.gps;

public class GpsData {
	private double latitude;  
	private double longitude;
	private double speedMph;
	private long time;
	private String driverId;
	
	public GpsData(double latitude, double longitude, double speedMph, long time, String driverId) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.speedMph = speedMph;
		this.time = time;
		this.driverId = driverId;
	}
	
	public double getSpeedMph() {
		return speedMph;
	}
	
	public double getLatitude() {
		return latitude; 
	}
	
	public double getLongitude() {
		return longitude; 
	}
	
	@Override
	public String toString() {
		return  driverId
				+ ", " + latitude 			
				+ ", " + longitude 
				+ ", " + speedMph
				+ ", " + time
				;
	}
}
