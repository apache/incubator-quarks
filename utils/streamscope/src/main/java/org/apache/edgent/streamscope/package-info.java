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
 * Stream Oscilloscope - package for instrumenting streams to capture tuples.
 * <P>
 * A {@link org.apache.edgent.streamscope.StreamScope StreamScope} captures tuples.
 * StreamScopes are registered with a {@link org.apache.edgent.streamscope.StreamScopeRegistry StreamScopeRegistry}
 * runtime service and are also controllable via
 * {@link org.apache.edgent.streamscope.mbeans.StreamScopeMXBean StreamScopeMXBean} mbeans
 * registered with a {@link org.apache.edgent.streamscope.mbeans.StreamScopeRegistryMXBean StreamScopeRegistryMXBean}
 * runtime {@link org.apache.edgent.execution.services.ControlService ControlService}.
 * </P><P>
 * {@link org.apache.edgent.streamscope.StreamScopeSetup StreamScopeSetup} performs the necessary setup for a
 * {@link org.apache.edgent.topology.TopologyProvider TopologyProvider} to use the package,
 * including registration of services and instrumenting a org.apache.edgent.org.apache.edgent.topology with
 * StreamScope instances.
 */
package org.apache.edgent.streamscope;
