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
 * Iot provider that allows multiple applications to
 * share an {@code IotDevice}.
 * 
 * <H3>IoT device</H3>
 * 
 * <H3>Application registration</H3>
 * The provider includes an {@link org.apache.edgent.topology.services.ApplicationService ApplicationService} that allows applications
 * to be registered by name. Once registered an application can be started (and stopped) remotely
 * through the control service using a device command.
 * 
 * <H3>Supported device commands</H3>
 * This provider supports a number of system level device commands to control the applications
 * running within it.
 * <H4>Control service</H4>
 * Device commands with the command identifier '{@link org.apache.edgent.connectors.iot.Commands#CONTROL_SERVICE edgentControl}'
 * are sent to the provider's control service, an instance of {@link org.apache.edgent.runtime.jsoncontrol.JsonControlService JsonControlService}.
 * This allows invocation of an operation against a control mbean registered with the
 * control service, either by an application or the provider itself.
 * <BR>
 * The command's data (JSON) uniquely identifies a control MBean through its type and
 * alias, and indicates the operation to call on the MBean and the arguments to
 * pass to the operation.
 * <BR>
 * Thus any control operation can be remotely invoked through a {@code edgentControl} device command,
 * including arbitrary control mbeans registered by applications.
 * 
 * <H4 id="providerOps">Provider operations</H4>
 * <table border="1" summary="Provider operations">
 * <tr>
 *     <th id="operation">Operation</th><th id="cmdIdentifier">Command identifier</th>
 *     <th id="type">type</th><th id="alias">alias</th><th id="op">op</th><th id="args">args</th>
 *     <th id="controlMbean">Control MBean</th>
 * </tr>
 * <tr>
 *    <td rowspan="2" headers="operation"><strong>Submit (start) a registered application</strong></td>
 *    <td headers="cmdIdentifier">{@code edgentControl}</td>
 *    <td headers="type">{@code appService}</td>
 *    <td headers="alias">{@code edgent}</td>
 *    <td headers="op">{@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean#submit(String, String) submit}</td>
 *    <td headers="args"><em>{@code [applicationName, configJSONObject]}</em></td>
 *    <td headers="controlMbean">{@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean ApplicationServiceMXBean}</td>
 * </tr>
 * <tr>
 *    <td headers="cmdIdentifier"><strong>Sample command data</strong></td>
 *    <td colspan=5 headers="type alias op args controlMbean">{@code {"type":"appService","alias":"edgent","op":"submit","args":["Heartbeat",{}]}}</td>
 * </tr>
 * <tr></tr>
 * 
 * <tr>
 *    <td rowspan="2" headers="operation"><strong>Close (stop) a running registered application</strong></td>
 *    <td headers="cmdIdentifier">{@code edgentControl}</td>
 *    <td headers="type">{@code job}</td>
 *    <td headers="alias"><em>{@code applicationName}</em></td>
 *    <td headers="op">{@link org.apache.edgent.execution.mbeans.JobMXBean#stateChange(org.apache.edgent.execution.Job.Action) stateChange}</td>
 *    <td headers="args">{@code ["CLOSE"]}</td>
 *    <td headers="controlMbean">{@link org.apache.edgent.execution.mbeans.JobMXBean JobMXBean}</td>
 * </tr>
 * <tr>
 *    <td headers="cmdIdentifier"><strong>Sample command data</strong></td>
 *    <td colspan=5 headers="type alias op args controlMbean">{@code {"type":"job","alias":"Heartbeat","op":"stateChange","args":["CLOSE"]}}</td>
 * </tr>
 * <tr></tr>
 * </table>
 * 
 */
package org.apache.edgent.providers.iot;
