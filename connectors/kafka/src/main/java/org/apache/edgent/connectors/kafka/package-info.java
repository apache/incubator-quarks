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
 * Apache Kafka enterprise messing hub stream connector.
 * <p>
 * The connector uses and includes components from the Kafka 0.8.2.2 release.
 * It has been successfully tested against kafka_2.11-0.10.1.0 and kafka_2.11-0.9.0.0 server as well.
 * <p>
 * Stream tuples may be published to Kafka broker topics
 * and created by subscribing to broker topics.
 * For more information about Kafka see
 * <a href="http://kafka.apache.org">http://kafka.apache.org</a>
 */
package org.apache.edgent.connectors.kafka;