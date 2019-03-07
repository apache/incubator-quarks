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
package org.apache.edgent.execution.services;

/**
 * At runtime a container provides services to
 * executing elements such as oplets and functions.
 * 
 * <P>
 * Services, providers and applications can optionally store
 * preferences (configuration information) using a {@code java.util.pref.Preferences}
 * instance registered as a service. Any provider may optionally register
 * a root {@code Preferences} node. Once a provider registers
 * a {@code Preferences} node, preferences will
 * be saved across reboots of the provider and/or JVM if the underlying
 * {@code Preferences} implementation supports it.
 * 
 * <BR>
 * Services and application code can store keys
 * in the root {@code Preferences} node or create child nodes
 * as required.
 * <pre>
 * <code>
 * Preferences prefs = rs.getService(Preferences.class);
 * if (prefs != null) {
 *     prefs.put("threshold", 3);
 * }
 * </code>
 * </pre>
 *
 */
public interface RuntimeServices {
    
    /**
     * Get a service for this invocation.
     * <P>
     * These services must be provided by all implementations:
     * </P>
     * <UL>
     * <LI>
     * {@code java.util.concurrent.ThreadFactory} - Thread factory, runtime code should
     * create new threads using this factory.
     * </LI>
     * <LI>
     * {@code java.util.concurrent.ScheduledExecutorService} - Scheduler, runtime code should
     * execute asynchronous and repeating tasks using this scheduler. 
     * </LI>
     * </UL>
     * 
     * @param <T> service class type
     * @param serviceClass Type of the service required.
     * @return Service of type implementing {@code serviceClass} if the 
     *      container this invocation runs in supports that service, 
     *      otherwise {@code null}.
     */
    <T> T getService(Class<T> serviceClass);
}
