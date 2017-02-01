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

import java.util.Collection;
import java.util.Map;

import org.apache.edgent.topology.TStream;

import com.google.gson.JsonObject;

/**
 * A generic IoT Gateway device connector.
 * <p>
 * An IoT Gateway device is a conduit for a collection of IoT devices 
 * that lack direct connection to the enterprise IoT hub.
 * <p>
 * The IoT Gateway device is an {@link IotDevice}.  Events can be published
 * that are from the gateway device and commands can be received that are targeted for it
 * using the IotGateway's {@code events()} and {@code commands()}.
 * <p>
 * Use {@link #getIotDevice(Map)} to get an IotDevice for a connected device.
 * The name/value pairs in the map are IotGateway implementation defined values.
 * Refer to the IotGateway implementation for details.
 * Events can be published that are from that device and commands can be 
 * received for that are targeted for that device using the connected device's IotDevice
 * {@code events()} and {@code commands()).
 * 
 * @see IotDevice
 */
public interface IotGateway extends IotDevice {
  
  /**
   * Get an {@link IotDevice} for a connected device.
   * @param deviceIdAttrs IotGateway implementation specific attributes
   *                    that identify a connected device.
   * @return
   */
  public IotDevice getIotDevice(Map<String,String> deviceIdAttrs);
  
  /**
   * Get an {@link IotDevice} for a connected device.
   * @param deviceId a value from {@link IotDevice#getDeviceId()}.
   * @return
   */
  public IotDevice getIotDevice(String deviceId);

  /**
   * Create a stream of device commands as JSON objects.
   * Each command sent to one of the specified devices matching {@code commands} will
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
   * This is logically equivalent to a union of a collection of individual IotDevice specific
   * command streams but enables an IotGateway implementation to implement it more efficiently. 
   * 
   * @param devices
   *            Only return commands for the specified connected devices
   * @param commands Command identifiers to include. If no command identifiers are provided then the
   * stream will contain all device commands for the specified devices.
   * @return Stream containing device commands.
   */
  TStream<JsonObject> commands(Collection<IotDevice> devices, String... commands);

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
