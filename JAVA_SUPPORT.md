<!--

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

-->
# Edgent Java support

The Edgent runtime is supported on all Java 8 SE, Java 7 SE, and Android
platforms with the exceptions noted below.

An Edgent release includes convenience binaries for the Edgent SDK.
The binaries (jars and wars) are available in the Apache Nexus repository
and Maven Central.  If you are building the Edgent SDK, the artifacts
will be installed into the local maven repository.

See `samples/README.md` for general information regarding Edgent Application
development and deployment and tooling to support that.

SDK binary artifacts are published for each of the supported platform types
and the artifact's groupId portion of its coordinate indicates which
platform the artifact is for.  The Edgent jar/war file name is the same
for each of the platforms.

The coordinates have the following form:
* groupId: `org.apache.edgent[.platform]`
* artifactId: `edgent-<component>-<subcomponent>`

The [.platform] is as follows:
* blank/omitted - Java 8 SE
* `.java7` - Java 7 SE
* `.android` - Android

For example:
```
<!-- for Java8 -->
<dependency>
  <groupId>org.apache.edgent</groupId>
  <artifactId>edgent-providers-direct</artifactId>
  <version>1.2.0</version>
</dependency>

<!-- for Java7 -->
<dependency>
  <groupId>org.apache.edgent.java7</groupId>
  <artifactId>edgent-providers-direct</artifactId>
  <version>1.2.0</version>
</dependency>
```

Generally, an Edgent application needs to declare these depedencies:
* the Edgent Provider used
* the Edgent Analytics used
* the Edgent Utils used
* the Edgent Connectors used
* an SLF4J implementation for category "runtime"

At application execution time, those same dependencies as well
as their dependencies (e.g., other "internal" Edgent Core jars, 
external dependency jars such as for mqtt) must be included on
the application's classpath.

The `samples/template/pom.xml` is structured to support all of this.

This page documents which Edgent SDK jars are expected to work in each environment.

A blank entry means no investigation has taken place to see if the jar
and its features are supported in that environment.

## Core

| Jar                                   | Java 8 SE | Java 7 SE | Android | Notes |
|---------------------------------------|-----------|-----------|---------|-------|
|edgent-api-execution-<ver>.jar         | yes       | yes       | yes     |       |
|edgent-api-function-<ver>.jar          | yes       | yes       | yes     |       |
|edgent-api-graph-<ver>.jar             | yes       | yes       | yes     |       |
|edgent-api-oplet-<ver>.jar             | yes       | yes       | yes     |       |
|edgent-api-topology-<ver>.jar          | yes       | yes       | yes     |       |
|edgent-api-window-<ver>.jar            | yes       | yes       | yes     |       |
|edgent-providers-development-<ver>.jar | yes       | yes       | no      | Uses JMX, For development only, not deployment |
|edgent-providers-direct-<ver>.jar      | yes       | yes       | yes     |       |
|edgent-providers-iot-<ver>.jar         | yes       | yes       | yes     |       |
|edgent-runtime-appservice-<ver>.jar    | yes       | yes       | yes     |       |
|edgent-runtime-etiao-<ver>.jar         | yes       | yes       | yes     |       |
|edgent-runtime-jmxcontrol-<ver>.jar    | yes       | yes       | no      | Uses JMX |
|edgent-runtime-jobregistry-<ver>.jar   | yes       | yes       | yes     |       |
|edgent-runtime-jsoncontrol-<ver>.jar   | yes       | yes       | yes     |       |
|edgent-spi-graph-<ver>.jar             | yes       | yes       | yes     |       |
|edgent-spi-topology-<ver>.jar          | yes       | yes       | yes     |       |

## Connectors

| Jar                                                 | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------------------------|-----------|-----------|---------|-------|
|edgent-connectors-common-<ver>.jar                   | yes       | yes       | yes     |       |
|edgent-connectors-command-<ver>.jar                  | yes       | yes       |         |       |
|edgent-connectors-csv-<ver>.jar                      | yes       | yes       |         |       |
|edgent-connectors-file-<ver>.jar                     | yes       | yes       |         |       |
|edgent-connectors-http-<ver>.jar                     | yes       | yes       | yes     |       |
|edgent-connectors-iotf-<ver>.jar                     | yes       | yes       | yes     |       |
|edgent-connectors-iot-<ver>.jar                      | yes       | yes       | yes     |       |
|edgent-connectors-jdbc-<ver>.jar                     | yes       | yes       |         |       |
|edgent-connectors-kafka-<ver>.jar                    | yes       | yes       |         |       |
|edgent-connectors-mqtt-<ver>.jar                     | yes       | yes       |         |       |
|edgent-connectors-pubsub-<ver>.jar                   | yes       | yes       | yes     |       |
|edgent-connectors-serial-<ver>.jar                   | yes       | yes       |         |       |
|edgent-connectors-websocket-<ver>.jar                | yes       | yes       |         |       |
|edgent-connectors-websocket-base-<ver>.jar           | yes       | yes       |         |       |
|edgent-connectors-websocket-jetty-<ver>.jar          | yes       | yes       |         |       |
|edgent-connectors-websocket-misc-<ver>.jar           | yes       | yes       |         |       |

## Applications
| Jar                          | Java 8 SE | Java 7 SE | Android | Notes |
|------------------------------|-----------|-----------|---------|-------|
|edgent-apps-iot-<ver>.jar     | yes       | yes       | yes     |       | 
|edgent-apps-runtime-<ver>.jar | yes       | yes       | yes     |       | 

### Analytics

| Jar                               | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------|-----------|-----------|---------|-------|
|edgent-analytics-math3-<ver>.jar   | yes       | yes       |         |       |
|edgent-analytics-sensors-<ver>.jar | yes       | yes       | yes     |       |

### Utilities

| Jar                               | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------|-----------|-----------|---------|-------|
|edgent-utils-metrics-<ver>.jar     | yes       | yes       |         |       |
|edgent-utils-streamscope-<ver>.jar | yes       | yes       |         |       |

### Development Console

| Jar                               | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------|-----------|-----------|---------|-------|
|edgent-console-server-<ver>.jar    | yes       | yes       | no      | Uses JMX, Servlet |
|edgent-console-servlets-<ver>.war  | yes       | yes       | no      | Uses JMX, Servlet |

### Android

| Jar                               | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------|-----------|-----------|---------|-------|
|edgent-android-topology-<ver>.jar  | no        | no        | yes     |       |
|edgent-android-hardware-<ver>.jar  | no        | no        | yes     |       |


## Java API Usage

Documented use of Java packages outside of the Java core packages-
Java core has a number of definitions, but at least those outside
of the Java 8 compact1 definition-

| Feature  | Packages              | Edgent Usage      | Notes |
|----------|-----------------------|-------------------|-------|
|JMX       | `java-lang-management, javax-managment*` |     | JMX not supported on Android |
|JMX       |                       | utils/metrics     | Optional utility methods |
|JMX       |                       | console/servlets, runtime/jmxcontrol | 
|Servlet   | `javax-servlet*`      | console/servlets  |
|Websocket | `javax-websocket`     | connectors/websocket* |
|JDBC      | `java-sql, javax-sql` | connectors/jdbc   |

