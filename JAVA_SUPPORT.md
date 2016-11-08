# Edgent Java support

As documented in (DEVELOPMENT.md) Edgent development is performed
using Java 8 but Java 7 and Android is supported through use of
retrolambda to build specific jars.

Building a release `ant release` produces three sets of Jars under
* target/java8 - Java 8 SE
* target/java7 - Java 7 SE
* target/android - Android

[WIP] Building a release `./gradlew release` produces three sets of Jars under
* build/distributions/java8 - Java 8 SE
* build/distributions/java7 - Java 7 SE
* build/distributions/android - Android

This page documents which jars are expected to work in each environment.

A blank entry means a jar is currently not supported in that environment
and no investigation has taken place to see if it can be supported.

## Core

| Jar | Java 8 | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.api.execution.jar| yes | yes | yes |
|edgent.api.function.jar| yes | yes | yes |
|edgent.api.graph.jar| yes | yes | yes |
|edgent.api.oplet.jar| yes | yes | yes |
|edgent.api.topology.jar| yes | yes | yes |
|edgent.api.window.jar| yes | yes | yes |
|edgent.providers.development.jar | yes | | no | Uses JMX, For development only, not deployment |
|edgent.providers.direct.jar| yes | yes | yes |
|edgent.providers.iot.jar| yes | yes | yes |
|edgent.runtime.appservice.jar| yes | yes | yes |
|edgent.runtime.etiao.jar| yes | yes | yes |
|edgent.runtime.jmxcontrol.jar| yes | yes | no | Uses JMX |
|edgent.runtime.jobregistry.jar| yes | | |
|edgent.runtime.jsoncontrol.jar| yes | yes | yes |
|edgent.spi.graph.jar| yes | yes | yes |
|edgent.spi.topology.jar| yes | yes | yes |

## Connectors

| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.connectors.common.jar | yes | yes | yes | |
|edgent.connectors.command.jar | yes | | | |
|edgent.connectors.csv.jar | yes | | | |
|edgent.connectors.file.jar | yes | | | |
|edgent.connectors.http.jar | yes | yes | yes | |
|edgent.connectors.iotf.jar | yes | yes | yes | |
|edgent.connectors.iot.jar | yes | yes | yes | |
|edgent.connectors.jdbc.jar | yes | | | |
|edgent.connectors.kafka.jar | yes | | | |
|edgent.connectors.mqtt.jar | yes | | | |
|edgent.connectors.pubsub.jar | yes | yes | yes | |
|edgent.connectors.serial.jar | yes | | | |
|edgent.connectors.wsclient.jar | yes | | | |
|edgent.connectors.wsclient-javax.websocket.jar | yes | | | |
|edgent.javax.websocket.jar | yes | | | |

## Applications
| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.apps.iot.jar | yes | yes | yes | | 
|edgent.apps.runtime.jar | yes | yes | yes | | 

### Analytics

| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.analytics.math3.jar | yes | | | |
|edgent.analytics.sensors.jar | yes | yes | yes | |

### Utilities

| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.utils.metrics.jar | yes | | | |
|edgent.utils.streamscope.jar | yes | | | |

### Development Console

| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.console.servlets.jar | yes | | no | Uses JMX, Servlet|
|edgent.console.server.jar | yes | | no | Uses JMX, Servlet |

### Android
| Jar | Java 8 SE | Java 7 SE | Android | Notes |
|---|---|---|---|---|
|edgent.android.topology.jar | no | no | yes | |
|edgent.android.hardware.jar | no | no | yes | |

## Adding Jars to Java 7 & Android

Java 7 jars are created using `platform/java7/build.xml`. Adding a Java just requires:
* Adding it to target `retro7.edgent` - Copy entry for an existing jar.
* Adding any tests for it to targets `test7.setup` and `test7.run` - Copy entry for an existing jar.

Any Java 7 jar is automatically included in Android unless it is explictly excluded in `platform/android/build.xml`.

## Java API Usage

Documented use of Java packages outside of the Java core packages.
Java core has a number of definitions, but at least those outside
of the Java 8 compact1 definition.

| Feature | Packages | Edgent Usage | Notes |
|---|---|---|---|
|JMX | `java.lang.management, javax.managment*` | | JMX not supported on Android |
|JMX | | utils/metrics | Optional utility methods |
|JMX | | console/servlets, runtime/jmxcontrol | 
|Servlet| `javax.servlet*` | console/servlets |
|Websocket| `javax.websocket` | connectors/edgent.javax.websocket, connectors/wsclient-javax-websocket, connectors/javax.websocket-client |
|JDBC| `java.sql, javax.sql` | connectors/jdbc |

