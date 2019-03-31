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
package org.apache.edgent.topology.services;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.topology.Topology;

import com.google.gson.JsonObject;

/**
 * Represents an org.apache.edgent.org.apache.edgent.topology that can be built.
 * 
 * A class implementing {@code TopologyBuilder} can
 * be registered as a service provider in a jar file for
 * automatic application registration using
 * {@link ApplicationService#registerJar(String, String)}.
 *
 */
public interface TopologyBuilder {
    /**
     * Name the application will be known as.
     * @return Name the application will be known as.
     */
    String getName();
    
    /**
     * How the application is built.
     * @return Function that builds the application.
     */
    BiConsumer<Topology, JsonObject> getBuilder();
}
