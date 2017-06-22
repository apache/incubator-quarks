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
package org.apache.edgent.streamscope.mbeans;

/**
 * A registry for {@link StreamScopeMXBean} instances.
 * <P>
 * The registry contains a collection of StreamScopeMXBean instances
 * that are registered by a stream identifier.
 * </P>
 * See {@code org.apache.edgent.providers.development.DevelopmentProvider}
 */
public interface StreamScopeRegistryMXBean {
  
  /**
   * TYPE is used to identify this bean as a StreamScopeRegistry bean when building the bean's {@code ObjectName}.
   * The value is {@value} 
   */
  public static String TYPE = "streamScopeRegistry";
  
  /**
   * Get the {@link StreamScopeMXBean} registered for the specified stream
   * @param jobId the job id (e.g., "JOB_0")
   * @param opletId the oplet id (e.g., "OP_2")
   * @param oport the oplet output port index (0-based)
   * @return null if not found
   */
  public StreamScopeMXBean lookup(String jobId, String opletId, int oport);
  
}

