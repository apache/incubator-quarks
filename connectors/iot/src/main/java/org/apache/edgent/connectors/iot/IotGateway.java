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

import java.util.Map;
import java.util.Set;

import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;

import com.google.gson.JsonObject;

/**
 * A generic IoT gateway device IoT hub connector.
 * <p>
 * <b>This interface is incubating and is subject to change.</b>
 * <p>
 * An IoT gateway device is a conduit for a collection of IoT devices 
 * that lack direct connection to the enterprise IoT hub.
 * <p>
 * An IoT gateway device is an IoT device.  Events can be published
 * that are from the gateway's deviceId and commands can be received that are targeted for it
 * using the gateway's {@link IotDevice#events(TStream, String, int) events()}
 * and {@link IotDevice#commands(String...) commands()}.
 * <p>
 * Use {@link #getIotDevice(Map)} to get an {@code IotDevice} for a gateway connected device
 * or {@link #getIotDeviceId(Map)} to get a deviceId for it.
 * The name/value pairs in the map are IotGateway implementation defined values.
 * Refer to the IotGateway implementation for details.
 * <p>
 * Events can be published that are from a connected device's deviceId and commands can be 
 * received for that are targeted for it using
 * {@link #eventsForDevice(String, TStream, String, int) eventsForDevice()}
 * and {@link #commandsForDevice(Set, String...) commandsForDevice()}. 
 * 
 * @see <a href="{@docRoot}/org/apache/edgent/connectors/iot/package-summary.html">Edgent generic IoT device model</a>
 * @see IotDevice
 */
public interface IotGateway extends IotDevice {
  
  /**
   * Get an {@link IotDevice} for a connected device.
   * No external validation of the attribute values is performed.
   * 
   * @param deviceIdAttrs IotGateway implementation specific attributes
   *                    that identify a device.
   * @return IotDevice
   */
  IotDevice getIotDevice(Map<String,String> deviceIdAttrs);
  
  /**
   * Get a {@code deviceId} for a device.
   * Logically equivalent to {@code getIotDevice(deviceIdAttrs).getDeviceId()}.
   * No external validation of the attribute values is performed.
   *
   * @param deviceIdAttrs IotGateway implementation specific attributes
   *                    that identify a device.
   * @return deviceId
   */
  String getIotDeviceId(Map<String,String> deviceIdAttrs);
  
  /**
   * Get an {@link IotDevice} for a connected device.
   * @param deviceId a value from {@link IotDevice#getDeviceId()}.
   * @return IotDevice
   */
  IotDevice getIotDevice(String deviceId);
  
  /**
   * Publish a stream's tuples as device events.
   * Each tuple is published as a device event with the supplied functions
   * providing the device identifier, event identifier, payload and QoS. 
   * The values can be generated based upon the tuple.
   * <p>
   * Events for a particular device can also be published via its 
   * {@link IotDevice#events(TStream, Function, UnaryOperator, Function) IotDevice.events()}.
   * 
   * @param deviceId
   *            function to supply the device-id that the event is associated with.
   * @param stream
   *            Stream to be published.
   * @param eventId
   *            function to supply the event identifier.
   * @param payload
   *            function to supply the event's payload.
   * @param qos
   *            function to supply the event's delivery Quality of Service.
   * @return TSink sink element representing termination of this stream.
   */
  TSink<JsonObject> eventsForDevice(Function<JsonObject,String> deviceId,
      TStream<JsonObject> stream, Function<JsonObject, String> eventId,
      UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) ;
  
  /**
   * Publish a stream's tuples as device events.
   * Each tuple is published as a device event with the supplied
   * device identifier, event identifier and QoS.
   * <p>
   * Events for a particular device can also be published via its 
   * {@link IotDevice#events(TStream, String, int) IotDevice.event()}.
   * 
   * @param deviceId
   *            Device-id that the event is associated with.
   * @param stream
   *            Stream to be published.
   * @param eventId
   *            Event identifier.
   * @param qos
   *            Event's delivery Quality of Service.
   * @return TSink sink element representing termination of this stream.
   */
  TSink<JsonObject> eventsForDevice(String deviceId,
      TStream<JsonObject> stream, String eventId, int qos) ;

  /**
   * Create a stream of device commands as JSON objects.
   * Each command sent to one of the specified {@code deviceIds} matching {@code commands} will
   * result in a tuple on the stream. The JSON object has these keys:
   * <UL>
   * <LI>{@link #CMD_DEVICE device} - Command's opaque target device's id String.
   * <LI>{@link #CMD_ID command} - Command identifier as a String</LI>
   * <LI>{@link #CMD_TS tsms} - Timestamp of the command in milliseconds since the 1970/1/1 epoch.</LI>
   * <LI>{@link #CMD_FORMAT format} - Format of the command as a String</LI>
   * <LI>{@link #CMD_PAYLOAD payload} - Payload of the command
   *   <UL>
   *   <LI>If {@code format} is {@code json} then {@code payload} is JSON</LI>
   *   <LI>Otherwise {@code payload} is String</LI>
   *   </UL>
   * </LI>
   * </UL>
   * 
   * Logically equivalent to a union of a collection of individual IotDevice specific
   * command streams but enables an IotGateway implementation to implement it more efficiently. 
   * 
   * @param deviceIds
   *            Filter to include commands for the specified deviceIds
   *            If the set is empty no filtering occurs. The commands for any device are included. 
   * @param commands Command identifiers to include. If no command identifiers are provided then the
   * stream will contain all device commands for the specified devices.
   * @return Stream containing device commands.
   */
  TStream<JsonObject> commandsForDevice(Set<String> deviceIds, String... commands);

  /**
   * Create a stream of device commands as JSON objects.
   * Each command sent to the specified {@code deviceId} matching {@code commands} will
   * result in a tuple on the stream. The JSON object has these keys:
   * <UL>
   * <LI>{@link IotDevice#CMD_DEVICE device} - Command's target device's opaque id String.
   * <LI>{@link IotDevice#CMD_ID command} - Command identifier as a String</LI>
   * <LI>{@link IotDevice#CMD_TS tsms} - Timestamp of the command in milliseconds since the 1970/1/1 epoch.</LI>
   * <LI>{@link IotDevice#CMD_FORMAT format} - Format of the command as a String</LI>
   * <LI>{@link IotDevice#CMD_PAYLOAD payload} - Payload of the command
   * <UL>
   * <LI>If {@code format} is {@code json} then {@code payload} is JSON</LI>
   * <LI>Otherwise {@code payload} is String</LI>
   * </UL>
   * </LI>
   * </UL>
   * <P>
   * Equivalent to {@code commandsForDevice(Collections.singleton(deviceId)), ...}. 
   * 
   * @param deviceId
   *            Filter to include commands for the specified deviceId
   * @param commands Command identifiers to include. If no command identifiers are provided then the
   * stream will contain all device commands for the specified device.
   * @return Stream containing device commands.
   */
  TStream<JsonObject> commandsForDevice(String deviceId, String... commands);

  /**
   * Create a stream of device commands as JSON objects.
   * Each command sent to connected devices of type {@code deviceTypeId} matching {@code commands}
   * will result in a tuple on the stream. The JSON object has these keys:
   * <UL>
   * <LI>{@link IotDevice#CMD_DEVICE device} - Command's target device's opaque id String.
   * <LI>{@link IotDevice#CMD_ID command} - Command identifier as a String</LI>
   * <LI>{@link IotDevice#CMD_TS tsms} - Timestamp of the command in milliseconds since the 1970/1/1 epoch.</LI>
   * <LI>{@link IotDevice#CMD_FORMAT format} - Format of the command as a String</LI>
   * <LI>{@link IotDevice#CMD_PAYLOAD payload} - Payload of the command
   * <UL>
   * <LI>If {@code format} is {@code json} then {@code payload} is JSON</LI>
   * <LI>Otherwise {@code payload} is String</LI>
   * </UL>
   * </LI>
   * </UL>
   * <P>
   * An IoT connector implementation may throw
   * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}
   * if it does not support this capability.  See the implementation's documentation.
   * 
   * @param deviceTypeId
   *            Only return commands for connected devices with the specified
   *            device type id value (a value from {@link IotDevice#getDeviceType()}).
   * @param commands Command identifiers to include. If no command identifiers are provided then the
   * stream will contain all device commands for devices with the specified device type id.
   * @return Stream containing device commands.
   */
  TStream<JsonObject> commandsForType(String deviceTypeId, String... commands);
}
