# Edgent Java support

The Edgent runtime is supported on all Java 8 SE, Java 7 SE, and Android
platforms with the exceptions noted below.

An Edgent binary release bundle has a directory for each of the target platforms.
Each target platform directory contains a set of jars for that platform
* java8 - Java 8 SE
* java7 - Java 7 SE
* android - Android

When building an Edgent release (`./gradlew release`) the above platform
directories are in `build/distributions`.  See [DEVELOPMENT.md](DEVELOPMENT.md) for more
information.

This page documents which jars are expected to work in each environment.

A blank entry means a jar is currently not supported in that environment
and no investigation has taken place to see if it can be supported.

## Core

| Jar                             | Java 8 SE | Java 7 SE | Android | Notes |
|---------------------------------|-----------|-----------|---------|-------|
|edgent.api.execution.jar         | yes       | yes       | yes     |       |
|edgent.api.function.jar          | yes       | yes       | yes     |       |
|edgent.api.graph.jar             | yes       | yes       | yes     |       |
|edgent.api.oplet.jar             | yes       | yes       | yes     |       |
|edgent.api.topology.jar          | yes       | yes       | yes     |       |
|edgent.api.window.jar            | yes       | yes       | yes     |       |
|edgent.providers.development.jar | yes       |           | no      | Uses JMX, For development only, not deployment |
|edgent.providers.direct.jar      | yes       | yes       | yes     |       |
|edgent.providers.iot.jar         | yes       | yes       | yes     |       |
|edgent.runtime.appservice.jar    | yes       | yes       | yes     |       |
|edgent.runtime.etiao.jar         | yes       | yes       | yes     |       |
|edgent.runtime.jmxcontrol.jar    | yes       | yes       | no      | Uses JMX |
|edgent.runtime.jobregistry.jar   | yes       |           |         |       |
|edgent.runtime.jsoncontrol.jar   | yes       | yes       | yes     |       |
|edgent.spi.graph.jar             | yes       | yes       | yes     |       |
|edgent.spi.topology.jar          | yes       | yes       | yes     |       |

## Connectors

| Jar                                           | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------------------------|-----------|-----------|---------|-------|
|edgent.connectors.common.jar                   | yes       | yes       | yes     |       |
|edgent.connectors.command.jar                  | yes       |           |         |       |
|edgent.connectors.csv.jar                      | yes       |           |         |       |
|edgent.connectors.file.jar                     | yes       |           |         |       |
|edgent.connectors.http.jar                     | yes       | yes       | yes     |       |
|edgent.connectors.iotf.jar                     | yes       | yes       | yes     |       |
|edgent.connectors.iot.jar                      | yes       | yes       | yes     |       |
|edgent.connectors.jdbc.jar                     | yes       |           |         |       |
|edgent.connectors.kafka.jar                    | yes       |           |         |       |
|edgent.connectors.mqtt.jar                     | yes       |           |         |       |
|edgent.connectors.pubsub.jar                   | yes       | yes       | yes     |       |
|edgent.connectors.serial.jar                   | yes       |           |         |       |
|edgent.connectors.wsclient.jar                 | yes       |           |         |       |
|edgent.connectors.wsclient-javax.websocket.jar | yes       |           |         |       |
|edgent.javax.websocket.jar                     | yes       |           |         |       |

## Applications
| Jar                    | Java 8 SE | Java 7 SE | Android | Notes |
|------------------------|-----------|-----------|---------|-------|
|edgent.apps.iot.jar     | yes       | yes       | yes     |       | 
|edgent.apps.runtime.jar | yes       | yes       | yes     |       | 

### Analytics

| Jar                         | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------|-----------|-----------|---------|-------|
|edgent.analytics.math3.jar   | yes       |           |         |       |
|edgent.analytics.sensors.jar | yes       | yes       | yes     |       |

### Utilities

| Jar                         | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------|-----------|-----------|---------|-------|
|edgent.utils.metrics.jar     | yes       |           |         |       |
|edgent.utils.streamscope.jar | yes       |           |         |       |

### Development Console

| Jar                         | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------|-----------|-----------|---------|-------|
|edgent.console.servlets.jar  | yes       |           | no      | Uses JMX, Servlet |
|edgent.console.server.jar    | yes       |           | no      | Uses JMX, Servlet |

### Android
| Jar                         | Java 8 SE | Java 7 SE | Android | Notes |
|-----------------------------|-----------|-----------|---------|-------|
|edgent.android.topology.jar  | no        | no        | yes     |       |
|edgent.android.hardware.jar  | no        | no        | yes     |       |


## Java API Usage

Documented use of Java packages outside of the Java core packages.
Java core has a number of definitions, but at least those outside
of the Java 8 compact1 definition.

| Feature  | Packages              | Edgent Usage      | Notes |
|----------|-----------------------|-------------------|-------|
|JMX       | `java.lang.managem-ent, javax.managment*` |     | JMX not supported on Android |
|JMX       |                       | utils/metrics     | Optional utility methods |
|JMX       |                       | console/servlets, runtime/jmxcontrol | 
|Servlet   | `javax.servlet*`      | console/servlets  |
|Websocket | `javax.websocket`     | connectors/edgent.javax.websocket, connectors/wsclient-javax-websocket, connectors/javax.websocket-client |
|JDBC      | `java.sql, javax.sql` | connectors/jdbc   |

