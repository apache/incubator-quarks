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
reusing current gradle-based Edgent workspaces.

## Branches

The `develop` branch is used for development.  Jenkins is setup to build this branch and publish internal SNAPSHOT build results to the ASF Nexus SNAPSHOTS Repository (https://repository.apache.org/content/repositories/snapshots).

The `master` branch contains released code. Releases are published to the ASF Nexus Releases Repository (https://repository.apache.org/content/repositories/releases). The Releases repository is automatically mirrored to the Maven Central Repository.

See `src/site/asciidoc/releasing.adoc` for a more complete overview of the branches and their content.  The Release Manager's Guide may also add some clarity (https://cwiki.apache.org/confluence/display/EDGENT/Release+Manager%27s+Guide)

## Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8
* Java 7 - *(optional) only required when also building the Java 7 and Android artifacts with `toolchain` support* 
* Maven - *(optional) (https://maven.apache.org/)*

Maven is used as build tool. Currently there are two options:

1. Using the maven-wrapper (the `mvnw` or `mvnw.bat` command - preferred)
2. Using an installed version of Maven (the `mvn` command)

The maven-wrapper will automatically download and install the correct Maven version and use that. Besides this, there is no difference between using the `mvn` and `mvnw` command.

You may also use a maven-integrated IDE for Edgent development.  e.g., see the _Using Eclipse_ section below.

All Edgent runtime development is done using Java 8. JARs for Java 7 and Android platforms are created by back-porting the compiled Java 8 code using a tool called `retrolambda`. More details on this below.

Per default the build will use Java 8 to perform the build of the Java 7 and Android modules. In order to reliably __test__ the Java 7 modules on a real Java 7 Runtime, we defined an additional profile `toolchain` which lets Maven run the tests in the Java 7 Modules with a real Java 7 Runtime.

In preparation for testing the Java 7 and Android modules with enabled `toolchain` support, edit or create `~/.m2/toolchains.xml`:

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

Set the jdkHome values appropriately for your system.
e.g., on an OSX system:
``` sh
  j8 jdkHome:  /Library/Java/JavaVirtualMachines/jdk1.8.0_112.jdk/Contents/Home
  j7 jdkHome:  /Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home
```


## Building Edgent For Edgent Development

Any pull request is expected to maintain the build success of `mvn package`.

To build and test for Java 8
``` sh
$ ./mvnw clean package   # -DskipTests to omit tests
```

To build __and properly test__ the Edgent Java 7 and Android platform jars
requires an installed Java 7 JRE and the `toolchains.xml` setup above, then
``` sh
$ ./mvnw clean package -Djava8.home=$JAVA_HOME -Ptoolchain,platform-java7,platform-android
```

### Documentation of all defined Maven profiles

A set of Maven `profiles` have been created to control which parts should be built. 
The default profile only builds and tests the Java 8 versions of all modules and doesn't assemble a binary distribution as usually Maven builds don't require such a step. 
It also doesn't build the Java 7 or Android modules either.

Edgent currently comes with these profiles:

- `apache-release`: Builds source release bundle under `target` 
- `distribution`: Builds one binary distribution bundle under `distribution/target` for Java 8. If the java 7 and android profiles are enabled too, for each of these an additional binary distribution is created.
- `platform-java7`: Builds Java 7 versions of all Edgent modules and runs the tests.
- `platform-android`: Builds Android versions of all Edgent modules that are compatible with Android (See [JAVA_SUPPORT.md](JAVA_SUPPORT.md).
- `toolchain`: Runs the tests in the Java 7 and Android modules using a Java 7 runtime instead of Java 8 version, which happens if this profile is not enabled. 

As the Android modules are based on the Java 7 versions, when building the `platform-android` profile, the `platform-java7` profile is required to be enabled too, or the build will fail. 

For a not quite two hour introduction into Maven please feel free to watch this video we created for another Apache project: https://vimeo.com/167857327

## Building Edgent For Using Edgent

__Note:__ Apache Edgent releases include convenience binaries. Use of them
is covered in [samples/APPLICATION_DEVELOPMENT.md](https://github.com/apache/incubator-edgent-samples/blob/develop/APPLICATION_DEVELOPMENT.md).

If instead you want to build Edgent for your use there are two different use-cases:

1.  Build Edgent for use with a Maven project. 
2.  Build Edgent for use with non-Maven integrated tooling.  

### Building Edgent for use with Maven

Build, test, and install the Edgent Java8 jars in the local maven repository
``` sh
$ ./mvnw clean install  # -DskipTests to skip tests
```

To also build, but not test, the Edgent Java 7 and Android platform jars:
``` sh
$ ./mvnw clean install -DskipTests -Pplatform-java7,platform-android
```

To build __and properly test__ the Edgent Java 7 and Android platform jars
requires an installed Java 7 JRE and the `toolchains.xml` setup above, then
``` sh
$ ./mvnw clean install -Djava8.home=$JAVA_HOME -Ptoolchain,platform-java7,platform-android
```


### Building Edgent for NOT using it with Maven

Build Edgent as described above to populate the local maven repository.
Then see [samples/APPLICATION_DEVELOPMENT.md](https://github.com/apache/incubator-edgent-samples/blob/develop/APPLICATION_DEVELOPMENT.md)
for information about the `get-edgent-jars.sh` script.

An alternative to using the `get-edgent-jars.sh` script is to
create a binary distribution bundle consisting of the Edgent runtime
jars and their external dependencies.

Build a binary distribution bundle for Java 8
``` sh
$ ./mvnw clean package -DskipTests -Pdistribution
```

The distribution bundle is created under `distribution/target`.
The `libs` directory inside the bundle contains the Edgent jars and 
the `ext` directory contains third party dependencies the Edgent jars require.

<b>NOTE: each third party dependency in the bundle comes with its 
own copyright and license terms which you implicitly accept when using
a distribution bundle.  See the README file in the bundle for more
information.</b>

You will need to manually setup the CLASSPATH for the build tooling that you're
using to develop your Edgent application.


To also build the Edgent Java 7 and Android platform jars:
``` sh
$ ./mvnw clean package -DskipTests -Pdistribution -Pplatform-java7,platform-android
```
The distribution bundles will be in `platforms/java7/distribution/target`
and `platforms/android/distribution/target` respectively.
 

## Continuous Integration

### Travis CI

When a pull request is opened on the GitHub mirror site, the Travis CI service runs a full build of the java8 modules.

The latest build status for the project's branches can be seen at: https://travis-ci.org/apache/incubator-edgent/branches

The build setup is contained in `.travis.yml` in the project root directory.
It includes:

* Building the project
* Testing on Java 8
  - Not all tests may be run, some tests are skipped due to timing issues or if excessive setup is required.

In an attempt to more generally desentize tmo failures
when the system property edgent.build.ci=true is set
some runtime and test infrastructure components will 
bump the normal tmo value (e.g., 10x).
This affects travis and Jenkins runs (both set edgent.build.ci).
See:

    * TStreamTest.waitForCompletion()
    * AbstractTester.complete()
    * Execuatble.invokeAction()
    * generally search for uses of edgent.build.ci
        * maybe remove other test specific uses of it in light of the general change 

The following may now best be avoided:
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

Heads up: the (Jenkins?) test failure reporting tooling seems to get confused
in the face of the same named tests being run for multiple platforms.
Generally you will see each test file listed twice: once for Java8 and once
for Java7.  In the html results it seems impossible to tell which platform
a failed test (or passed test for that matter) applies to.  Even though the
html links for the two tests differ (e.g., the 2nd one has a "_2" at the end
of the URL), a failed test's page shows the passed test's page.  My approach
to investigating failures is to open the "View as plain text" page and
then use the browser's search feature to look for the test name of interest
to locate the output for the failing test.  ugh.

## Java 7 and Android Build Tooling

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
same location it would have when normally compiling the Java 8 version. 
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

Also see _Coding Conventions_ below.

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

* clean - For cleaning up (effectively simply deleting the output folder)
* default - For building, testing, deploying the code
* site - For generating, documentation, reports, ...

If the human readable version of all of these should be generated, all needed
to do this, is to append a simple `site:site` at the end of the maven command.

```sh
./mvnw -Pdistribution,platform-java7,platform-android clean verify site:site
```
Each modules `<module>/target/site` directory will then contain the generated 
Module documentation.

### Aggregated Javadoc

Javadoc generation is a bit of a mess / problem.  

Our customized "aggregated javadoc" (as seen on the website) is formed
by configurations of the `maven-javadoc-plugin`.  As noted in the pom, 
that configuration has no effect for `mvn javadoc:aggregate`, only `mvn site`.

Then, as also noted in the pom, a problem was encountered in the release
processing so the `aggregate reportSet` was commented out.  Hence only
non-aggregated javadoc is created.

Until someone figures out how to address this in a reasonable manner, the
following can be used:

``` sh
 # edit pom.xml to uncomment the javadoc plugin config's aggregate reportSet
./mvnw install -DskipTests  # "package" isn't enough
./mvnw site  # target/site/apidocs will have the aggregated javadoc
``` 

To generate this javadoc for a release, in the clone you're doing
the release in, and after the `release:perform`, 
cd to the release's `target/checkout` directory and perform the above cmds

Or, create a new clone from the release's tag
(`git clone --branch edgent-<ver> the-https-repo-url tmp-javadoc-clone`)
and do the cmds there.

Note, trying to do the above using a source-release tar/zip bundle fails
because `mvn site` must be run in a git repo.

## More Build Tooling Miscellenea

There is a lot of surface area to the maven build tooling.  The following
information may help to better understand it.

* `pom.xml/maven-surefile-plugin` - unit test execution
* `pom.xml/maven-failsafe-plugin` - integration test execution
* `pom.xml/jacoco-maven-plugin` - jacoco code coverage reports
* `pom.xml/animal-sniffer-maven-plugin` - retrolambda results checker
* `pom.xml/org.codehaus.sonar-plugins` - SonarQube code quality reports
* `pom.xml/maven-javadoc-plugin` - javadoc generation and the config
  for all of the "grouping" control.
* `pom.xml/apache-rat-plugin` - builds automatically run Apache RAT
  (Release Audit Tool) for checking for appropriate content.  
  The build fails if the checking fails.
  See configuration info for controlling excluded artifacts.
* `pom.xml/maven-assembly-plugin` - used in a couple places for configuring
  and generating "assemblies" => source release bundle, distribution bundles  
* `pom.xml/maven-site-plugin` - things related to website generation that includes
   a number of interesting things, including html reports from various things
   above plus aggregated javadoc.  How / if this untimately relates to the
   public website is TDB.
* `platforms/java7/pom.xml/retrolambda-maven-plugin` 
   and `platforms/android/android/pom.xml/retrolambda-maven-plugin` - where
   retrolambda is enabled
* As mentioned earlier the current scheme for generating Java7 and Android
  Edgent jars, is achieved by replicating the java8 project structure in
  platforms/{java7,android}.  <b>Manual synchronization of the corresponding
  info in the alternate platform poms and other configuration files
  is required.</b>
* LICENSE and NOTICE: it's a requirement that released bundles
  (source release bundle, released jars) contain accurate LICENSE, NOTICE
  and DISCLAIMER files.  Some of the Edgent projects contain code that
  that was contributed by IBM, some of the generated jars/war bundle
  external components.  The build tooling is configured to automatically
  include standard ALv2.0 LICENSE and NOTICE files in the jars.
  The non-default cases are handled in a variety of ways: 
    * `pom.xml/maven-remote-resources-plugin` config plays a role in all of this
    * `src/main/appended-resources` - contains copies of license text for
      artifacts that are bundled in Edgent bundles.  For the most part
      this means the Edgent Console jar/war.  These are incorporated
      into a jar by including a declaration like the following in the
      project's pom:
``` xml
        <resource>
          <directory>${project.basedir}/../../src/main/appended-resources/licenses</directory>
          <targetPath>${project.build.directory}/${project.artifactId}-${project.version}/META-INF/licenses</targetPath>
        </resource>
```
    * `src/main/ibm-remote-resources` - contains the NOTICE fragment for
      projects containing IBM contributed code.  Applicable project's
      define the following in their pom to ensure a correct
      NOTICE is included in the project's jar:
``` xml
        <properties>
          <remote-resources-maven-plugin.remote-resources.dir>../../src/main/ibm-remote-resources</remote-resources-maven-plugin.remote-resources.dir>
        </properties>
```
    * `edgent-console-servlets:war` contains bundled code (downloaded and
      incorporated by the build tooling).  It includes the bundled code's
      license text as described above.  Its own LICENSE and NOTICE is
      copied from the respective files in its `src/main/remote-resources/META-INF`.
      <b>There are copies of those in under the java7 platform as well.</b>
    * `edgent-console-server-jetty:jar` bundles the console-servlets war and as such
      requires the same LICENSE/NOTICE/licenses treatment as the servlets war.
      <b>There are copies of its LICENSE/NOTICE in its  `src/main/remote-resources/META-INF`.
      There are copies of those in under the java7 platform as well.</b>
* source-release bundle
    * `src/assembly/source-release.xml` - configuration information
      controlling source-release bundle and distribution bundle names,
      included/excluded files, etc.
* distribution bundles:
  Each platform has a "distribution" project.
  We don't release these bundles hence they aren't obligated to 
  strictly conform to the ASF LICENSE/NOTICE file requirements.
  That said, much of the same information is provided via an
  automatically generated DEPENDENCIES file in the bundle.
  <b>There are copies of the following information in the java7 and android platforms.</b>
  Related, `samples/get-edgent-jars-project` uses the same scheme and has
  its own copies of the files.
  * the name of the bundle is inherited from the source-release bundle's
    configuration file noted above.
  * src/assembly/distribution.xml - additional configuration info
  * src/main/resources/README - source of the file in the bundle
* `utils/edgent-deployment-filter-maven-plugin` - a plugin for eliminating
  the publishing of test related jars during a release.  See EDGENT-440.
  The plugin is built and released separately.  
    

## Testing the Kafka Connector

The kafka connector tests aren't run by default as the connector must
connect to a running Kafka/Zookeeper config.

There are apparently ways to embed Kafka and Zookeeper for testing purposes but
we're not there yet. Contributions welcome!

Setting up the servers is easy.
Follow the steps in the [KafkaStreamsTestManual](connectors/kafka/src/test/java/org/apache/edgent/test/connectors/kafka/KafkaStreamsTestManual.java) javadoc.

Once kafka/zookeeper are running you can run the tests and samples:
```sh
#### run the kafka tests
./mvnw -pl connectors/kafka test '-Dtest=**/*Manual'

# hint: to run just a single test case: '-Dtest=**/KafkaStreamsTestManual#testSimple'

#### run the sample
(cd samples; ./mvnw package -DskipTests)  # build if not already done
cd samples/connectors/scripts/kafka
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
* `android` - Code specific to Android.
* `test` - SVT

Samples are located at https://github.com/apache/incubator-edgent-samples

## Coding Conventions

Placeholder: see [EDGENT-23](https://issues.apache.org/jira/browse/EDGENT-23)

A couple of key items in the mean time:

* Use spaces not hard tabs, indent is 4 spaces
* Don't use wildcard imports
* Don't deliver code with warnings (e.g., unused imports)
* All source files, scripts, etc must have the standard Apache License header
  * the build tooling automatically runs `rat` to check license headers
    and fails if non-conforming files are encountered.
* __Per ASF policy, released source bundles must not contain binaries (e.g., .class, .jar)__
* Per ASF policy, release source and binary bundle LICENSE and NOTICE files must be accurate and up to date, and only bundled 3rd party dependencies whose license meets the ASF licensing requirements can be included. 

### Use of Java 8 features
Edgent's primary development environment is Java 8, to take advantage of lambda expressions
since Edgent's primary API is a functional one.

**However**, in order to support Android (and Java 7), other features of Java 8 are not used in the core
code. Lambdas are translated into Java 7 compatible classes using retrolambda.

Thus for core code and tests that needs to run on Android/Java7:

   * The only Java 8 feature that can be used is lambda expressions
   * Java 8 default & static interface methods cannot be used
   * Java 8 new classes and methods cannot be used
   * Android only: JMX functionality cannot be used

In general, most code is expected to work on Android (but might not yet)
with the exception of these excluded features:

   * Functionality aimed at the developer environment, such as console and development provider
   * Any JMX related code

### Logging

[SLF4J](http://www.slf4j.org) is used for logging and tracing.

Search the code for org.slf4j.LoggerFactory to see a sample of its use.

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

## Renamed from Apache Quarks
Apache Edgent is the new name and the conversion is complete.

Code changes:

  * Package names have the prefix "org.apache.edgent"
  * JAR names have the prefix "edgent"

Users of Edgent will need to update their references to the above.
It's recommended that developers of Edgent create a new workspace instead of
reusing their Quarks workspace.

