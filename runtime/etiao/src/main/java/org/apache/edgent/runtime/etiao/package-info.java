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
 * A runtime for executing an Edgent streaming org.apache.edgent.org.apache.edgent.topology, designed as an embeddable library
 * so that it can be executed in a simple Java application.
 * 
 * <h2>"EveryThing Is An Oplet" (ETIAO)</h2>
 *
 * The runtime's focus is on executing oplets and their connected streams, where each 
 * oplet is just a black box. Specifically this means that functionality is added by the introduction 
 * of oplets into the org.apache.edgent.graph that were not explicitly declared by the application developer.
 * For example, metrics are implemented by oplets, not the runtime. A metric collector is an 
 * oplet that calculates metrics on tuples accepted on its input port, and them makes them 
 * available, for example through JMX.
 */
package org.apache.edgent.runtime.etiao;
