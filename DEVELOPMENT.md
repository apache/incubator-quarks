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
## Development of Apache Edgent

*Apache Edgent is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Incubator PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.*

See [README.md](README.md) for high-level information about Apache Edgent.

This document describes building and the development of Apache Edgent itself, not how to develop Edgent applications.

 * See http://edgent.incubator.apache.org/docs/edgent-getting-started for getting started using Edgent

The Edgent community welcomes contributions, please *Get Involved*!

 * http://edgent.incubator.apache.org/docs/community
 
If you are interested in developing a new connector see [Writing Connectors for Edgent Applications](https://cwiki.apache.org/confluence/display/EDGENT/Writing+Connectors+For+Edgent+Applications)

See the [Edgent Wiki](https://cwiki.apache.org/confluence/display/EDGENT) for additional information including Internal and Design notes. 

## Switched from Ant and Gradle to Maven

See the updated _Building_ and _Using Eclipse_ sections below.
The Ant and Gradle tooling is no longer functional.

It's recommended that developers of Edgent create a new workspace instead of
reusing current ant-based Edgent workspaces.

## Renamed from Apache Quarks
Apache Edgent is the new name and the conversion is complete.

Code changes:

  * Package names have the prefix "org.apache.edgent"
  * JAR names have the prefix "edgent"

Users of Edgent will need to update their references to the above.
It's recommended that developers of Edgent create a new workspace instead of
reusing their Quarks workspace.

## Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8
* Java 7 - *(optional) only required when also building the Java 7 and Android artifacts with `toolchain` support* 
* Maven - *(optional) (https://maven.apache.org/)*

Maven is used as build tool in any case. Currently there are two options however:

1. Using an installed version of Maven (using the `mvn` command)
2. Using the maven-wrapper (using the `mvnw` command)

When using option 2 the script will automatically download and install the correct Maven version and use that. Besides this, there is no difference between using the `mvn` and `mvnw` command.

Per default the build will use Java 8 to perform the build of the Java 7 and Android modules. In order to reliably test the Java 7 modules on a real Java 7 Runtime, we defined an additional profile `toolchain` which lets Maven run the tests in the Java 7 Modules with a real Java 7 Runtime.

In preparation of building the Java 7 and Android modules with enabled `toolchain` support it is required to tell Maven the location of both the Java 7 and Java 8 SDKs. This is done in a file called `toolchains.xml`:

``` toolchains.xml
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>{path to the Java 8 SDK}</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.7</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>{path to the Java 7 SDK}</jdkHome>
    </configuration>
  </toolchain>
<toolchains>
```

This file is located or has to be created in: `~/.m2/toolchains.xml`

All Edgent runtime development is done using Java 8. JARs for Java 7 and Android platforms are created by back-porting the compiled Java 8 code using a tool called `retrolambda`. More details on this below.

## Building Edgent (For using Edgent)

///////////////////////////////////////////////////////////
TODO this chapter needs work.  
On one hand, the README file with the source release
describes how to build Edgent - at least the simple/common case.
Also below, for this "using Edgent" case isn't "install" more
appropriate than "package" since the typical case will then be
to build Edgent apps using the Edgent jars that are then installed into the
local maven repository.
Lastly, I don't understand the value of -Pdistribution (I guess the "package"
task makes sense for use with that), i.e., -Pdistribution creates a tarball that
only contains the Edgent SDK jars, not any of the transitive dependencies.
Then what?  Is the story/tools noted in samples/APPLICATION_DEVELOPMENT.md
more complete / useful? (get-edgent-jars.sh can be used to collect
the Edgent jars and the transitive deps, which the user can then manually
bundle into a tarball or zip).
///////////////////////////////////////////////////////////

Building using a pre-installed Maven installation from a source release bundle:
``` sh
$ mvn package
```

Building using the maven-wrapper which automatically handles downloading the right Maven version.
``` sh
$ ./mvnw package
```

Both will build and test all Edgent Java 8 modules using Maven.

If you want to use the artifacts built by this in other projects, please 
use `install` instead of `package` as this will additionally make the build 
results available to other projects on the same machine in the local Maven 
repository.

For a not quite two hour introduction into Maven please feel free to watch
this video we created for another Apache project:
https://vimeo.com/167857327

A set of Maven `profiles` have been created to control which parts
should be built. The default profile only builds and tests the Java 8
versions of all modules and doesn't assemble a binary distribution
as usually Maven builds don't require such a step. It also doesn't build
the Java 7 or Android modules either.

Edgent currently comes with these profiles:

- `distribution`: Builds one binary distribution for Java 8. If the java 7 and android profiles are enabled too, for each of these an additional binary distribution is created.
- `platform-java7`: Builds Java 7 versions of all Edgent modules and runs the tests.
- `platform-android`: Builds Android versions of all Edgent modules that are compatible with Android (See [JAVA_SUPPORT.md](JAVA_SUPPORT.md).
- `toolchain`: Runs the tests in the Java 7 and Android modules using a Java 7 runtime instead of Java 8 version, which happens if this profile is not enabled. 

As the Android modules are based on the Java 7 versions, when building the `platform-android` profile, the `platform-java7` profile is required to be enabled too, or the build will fail. 

Example: Building an Edgent binary release bundle for Java 8:
``` sh
$ mvn package -Pdistribution
```

Each artifact built by this will be located in the default Maven location (inside each modules `target` directory)
The distribution archive will be located in `distribution/target/apache-edgent-incubating-1.2.0-SNAPSHOT-bin.zip` (and tar.gz)

See [Getting Started](https://edgent.apache.org/docs/edgent-getting-started) for information on using the binary release bundle.

## Building for Edgent (For Edgent development)

The primary build process is using [Maven](https://maven.apache.org/),
any pull request is expected to maintain the build success of `mvn package`.

The Maven wrapper `<edgent>/{mvnw,mvnw.cmd}` should be used.
The wrapper ensures the appropriate version of Maven is used and it
will automatically download it if needed, e.g.:
``` sh
$ ./mvnw clean package
```

## Continuous Integration

### Travis CI

When a pull request is opened on the GitHub mirror site, the Travis CI service runs a full build of the java8 modules.

The latest build status for the project's branches can be seen at: https://travis-ci.org/apache/incubator-edgent/branches

The build setup is contained in `.travis.yml` in the project root directory.
It includes:

* Building the project
* Testing on Java 8
  - Not all tests may be run, some tests are skipped due to timing issues or if excessive setup is required.

If your test randomly fails because, for example, it depends on publicly available test services,
or is timing dependent, and if timing variances on the Travis CI servers may make it more likely
for your tests to fail, you may disable the test from being executed on Travis CI using the
following statement:
``` Java
    @Test
    public void testMyMethod() {
        assumeTrue(!Boolean.getBoolean("edgent.build.ci"));
        // your test code comes here
        ...
    }
```

Closing and reopening a pull request will kick off a new build against the pull request.

### Jenkins, SonarQube

In addition to Travis CI running the quick tests with only the Java8 modules, we have also setup additional build-jobs at the Apaches Jenkins instance at https://builds.apache.org/view/E-G/view/Edgent/

This build also automatically runs on every commit, but in contrast to the Travis build, it also builds and tests the Java7 and Android modules using the toolchain profile.

This is also the build which produces and deploys the Maven artifacts that are published to the Apache Maven repository at https://repository.apache.org/

As an additional quality assurance tool, this build also runs a SonarQube analysis who's results are available at Apaches SonarQube instance at https://builds.apache.org/analysis/overview?id=45154

## Java 7 and Android

Java 7 and Android target platforms are supported through use of
retrolambda to convert Edgent Java 8 JARs to Java 7 JARs. In order
to make it easy to address easily, for each Java 8 module a matching
Java 7 version is located inside the `<edgent>/platforms/java7`
directory. For Android only those counterparts exist which are generally
supported on Android.

In general all Java 7 modules differ from the ordinary Java 8 versions 
as these modules don't contain any source code or resources. They are
all built by unpacking the Java 8 jars content into the current modules 
target directory. So the output is effectively located exactly in the 
ame location it would have when normally compiling the Java 8 version. 
There the retrolambda plugin is executed to convert the existing class 
files into ones compatible with Java 7.

The Android versions are even simpler, as all they do is unpack the Java 7
versions and re-pack the content with the android groupId. All except the
two modules which are currently only available on Android 
(located in the `<edgent>/platforms/android/android` directory). These 
modules are built up similar to the Java 8 versions, but they also contain
the retrolambda plugin execution. While it would have been possible to 
treat these modules as Java 7, for the sake of an equal coding experience
it was decided to make it possible to write the same type of code for all
modules.

An Android module's dependency on the Java 7 version makes the requirement
obvious, that in order to build the Android versions, the Java 7 versions
have to be built too.

See [JAVA_SUPPORT.md](JAVA_SUPPORT.md) for which Edgent capabilities / JARs 
are supported for each environment.

## Test reports

The typical maven build contains two phases of unit-tests.
The Unit-Test phase which is executed by the surefire maven plugin
and the Integration-Test phase, which is executed by the failsafe
maven plugin.

When running a normal maven `package` build, only the unit-test phase is executed.
When running `verify` or above (`install`, `deploy`, etc.) the integration
tests are also executed.

Each Maven plugin produces output to different directories:
* `<module>/target/surefire-reports` - JUnit unit-test reports
* `<module>/target/failsafe-reports` - JUnit integration-test reports

In addition to running the unit tests, coverage data is automatically 
collected by the `jacoco-maven-plugin`, which is configured to store
its data in `<module>/target/coverage-reports` in files called 
`jacoco-ut.exec` and `jacoco-it.exec`.

Even if at least the surfire and failsafe output is generated in a human
readable txt and xml form, the jacoco output is intended on being used 
by tools. SonarQube is for example able to interpret this information 
In order to generate nicely formatted html reports, please have a look
at the following `Site generation` chapter.

## Site generation

Maven has 3 built in lifecycles:
* clean - For cleaning up (effectively simply deleting the output forler)
* default - For building, testing, deploying the code
* site - For generating, documentation, reports, ...

If the human readable version of all of these should be generated, all needed
to do this, is to append a simple `site:site` at the end of the maven command.

```sh
./mvnw -Pdistribution,platform-java7,platform-android clean verify site:site
```
Each modules `<module>/target/site` directory will then contain the generated 
Module documentation.

## Testing the Kafka Connector

The kafka connector tests aren't run by default as the connector must
connect to a running Kafka/Zookeeper config.

There are apparently ways to embedd Kafka and Zookeeper for testing purposes but
we're not there yet. Contributions welcome!

Setting up the servers is easy.
Follow the steps in the [KafkaStreamsTestManual](connectors/kafka/src/test/java/org/apache/edgent/test/connectors/kafka/KafkaStreamsTestManual.java) javadoc.

Once kafka/zookeeper are running you can run the tests and samples:
```sh
#### run the kafka tests
./mvnw -pl connectors/kafka test '-Dtest=**/*Manual'

#### run the sample
(cd samples; ./mvnw package -DskipTests)  # build if not already done
cd samples/scripts/connectors/kafka
cat README
./runkafkasample.sh sub
./runkafkasample.sh pub
```

## Code Layout

The code is broken into a number of projects and modules within those projects defined by directories under `edgent`.
Each top level directory is a project and contains one or more modules:

* `api` - The APIs for Edgent. In general there is a strict split between APIs and
implementations to allow multiple implementations of an API, such as for different device types or different approaches.
* `spi` - Common implementation code that may be shared by multiple implementations of an API.
There is no requirement for an API implementation to use the provided spi code.
* `runtime` - Implementations of APIs for executing Edgent applications at runtime.
Initially a single runtime is provided, `etiao` - *EveryThing Is An Oplet* -
A micro-kernel that executes Edgent applications by being a very simple runtime where all
functionality is provided as *oplets*, execution objects that process streaming data.
So an Edgent application becomes a graph of connected oplets, and items such as fan-in or fan-out,
metrics etc. are implemented by inserting additional oplets into the application's graph.
* `providers` - Providers bring the Edgent modules together to allow Edgent applications to
be developed and run.
* `connectors` - Connectors to files, HTTP, MQTT, Kafka, JDBC, etc. Connectors are modular so that deployed
applications need only include the connectors they use, such as only MQTT. Edgent applications
running at the edge are expected to connect to back-end systems through some form of message-hub,
such as an MQTT broker, Apache Kafka, a cloud based IoT service, etc.
* `apps` - Applications for use in an Internet of Things environment.
* `analytics` - Analytics for use by Edgent applications.
* `utils` - Optional utilities for Edgent applications.
* `console` - Development console that allows visualization of the streams within an Edgent application during development.
* `samples` - Sample applications, from Hello World to some sensor simulation applications.
* `android` - Code specific to Android.
* `test` - SVT

## Coding Conventions

Placeholder: see [EDGENT-23](https://issues.apache.org/jira/browse/EDGENT-23)

A couple of key items in the mean time:

* Use spaces not hard tabs, indent is 4 spaces
* Don't use wildcard imports
* Don't deliver code with warnings (e.g., unused imports)
* All source files, scripts, etc must have the standard Apache License header
  * run the `rat` build task to check license headers
*** Per ASF policy, released source bundles must not contain binaries (e.g., .class, .jar)**
* Per ASF policy, release source and binary bundle LICENSE and NOTICE files must be accurate and up to date, and only bundled 3rd party dependencies whose license meets the ASF licensing requirements can be included. 

## Logging

[SLF4J](http://www.slf4j.org) is used for logging and tracing.

Search the code for org.slf4j.LoggerFactory to see a sample of its use.

### Use of Java 8 features
Edgent's primary development environment is Java 8, to take advantage of lambda expressions
since Edgent's primary API is a functional one.

**However**, in order to support Android (and Java 7), other features of Java 8 are not used in the core
code. Lambdas are translated into Java 7 compatible classes using retrolambda.

Thus:

* For core code that needs to run on Android:
   * The only Java 8 feature that can be used is lambda expressions
   * JMX functionality cannot be used.
* For test code that tests core code that runs on Android:
   * Java 8 lambda expressions can be used
   * Java 8 default & static interface methods
   * Java 8 new classes and methods cannot be used

In general, most code is expected to work on Android (but might not yet) with the exception:

* Functionality aimed at the developer environment, such as console and development provider
* Any JMX related code

## The ASF / GitHub Integration

The Edgent code is in ASF resident git repositories:

    https://git-wip-us.apache.org/repos/asf/incubator-edgent.git

The repositories are mirrored on GitHub:

    https://github.com/apache/incubator-edgent

Use of the normal GitHub workflow brings benefits to the team including
lightweight code reviewing, automatic regression tests, etc.
for both committers and non-committers.

For a description of the GitHub workflow, see:

    https://guides.github.com/introduction/flow/
    https://guides.github.com/activities/hello-world/

In summary:

* Fork the incubator-edgent GitHub repository
* Clone your fork, use lightweight per-task branches, and commit / push changes to your fork
  * Descriptive branch names are good. You can also include a reference
    to the JIRA issue, e.g., *mqtt-ssl-edgent-100* for issue EDGENT-100
* When ready, create a pull request.  Committers will get notified.
  * Include *EDGENT-XXXX* (the JIRA issue) in the name of your pull request
  * For early preview / feedback, create a pull request with *[WIP]* in the title.
    Committers won’t consider it for merging until after *[WIP]* is removed.

Since the GitHub incubator-edgent repository is a mirror of the ASF repository,
the usual GitHub based merge workflow for committers isn’t supported.

Committers can use one of several ways to ultimately merge the pull request
into the repo at the ASF. One way is described here:

* http://mail-archives.apache.org/mod_mbox/incubator-quarks-dev/201603.mbox/%3C1633289677.553519.1457733763078.JavaMail.yahoo%40mail.yahoo.com%3E

Notes with the above PR merge directions:

  * Use an HTTPS URL unless you have a SSH key setup at GitHub:
    - `$ git remote add mirror https://github.com/apache/incubator-edgent.git`

## Using Eclipse

The Edgent Git repository, or source release bundle, contains 
Maven project definitions for the various components of Edgent
such as api, runtime, connectors.

Once you import the Maven projects into your workspace,
builds and JUnit testing of Edgent in Eclipse use the 
same artifacts as the Maven command line tooling. Like
the command line tooling, the jars for dependent projects
are automatically downloaded to the local maven repository
and used.

If you want to use Eclipse to clone your fork, use the Eclipse Git Team Provider plugin
1. From the *File* menu, select *Import...*
2. From the *Git* folder, select *Projects from Git* and click *Next*
3. Select *Clone URI* to clone the remote repository. Click *Next*.
    + In the *Location* section, enter the URI of your fork in the *URI* field (e.g., `git@github.com:<username>/incubator-edgent.git`). The other fields will be populated automatically. Click *Next*. If required, enter your passphrase.
    + In the *Source Git Repository* window, select the branch (usually `master`) and click *Next*
    + Specify the directory where your local clone will be stored and click *Next*. The repository will be cloned. Note: You can build and run tests using Maven in this directory.
4. In the *Select a wizard to use for importing projects* window, click *Cancel*.  Then follow the steps below to import the Maven projects.


Once you have cloned the Git repository to your machine or are working from an unpacked source release bundle, import the Maven projects into your workspace
1. From the *File* menu, select *Import...*
2. From the *Maven* folder, select *Existing Maven Projects* and click *Next*
  + browse to the root of the clone or source release directory and select it.  A hierarchy of projects / pom.xml files will be listed and all selected. 
  + Verify the *Add project(s) to working set* checkbox is checked
  + Click *Finish*.  Eclipse starts the import process and builds the workspace.  Be patient, it may take a minute or so.

Top-level artifacts such as `README.md` are available under the `edgent-parent` project.

Note: Specifics may change depending on your version of Eclipse or the Eclipse Maven or Git Team Provider.

### Markdown Text Editor

The ALv2 license headers in various markdown files (e.g., README.md)
seem to confuse the Eclipse `wikitext` editor resulting in blank contents
in its preview panel.  This situation may be improved by installing 
the `Markdown text editor` from the Eclipse marketplace and adjusting
Eclipse's file associations accordingly.
