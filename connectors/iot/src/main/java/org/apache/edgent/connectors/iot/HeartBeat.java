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
package org.apache.edgent.connectors.iot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Functions;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.plumbing.PlumbingStreams;

import com.google.gson.JsonObject;

/**
 * An IoT device heartbeat event generator.
 */
public class HeartBeat {
  private HeartBeat() { };
  
  /**
   * Add IoT device heart beat processing to a topology.
   * <P>
   * An IoTDevice event containing heart beat information 
   * is periodically published to the specified {@code eventId}.
   * </P>
   * <P>
   * The heart beat provides clients of the IoT hub with liveness information
   * about the device and its connection to the hub.
   * </P>
   * <P>
   * The heart beat also ensures there is some immediate output so
   * the connection to the IoT hub happens as soon as possible.
   * In the case where there may not otherwise be
   * IoT events to publish, a heart beat ensures a connection
   * to the IoT hub is maintained.
   * </P>
   * <P>
   * The heart beat's event payload is the JSON for a JsonObject with the
   * heart beat's properties:
   * <ul>
   * <li>"when" : (string) ISO8601 UTC date/time. e.g. "2016-07-12T17:57:08Z"</li>
   * <li>"time" : (number) {@link System#currentTimeMillis()}</li>
   * </ul> 
   * 
   * @param iotDevice IoT hub device
   * @param period the heart beat period
   * @param unit TimeUnit for the period
   * @param eventId the IotDevice eventId to use for the event
   * @return the {@code TStream<JsonObject>} heartbeat stream
   */
  public static TStream<JsonObject> addHeartBeat(IotDevice iotDevice, long period, TimeUnit unit, String eventId) {
    DateFormat df = newIso8601Formatter();
    TStream<Date> hb = iotDevice.topology().poll(
        () -> new Date(),
        period, unit).tag("heartbeat");
    // Convert to JSON
    TStream<JsonObject> hbj = hb.map(date -> {
        JsonObject j = new  JsonObject();
        j.addProperty("when", df.format(date));
        j.addProperty("time", date.getTime());
        return j;
    }).tag("heartbeat");
    
    TStream<JsonObject> hbs = hbj;
  
    // Tolerate connection outages.  Don't block upstream processing
    // and retain the most recent heartbeat if unable to publish.
    hbj = PlumbingStreams.pressureReliever(hbj, 
                Functions.unpartitioned(), 1).tag("pressureRelieved");
  
    iotDevice.events(hbj, eventId, QoS.FIRE_AND_FORGET);
    
    return hbs;
  }
  
  private static DateFormat newIso8601Formatter() {
    // Quoted "Z" to indicate UTC, no timezone offset
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df;
  }

}
