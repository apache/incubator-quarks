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

See [APPLICATION_DEVELOPMENT.md](APPLICATION_DEVELOPMENT.md) for general
information on Edgent Application Development, Packaging and Execution.

Additional information may also be found in
Getting Started https://edgent.apache.org/docs/edgent-getting-started

# Setup

Once you have downloaded and unpacked the samples source bundle 
or cloned the the samples repository you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8
* Maven - *(optional) (https://maven.apache.org/)*

Maven is used as build tool. Currently there are two options:

1. Using the maven-wrapper (the `mvnw` or `mvnw.bat` command - preferred)
2. Using an installed version of Maven (the `mvn` command)

The maven-wrapper will automatically download and install the
correct Maven version and use that. Besides this, there is no
difference between using the `mvn` and `mvnw` command.

You may also use a maven-integrated IDE with the samples.
e.g., see the _Using Eclipse_ section below.

The samples use Edgent SDK jars that have been released
in a maven repository such as Maven Central.

Alternatively, you can download the Edgent SDK sources and build them,
populating your local maven repository.  The samples
will then use those Edgent SDK jars.  Adjust the `edgent.version` 
property in the top level samples `pom.xml` accordingly.
See [downloads](https://edgent.apache.org/docs/downloads) 
for downloading the Edgent SDK sources.

# Building the Edgent samples

By default Java8 class files are generated.
Java7 platform class files are produced when the appropriate
profile is specified.

Currently, building and running the samples for the Android platform
is not supported.  Many samples use the `DevelopmentProvider` and
the `DevelopmentProvider` is not supported on the Android platform.

Build the samples
```sh
./mvnw clean package  # -Pplatform-java7 as needed
```

A standard jar and uber jar are created for each sample category
in the sample category's target directory: `<category>/target`.


## Running the samples

See the README.md in each sample category directory.

# Using Eclipse

The Edgent Git repository, or samples source release bundle, contains 
Maven project definitions for the samples.

Once you import the Maven projects into your workspace, builds
in Eclipse use the same artifacts as the Maven command line tooling. 
Like the command line tooling, the jars for dependent projects
are automatically downloaded to the local maven repository
and used.

If you want to use Eclipse to clone your fork, use the 
Eclipse Git Team Provider plugin

1. From the *File* menu, select *Import...*
2. From the *Git* folder, select *Projects from Git* and click *Next*
3. Select *Clone URI* to clone the remote repository. Click *Next*.
    + In the *Location* section, enter the URI of your fork in the *URI* field (e.g., `git@github.com:<username>/incubator-edgent.git`). The other fields will be populated automatically. Click *Next*. If required, enter your passphrase.
    + In the *Source Git Repository* window, select the branch (usually `master`) and click *Next*
    + Specify the directory where your local clone will be stored and click *Next*. The repository will be cloned. Note: You can build and run tests using Maven in this directory.
4. In the *Select a wizard to use for importing projects* window, click *Cancel*.  Then follow the steps below to import the Maven projects.


Once you have cloned the Git repository to your machine or are working 
from an unpacked samples source release bundle, import the Maven projects
into your workspace

1. From the *File* menu, select *Import...*
2. From the *Maven* folder, select *Existing Maven Projects* and click *Next*
  + browse to the `samples` directory in the clone or source release directory and select it.  A hierarchy of samples projects / pom.xml files will be listed and all selected. 
  + Verify the *Add project(s) to working set* checkbox is checked
  + Click *Finish*.  Eclipse starts the import process and builds the workspace.

Top-level artifacts such as `README.md` are available under the 
`edgent-samples` project.

Note: Specifics may change depending on your version of Eclipse or the 
Eclipse Maven or Git Team Provider.

Once the samples projects have been imported you can run them as any 
Eclipse application. E.g.,

1. open the `HelloEdgent.java` sample
2. click on *Run*, *Run As*, then *Java application*.  `HelloEdgent` runs and prints to the Console view.

# Samples Summary

<pre>
HelloEdgent          Basic mechanics of declaring a topology and executing
                     it. Prints Hello Edgent! to standard output.

PeriodicSource       Create a stream by polling a random number generator
                     for a new value every second and then prints out the
                     raw tuple value and a filtered and transformed stream.
                          
SensorAggregates     Demonstrates partitioned window aggregation and 
                     filtering of simulated sensors that are bursty in
                     nature, so that only intermittently is the data output
                     to standard output.
                         
File                 Use the File stream connector to write a stream of
                     tuples to files. Also watch a directory for new files
                     and create a stream of tuples from the file contents.
                         
Iotp                 Use the IBM Watson IoT Platform connector to send
                     simulated sensor readings to an IBM Watson IoT Platform
                     instance as device events. Receive device commands.
                         
JDBC                 Use the JDBC stream connector to write a stream of
                     tuples to an Apache Derby database table. Create a
                     stream of tuples by reading a table.
                         
Kafka                Use the Kafka stream connector to publish a stream of
                     tuples to a Kafka topic. Create a stream of tuples by
                     subscribing to a topic and receiving messages from it.
                         
MQTT                 Use the MQTT stream connector to publish a stream of
                     tuples to a MQTT topic. Create a stream of tuples by
                     subscribing to a topic and receiving messages from it.
                         
SensorAnalytics      Demonstrates a more complex sample that includes 
                     configuration control, a device of one or more sensors
                     and some typical analytics, use of MQTT for publishing
                     results and receiving commands, local results logging,
                     conditional stream tracing.
</pre>

Other samples are provided but have not yet been fully documented.
Feel free to explore.

