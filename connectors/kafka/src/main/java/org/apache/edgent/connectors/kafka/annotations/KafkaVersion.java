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
package org.apache.edgent.connectors.kafka.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to prevent the need to implement multiple versions of kafka-adapters.
 * The {@link KafkaVersion} annotation allows specifying a minimum and maximum version
 * of Kafka that supports a given functionality. This information currently can only
 * be provided on a class/type (includes all methods) or individual method level.
 *
 * This information is then used by an aspect that wraps access to version constrained
 * methods and throws meaningful exceptions when being used without satisfying the
 * version criteria.
 */
// We need the information to be available at runtime.
@Retention(RetentionPolicy.RUNTIME)
// We will only annotate methods or classes with this version check.
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface KafkaVersion {

    String fromVersion();

    String toVersion();

}
