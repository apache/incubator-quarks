/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015,2016 
*/

/**
 * Quarks device connector API to a message hub.
 * 
 * Generic device model that supports a device model consisting of:
 * <UL>
 * <LI>
 * <B>Device events</B> - A device {@link quarks.connectors.iot.IotDevice#events(quarks.topology.TStream, String, int) publishes} <em>events</em> as messages to a message hub to allow
 * analysis or processing by back-end systems, etc.. A device event consists of:
 * <UL>
 * <LI>  <B>event identifier</B> - Application specified event type. E.g. {@code engineAlert}</LI>
 * <LI>  <B>event payload</B> - Application specified event payload. E.g. the engine alert code and sensor reading.</LI>
 * <LI>  <B>QoS</B> - {@link quarks.connectors.iot.QoS Quality of service} for message delivery. Using MQTT QoS definitions.</LI>
 * </UL>
 * Device events can be used to send any data including abnormal events
 * (e.g. a fault condition on an engine), periodic or aggregate sensor readings,
 * device user input etc.
 * <BR>
 * The format for the payload is JSON, support for other payload formats may be added
 * in the future.
 * </LI>
 * <P>
 * <LI>
 * <B>Device Commands</B> - A device {@link quarks.connectors.iot.IotDevice#commands(String...) subscribes} to <em>commands</em> from back-end systems
 * through the message hub. A device command consists of:
 * <UL>
 * <LI>  <B>command identifier</B> - Application specified command type. E.g. {@code statusMessage}</LI>
 * <LI>  <B>command payload</B> - Application specified command payload. E.g. the severity and
 * text of the message to display.</LI>
 * </UL>
 * Device commands can be used to perform any action on the device including displaying information,
 * controlling the device (e.g. reduce maximum engine revolutions), controlling the Quarks application, etc.
 * </LI>
 * The format for the payload is typically JSON, though other formats may be used.
 * </UL>
 * </P>
 * <P>
 * Device event and command identifiers starting with "{@link quarks.connectors.iot.IotDevice#RESERVED_ID_PREFIX quarks}"
 * are reserved for use by Quarks.
 * </P>
 */
package quarks.connectors.iot;

