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
 * Oplets API.
 * <P>
 * An oplet is a stream processor that can have 0-N input ports and 0-M output ports.
 * Tuples on streams connected to an oplet's input port are delivered to the oplet for processing.
 * The oplet submits tuples to its output ports which results in the tuples
 * being present on the connected streams.
 * </P>
 */
package org.apache.edgent.oplet;