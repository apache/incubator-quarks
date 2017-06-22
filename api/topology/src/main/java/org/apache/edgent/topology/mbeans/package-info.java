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
 * Controls for executing topologies.
 * <P>
 * The "Attribute Name" column values below correspond to {@code KEY} values defined
 * in {@code org.apache.edgent.runtime.jsoncontrol.JsonControlService}.
 * 
 * <h3>Application Service </h3>
 * {@linkplain org.apache.edgent.topology.services.ApplicationService Application service}
 * allows an application to be registered
 * so that it be be submitted remotely using a device command.
 * 
 * <BR>
 * This service registers a control MBean
 * {@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean}
 * to provide control of the service.
 *  
 * <h4 id="submitApp">Submit an Application</h4>
 * Method: {@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean#submit(String, String)}
 * <table border=1 cellpadding=3 cellspacing=1>
 * <caption>JSON Submit Application</caption>
 * <tr>
 *    <th id="attrName" align=center><b>Attribute name</b></th>
 *    <th id="type" align=center><b>Type</b></th>
 *    <th id="value" align=center><b>Value</b></th>
 *    <th id="desc" align=center><b>Description</b></th>
 *  </tr>
 * <tr>
 *    <td headers="attrName">{@code type}</td>
 *    <td headers="type">String</td>
 *    <td headers="value">{@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean#TYPE appService}</td>
 *    <td headers="desc">{@code ApplicationServiceMXBean} control MBean type.</td>
 *  </tr>
 *  <tr>
 *    <td headers="attrName">{@code op}</td>
 *    <td headers="type">String</td>
 *    <td headers="value">{@code submit}</td>
 *    <td headers="desc">Invoke {@link org.apache.edgent.topology.mbeans.ApplicationServiceMXBean#submit(String, String) submit} operation
 *    against the control MBean.</td>
 *  </tr>
 *  <tr>
 *    <td headers="attrName">{@code alias}</td>
 *    <td headers="type">String</td>
 *    <td headers="value">Alias of control MBean.</td>
 *    <td headers="desc">Default is {@link org.apache.edgent.topology.services.ApplicationService#ALIAS edgent}.</td>
 *  </tr>
 *  <tr>
 *    <td rowspan="2" headers="attrName">{@code args}</td>
 *    <td rowspan="2" headers="type">List</td>
 *    <td headers="value">String: application name</td>
 *    <td headers="desc">Registered application to submit.</td>
 *  </tr>
 *  <tr>
 *    <td headers="value">JSON Object: submission configuration</td>
 *    <td headers="desc">Configuration for the submission,
 *    see {@link org.apache.edgent.execution.Submitter#submit(Object, com.google.gson.JsonObject) submit()}.
 *    If {@code jobName} is not set in the configuration then the job is submitted with {@code jobName} set to the
 *    application name.</td>
 *  </tr>
 * </table>
 * <BR>
 * Example submitting the application {@code EngineTemp} with no configuration, will result in a running
 * job named {@code EngineTemp}.
 * <BR>
 * {@code {"type":"appService","alias":"edgent","op":"submit","args":["EngineTemp",{}]}}
 */
package org.apache.edgent.topology.mbeans;
