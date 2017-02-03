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

/**
 * Edgent IoT device and IoT Gateway device connector API to an IoT message hub.
 * <P>
 * An IoT environment consists of an enterprise IoT message hub and devices and other clients
 * connected to it.
 * Each IoT device has an identity in the hub. The form of a device's identity is the domain
 * of an IoT hub implementation.
 * How a device becomes registered to an IoT hub and generally what device management capabilities
 * exist and how a device is managed is beyond the scope of the 
 * "analytic pipelines" focused Edgent APIs.
 * </P>
 * <P>
 * An IoT device can publish device events to and receive device commands from an IoT hub.
 * An IoT gateway device is an IoT device that is also conduit for a collection of IoT devices 
 * that lack direct connection to the enterprise IoT hub.
 * A gateway can publish events on behalf of its connected devices and 
 * receive commands from the hub targeted to them.
 * An Edgent IoT hub connector bridges the gap between this generic model and and
 * particular IoT hub implementation's underlying protocols.
 * </P>
 * <P>
 * More specifically, the generic IoT device model consists of:
 * <UL>
 * <LI>
 * <B>Device events</B> - A device {@link org.apache.edgent.connectors.iot.IotDevice#events(org.apache.edgent.topology.TStream, String, int) publishes} <em>events</em> as messages to a message hub to allow
 * analysis or processing by back-end systems, etc.. A device event consists of:
 * <UL>
 * <LI>  <B>event identifier</B> - Application specified event type. E.g. {@code engineAlert}</LI>
 * <LI>  <B>event payload</B> - Application specified event payload. E.g. the engine alert code and sensor reading.</LI>
 * <LI>  <B>QoS</B> - {@link org.apache.edgent.connectors.iot.QoS Quality of service} for message delivery. Using MQTT QoS definitions.</LI>
 * </UL>
 * Device events can be used to send any data including abnormal events
 * (e.g. a fault condition on an engine), periodic or aggregate sensor readings,
 * device user input etc.
 * <BR>
 * The format for the payload is JSON, support for other payload formats may be added
 * in the future.
 * </LI>
 * <LI>
 * <B>Device Commands</B> - A device {@link org.apache.edgent.connectors.iot.IotDevice#commands(String...) subscribes} to <em>commands</em> from back-end systems
 * through the message hub. A device command consists of:
 * <UL>
 * <LI>  <B>command identifier</B> - Application specified command type. E.g. {@code statusMessage}</LI>
 * <LI>  <B>command payload</B> - Application specified command payload. E.g. the severity and
 * text of the message to display.</LI>
 * </UL>
 * Device commands can be used to perform any action on the device including displaying information,
 * controlling the device (e.g. reduce maximum engine revolutions), controlling the Edgent application, etc.
 * <BR>
 * The format for the payload is typically JSON, though other formats may be used.
 * </LI>
 * </UL>
 * <P>
 * Device event and command identifiers starting with "{@link org.apache.edgent.connectors.iot.IotDevice#RESERVED_ID_PREFIX edgent}"
 * are reserved for use by Edgent.
 * </P>
 */
package org.apache.edgent.connectors.iot;

