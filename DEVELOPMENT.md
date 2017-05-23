## Development of Apache Edgent

*Apache Edgent is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Incubator PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.*

See [README.md](README.md) for high-level information about Apache Edgent.

This document describes development of Apache Edgent itself, not how to develop Edgent applications.
 * See http://edgent.incubator.apache.org/docs/edgent-getting-started for getting started using Edgent

The Edgent community welcomes contributions, please *Get Involved*!
 * http://edgent.incubator.apache.org/docs/community
 
If you are interested in developing a new connector see [Writing Connectors for Edgent Applications](https://cwiki.apache.org/confluence/display/EDGENT/Writing+Connectors+For+Edgent+Applications) 

## Switched from Ant to Gradle

See the updated _Building_ and _Using Eclipse_ sections below.
The Ant tooling is no longer functional.

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

* Java 8 - The development setup assumes Java 8 and Linux.
* gradle - (https://gradle.org/) only if building from a source release bundle

All Edgent runtime development is done using Java 8.  JARs for Java 7 and Android
platforms are created as described below.

## Building a Binary Release Bundle

Building from a source release bundle (lacking a `./gradlew`) requires
performing a one-time bootstrap step using an installed version of gradle:
``` sh
$ gradle          # one time gradle build bootstrap setup.
```

Building an Edgent binary release bundle:
``` sh
$ ./gradlew release
```

The build reports the location of the binary distribution bundle that can then
be unpacked and used in building applications.

See [Getting Started](https://edgent.apache.org/docs/edgent-getting-started)
for information on using the binary release bundle.

## Building for Edgent Runtime Development

The primary build process is using [Gradle](https://gradle.org/),
any pull request is expected to maintain the build success of `clean, assemble, test`.

The Gradle wrapper `edgent/{gradlew,gradlew.bat}` should be used.
The wrapper ensures the appropriate version of Gradle is used and it
will automatically download it if needed, e.g.:
``` sh
$ ./gradlew --version
$ ./gradlew clean build
```

The Gradle tooling:
- Creates release images under `<edgent>/build/release-edgent`
- Creates build artifacts under `<edgent>/build/distributions` and `<edgent>/<project>/build`


The top-level Gradle file is `edgent/build.gradle` and it contains several
unique tasks:

* `wrapper` (default) : one-time bootstrap processing for use when building from a source release bundle 
* `assemble` : Build all code and Javadoc into `build\distributions`. The build will fail on any code error or Javadoc warning or error.
* `all` : Synonym for `assemble`
* `build` : Essentially like "assemble test reports"
* `clean` : Clean the project
* `test` : Run the JUnit tests, if any test fails the test run stops.  Use `--continue` to not stop on the first failure.
  * Use a project test task and optionally the `--tests` option to run a subset of the tests.  Multiple `--tests` options may be specified following each test task.
    * `$ ./gradlew <project>:test`
    * `$ ./gradlew <project>:test --tests '*.SomeTest'`
    * `$ ./gradlew <project>:test --tests '*.SomeTest.someMethod'`
  * Use the `cleanTest` task to force rerunning a previously successful test task (without forcing a rerun of all of the task's dependencies):
    * `$ ./gradlew [<project>:]cleanTest [<project>:]test`
* `reports` : Generate JUnit and Code Coverage reports in `build\distributions\reports`. Use after executing the `test` target.
  * `reports\tests\overview-summary.html` - JUnit test report
  * `reports\coverage\index.html` - Code coverage report
* `release` : Build release bundles in `build/release-edgent`, that includes subsets of the Edgent JARs that run on Java 7 (`build/distributions/java7`) and Android (`build/distributions/android`). By default, SNAPSHOT bundles are created.  Specify `-Dedgent.snapshotId=""` to create bundles for a formal release.
* `rat` : run the Apache Release Analysis Tool (license checking).
* `signAll` : Sign the release bundles in `build/release-edgent` (first run `release`).  You will be promoted for your PGP code signing key's ID, the location of the keyring file, and the secret key password.  Default response values may be set with environment variables:
  * `GPG_ID` - the code signing key's ID (e.g., D0F56CAD)
  * `GPG_SECRING` - path to the secret key's keyring file

The build process has been tested on Linux and macOS.

To build on Windows probably needs some changes, please get involved and contribute them!

## Continuous Integration

When a pull request is opened on the GitHub mirror site, the Travis CI service runs a full build.

The latest build status for the project's branches can be seen at: https://travis-ci.org/apache/incubator-edgent/branches

The build setup is contained in `.travis.yml` in the project root directory.
It includes:
* Building the project
* Testing on Java 8 and Java 7
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

## Java 7 and Android
Java 7 and Android target platforms are supported through use of
retrolambda to convert Edgent Java8 JARs to Java7 JARs.

Building a release (`./gradlew release`) produces three sets of JARs under
* build/distributions/java8 - Java 8 SE
* build/distributions/java7 - Java 7 SE
* build/distributions/android - Android

See [JAVA_SUPPORT.md](JAVA_SUPPORT.md) for which Edgent capabilities / JARs are supported
for each environment.

### Adding Edgent Runtime JARs to Java 7 & Android

The Gradle tooling uses some Ant tooling to create the Java 7 and Android platform JARs.

Java 7 Edgent runtime JARs are created using `platform/java7/build.xml`. Adding a JAR just requires:
* Adding it to target `retro7.edgent` - Copy entry for an existing JAR.
* Adding any tests for it to targets `test7.setup` and `test7.run` - Copy entry for an existing JAR.

Any Java 7 JAR is automatically included in Android unless it is explictly excluded in `platform/android/build.xml`.

## Test reports

Running the `reports` target produces two reports:
* `builds/distributions/reports/tests/index.html` - JUnit test report
* `builds/distributions/reports/coverage/index.html` - Code coverage report.

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
./gradlew connectors:kafka:test --tests '*.*Manual'

#### run the sample
cd java8/scripts/connectors/kafka
cat README
./runkafkasample.sh sub
./runkafkasample.sh pub
```

## Testing the JDBC Connector

The JDBC connector tests are written to run against Apache Derby 
as the backing dbms and the derby jar needs to be on the classpath.
The tests are skipped if derby can't be loaded.
The test harness adds $DERBY_HOME/db/lib/derby.jar to the classpath.
See [JdbcStreamsTest](connectors/jdbc/src/test/java/org/apache/edgent/test/connectors/jdbc/JdbcStreamsTest.java) 
for more info but the following should suffice:

```sh
export DERBY_HOME=$JAVA_HOME/db

#### if JAVA_HOME isn't set - e.g., on OSX...
export DERBY_HOME=`/usr/libexec/java_home`/db
```

Once DERBY_HOME is set the tests and samples can be run as follows
```sh
#### run the jdbc tests
./gradlew connectors:jdbc:test

#### run the sample
cd java8/scripts/connectors/jdbc
cat README
./runjdbcsample.sh writer
./runjdbcsample.sh reader
```

## Testing Edgent with Java7

All of the standard build system _tasks_ above must be run with
`JAVA_HOME` set to use a Java8 VM.

As noted above, the `release` task includes generation of Java7
compatible versions of the Edgent JARs. After the release task has been run,
Edgent may be tested in a Java7 context using some special _test7_ tasks.

See [JAVA_SUPPORT](JAVA_SUPPORT.md) for information about what
Edgent features are supported in the different environments.

``` sh
 # pave the way for useful report generation at the end
$ ./gradlew cleanTest
$ ./gradlew reports

 # run with JAVA_HOME/PATH set for Java8
$ ./gradlew test7Compile  # compile the Edgent tests to operate in a Java7 environment

$ sh   # muck with EVs for Java7 in a subshell
  $ export JAVA_HOME=`/usr/libexec/java_home -v 1.7`   # on OSX
  $ export PATH=$JAVA_HOME/bin:$PATH
  $ ./gradlew test7Run      # run the tests with a Java7 VM
  $ exit

 # run with JAVA_HOME set for Java8
$ ./gradlew test7Reports  # generate the JUnit and coverage reports
```

## Publish to Maven Repository

### Pushing Snapshots

Snapshots may be published to the ASF's internal Nexus instance for developer testing.  To publish these, ensure that you have
a `~/.gradle/gradle.properties` with the values for `asfNexusUsername` and `asfNexusPassword` filled in.

```
asfNexusUsername: mrapache
asfNexusPassword: apache4life
```

Then you can simply run `gradlew publish` to build and publish your changes.

### Pushing Releases

Initial support for publishing to a local Maven repository has been added.
Use the following to do the publish.

``` sh
$ ./gradlew publishToMavenLocal
```

The component JARs / WARs are published as well as their sources.
The published groupId is `org.apache.edgent`. The artifactIds match the
names of the JARs in the target-dir / release tgz.

For example: `org.apache.edgent:edgent.api.topology:0.4.0`


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
* Per ASF policy, released source bundles must not contain binaries (e.g., .class, .jar)
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

The Edgent Git repository contains Eclipse project definitions for the
top-level directories that contain code, such as api, runtime, connectors.

**The Git repository does not include the 3rd party JARs that Edgent depends on,
and Eclipse builds of Edgent projects will fail until a Gradle task is run
to make them available in your workspace.  See the steps below.**

Using the Eclipse Git Team Provider plugin allows you to import these projects
into your Eclipse workspace directly from your fork.

1. From the *File* menu, select *Import...*
2. From the *Git* folder, select *Projects from Git* and click *Next*
3. In the *Select Repository Source* window, select one of the following:
  - *Existing local repository* if you have already cloned the project to your machine. Click *Next*.
    + Click *Add* and browse to the local Git repository directory
    + Select the checkbox next to the repository and click *Finish*
    + In the *Select a Git repository* window, choose *incubator-edgent* and click *Next*
  - *Clone URI* to clone the remote repository. Click *Next*.
    + In the *Location* section, enter the URI of your fork in the *URI* field (e.g., `git@github.com:<username>/incubator-edgent.git`). The other fields will be populated automatically. Click *Next*. If required, enter your passphrase.
    + In the *Source Git Repository* window, select the branch (usually `master`) and click *Next*
    + Specify the directory where your local clone will be stored and click *Next*. Note: You can build and run tests using the Gradle targets in this directory.
4. In the *Select a wizard to use for importing projects* window, choose *Import existing Eclipse projects* and click *Next*
5. In the *Import Projects* window, ensure that the *Search for nested projects* checkbox is selected. Click *Finish* to bring in all the Edgent projects. **Expect build failures until you...**
6. Run the following Gradle task in your clone directory to make all of the dependant 3rd party JARs available to Eclipse. When the command finishes, refresh your Eclipse workspace (*File* > *Refresh*) so that it rebuilds the projects.
``` sh
$ ./gradlew setupExternalJars
```

The `_edgent` project exists to make the top-level artifacts such as
`build.gradle` manageable via Eclipse.  Unfortunately, folders for the
other projects (e.g., `api`) also show up in the `_edgent` folder, and
are best ignored.

Builds and JUnit testing of Edgent in Eclipse are independent from the artifacts
generated by the Gradle build tooling.  Neither environment is affected by
the other. This is not ideal, but it's where things are at at this time.
Both sets of tooling can be, and typically are, used in the same workspace.

Note: Specifics may change depending on your version of Eclipse or the Eclipse Git Team Provider.
