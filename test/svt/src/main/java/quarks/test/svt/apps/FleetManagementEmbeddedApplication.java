/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quarks.test.svt.apps;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import quarks.console.server.HttpServer;
import quarks.metrics.Metrics;
import quarks.providers.development.DevelopmentProvider;
import quarks.test.svt.utils.sensor.gps.GpsSensor;
import quarks.test.svt.utils.sensor.gps.SimulatedGeofence;
import quarks.test.svt.utils.sensor.gps.SimulatedGpsSensor;
import quarks.test.svt.utils.sensor.gps.GpsData;
import quarks.topology.TStream;
import quarks.topology.TWindow;
import quarks.topology.Topology;

/**
 * These are sample Quarks Fleet Management applications that can be embedded in a motor vehicle. The Quarks 
 * application reads in Global Positioning System (GPS) and On-board Diagnostics(OBD) tuples, which are then 
 * analyzed in real-time.  
 * 
 * <p>
 * The GPS application 1) logs the GPS coordinates, 2) filters for tuples where the speed exceeds a threshold,
 * and 3) filters for tuples where the GPS coordinates is outside a geofence boundary, then publishes log/alert messages
 * to a message broker such as Kafka or MQTT. 
 * 
 * <p> Optionally, centralized applications on servers can further manage the fleet of vehicles by 
 * subscribing to the messages for all the vehicles. For example, a Quarks application can subscribe to these messages, which are converted into a 
 * tuple flow for further analysis.  On a server, with more resources, Quarkscan correlate speeds with actual speed 
 * limit signs at the GPS location, to assess the driver. Another example may be to visualize all the vehicles on a map. 
 * These ideas are outside the scope of these sample applications. 
 *  
 * 
 * TODO: Add OBD
 * 
 * Parameter (optional): console
 * If specified, the Quarks development console's URL is written to file consoleUrl.txt. 
 * Enter this URL into a browser to display the topology graph and metrics for 
 * this application.
 */

public class FleetManagementEmbeddedApplication {   

	public static void main(String[] args) throws Exception {
		
		//TODO: make these configurable properties
		boolean trackGpsLocation = true;
		boolean trackSpeeding = true;
		boolean trackGeofence = true;
		double MILES_PER_HOUR_TO_METERS_PER_SEC_MULTIPLER =  0.44704;
		
		//TODO - get driverId from input/file and VIN from OBD
		String driverId = "driver1";
		String VIN = "123456"; 
		int maxSpeedMph = 70;
		
		boolean console = false;
		if (args.length == 1 && args[0].toLowerCase().equals("console"))
			console = true;
		
		double maxSpeedMetersPerSec = maxSpeedMph * MILES_PER_HOUR_TO_METERS_PER_SEC_MULTIPLER;
		System.out.println("maxSpeedMph: " + maxSpeedMph);
		System.out.println("maxSpeedMetersPerSec: " + maxSpeedMetersPerSec);
		
		DevelopmentProvider tp = new DevelopmentProvider(); 
		Topology t1 = tp.newTopology("GPS topology");

		// Source 1: GPS data
		SimulatedGpsSensor g = new SimulatedGpsSensor();
		TStream<GpsSensor> gpsSensor = t1.poll(() -> g.nextGps(), 500, TimeUnit.MILLISECONDS);		
		
		// 1.1 Log GPS location 
		if (trackGpsLocation) {
			TStream<GpsSensor> logGps = gpsSensor.peek(t -> System.out.println("log GPS: " + t.toString()));
			logGps.tag("logGps");
			//TODO: replace print with write to Kafka
			logGps.print();
		}
		
		//1.2 Filter for speeding
		if (trackSpeeding) {
			TStream<GpsSensor> speeding = gpsSensor.filter(t -> t.getSpeedMetersPerSec() > maxSpeedMph);
			
			speeding.tag("speeding");
			//Count speeding tuples
//			Metrics.counter(speeding);

			speeding.peek(t -> System.out.println("Alert: speeding - " + t.toString()));
			// TODO: replace print() with write to Kafka topic: Alert_speeding"
			speeding.print();
		}
		
		// 1.3. Filter for Geofence boundary exceptions
		if (trackGeofence) {
			TStream<GpsSensor> geofence = gpsSensor.filter(t -> 
				 SimulatedGeofence.outsideGeofence(t.getLatitude(), t.getLongitude()));
			
			geofence.tag("geofence");
			//Count Geofence exceptions
//			Metrics.counter(geofence);

			geofence.peek(t -> System.out.println("Alert: geofence - " + t.toString()));
			//TODO: replace print with write to Kafka topic "Alert_geofence_violation"
			geofence.print();
		}
		
		// Remove the altitude data since it's not needed, to help reduce transmission costs
		// Add driverId
		// Convert speed from meters/sec to miles/hour
		TStream<GpsData> gpsData = gpsSensor.map(t -> new GpsData (t.getLatitude(), t.getLongitude(), t.getSpeedMetersPerSec() * (1/ MILES_PER_HOUR_TO_METERS_PER_SEC_MULTIPLER), t.getTime(), driverId));
		gpsData.print();


		// Source 2: OBD & EML
		// TODO: Add Source 2: On-Board Diagnostic data

		tp.submit(t1);  	
		
	    // If the console option was specified, write the console URL into file consoleUrl.txt
        if (console && tp instanceof DevelopmentProvider) {
            try {
                PrintWriter writer = new PrintWriter("consoleUrl.txt", "UTF-8");
                writer.println(tp.getServices().getService(HttpServer.class).getConsoleUrl());
                writer.close();
            } catch ( Exception e) {
                e.printStackTrace();
            }
        }      
	}
}
  