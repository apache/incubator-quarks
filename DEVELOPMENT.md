## Development of Apache Edgent

*Apache Edgent is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Incubator PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.*

This describes development of Apache Edgent itself, not how to develop Edgent applications.
 * See http://edgent.incubator.apache.org/docs/edgent-getting-started for getting started using Edgent

The Edgent community welcomes contributions, please *Get Involved*!
 * http://edgent.incubator.apache.org/docs/community

## Switching from Ant to Gradle

We are getting close to being able to permanently switch over to a gradle based
build tooling [EDGENT-139](https://issues.apache.org/jira/browse/EDGENT-139).

For a short time, both the gradle and ant build tooling environments will be functional.
The gradle build tooling and the ant tooling create artifacts in
different places in the workspace so they do not interfere with each other.

Once the 3rd party jars have been removed from the Edgent repository, the
ant based tooling will no longer work.

**See _Using Eclipse_ below for a gradle task that must be run to make the
3rd party jars available to an Eclipse Edgent runtime development environment.**

Gradle tooling:
- creates release images under `<edgent>/build/release-edgent`
- creates build artifacts under `<edgent>/build/distributions` and `<edgent>/<project>/build`

Ant tooling:
- creates release images under `<edgent>/release-edgent`
- creates build artifacts under `<edgent>/target` and `<edgent>/<project>/{classes,test.classes}`

Eclipse:
- creates build artifacts under `<edgent>/<project>/bin`


Timeline:
- Sep 27 - Start using the gradle build tooling for developing Edgent.
           See below for using gradle.
- Sep 30 - Eclipse based Edgent runtime development doesn't use 3rd party jars in the repo
- Oct 01 - switch travis-ci builds to using the gradle tooling
- Nov 01 - remove the 3rd party jars from the Edgent repository.
           The ant build tooling and "old" Eclipse build environment no
           longer function.

It's recommended that developers of Edgent create a new workspace instead of
reusing current ant-based Edgent workspaces.

## Renamed from Apache Quarks
Apache Edgent is the new name and the conversion is complete.

Code changes:
  * package names have the prefix "org.apache.edgent"
  * jar names have the prefix "edgent"
  
Users of Edgent will need to update their references to the above.
It's recommended that developers of Edgent create a new workspace instead of
reusing their Quarks workspace.

### Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8 and Linux. 

These are optional

* Android SDK - Allows building of Android specific modules. Set the environment variable `ANDROID_SDK_PLATFORM` to
the location of the Android platform so that ${ANDROID_SDK_PLATFORM}/android.jar points to a valid jar.


### Building

The primary build process is using [Gradle](https://gradle.org/), 
any pull request is expected to maintain the build success of `clean, assemble, test`.

The Gradle wrapper `edgent/{gradlew,gradlew.bat}` should be used.
The wrapper ensures the appropriate version of gradle is used and it
will automatically download it if needed.  e.g.:
``` sh
$ ./gradlew --version
$ ./gradlew clean build
```

The top-level Gradle file is `edgent/build.gradle` and it contains several
unique tasks:

* `assemble` (default) : Build all code and Javadoc into `build\distributions`. The build will fail on any code error or Javadoc warning or error.
* `all` : synonym for `assemble`
* `build` : essentially like "assemble test reports"
* `clean` : Clean the project
* `test` : Run the JUnit tests, if any test fails the test run stops.  Use `--continue` to not stop on the first failure.
  * use a project test task and optionally the `--tests` option to run a subset of the tests.  Multiple `--tests` options may be specified following each test task.
    * `$ ./gradlew <project>:test`
    * `$ ./gradlew <project>:test --tests '*.SomeTest'`
    * `$ ./gradlew <project>:test --tests '*.SomeTest.someMethod'`
  * use the `cleanTest` task to force rerunning a previously successful test task (without forcing a rerun of all of the task's dependencies):
    * `$ ./gradlew [<project>:]cleanTest [<project>:]test`
* `reports` : Generate JUnit and Code Coverage reports in `build\distributions\reports`. Use after executing the `test` target. 
  * `reports\tests\overview-summary.html` - JUnit test report
  * `reports\coverage\index.html` - Code coverage report
* `release` : Build a release bundle in `build/release-edgent`, that includes subsets of the Edgent jars that run on Java 7 (`build/distributions/java7`) and Android (`build/distributions/android`).

The build process has been tested on Linux and MacOSX.

To build on Windows probably needs some changes, please get involved and contribute them!


**Continuous Integration**

When a pull request is opened on the GitHub mirror site, the Travis CI service runs a full build.

The latest build status for the project's branches can be seen at: https://travis-ci.org/apache/incubator-edgent/branches

The build setup is contained in `.travis.yml` in the project root directory.
It includes:
* Building the project
* Testing on Java 8 and Java 7
Not all tests may be run, some tests are skipped due to timing issues or if excessive setup is required.

If your test may randomly fail because for example it depends on publicly available test services, 
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

### Test reports

Running the `reports` target produces two reports:
* `builds/distributions/reports/tests/index.html` - JUnit test report
* `builds/distributions/reports/coverage/index.html` - Code coverage report.

### Testing Edgent with Java7

All of the standard build system _tasks_ above must be run with
`JAVA_HOME` set to use a Java8 VM.

As noted above, the `release` task includes generation of Java7 
compatible versions of the Edgent jars. After the release task has been run,
Edgent may be tested in a Java7 context using some special _test7_ tasks.

See [JAVA_SUPPORT](JAVA_SUPPORT.md) for information about what 
Edgent features are supported in the different environments.

``` sh
 # run with JAVA_HOME set for Java8
$ ./gradlew test7Compile  # compile the Edgent tests to operate in a java7 environment

 # run with JAVA_HOME set for Java7
$ ./gradlew test7Run      # run the tests with a java7 VM

 # run with JAVA_HOME set for Java8
$ ./gradlew test7Reports  # generate the junit and coverage tests
```

[DEPRECATED] using the Ant tooling
``` sh
 # run with JAVA_HOME set for Java8
$ ant -buildfile platform/java7 test7.setup  # compile the Edgent tests to operate in a java7 environment

 # run with JAVA_HOME set for Java7
$ ant -buildfile platform/java7 test7.run    # run the tests with a java7 VM
```

#### Publish to Maven Repository

Initial support for publishing to a local Maven repository has been added.
Use the following to do the publish.

``` sh
$ ./gradlew publishToMavenLocal
```

The component jars / wars are published as well as their sources.
The published groupId is `org.apache.edgent`. The artifactIds match the
names of the jars in the target-dir / release tgz.

E.g. `org.apache.edgent:edgent.api.topology:0.4.0`


### [DEPRECATED] Ant specific Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Apache Ant 1.9.4: The build uses Ant, the version it has been tested with is 1.9.4. - https://ant.apache.org/
* JUnit 4.10: Java unit tests are written using JUnit, tested at version 4.10. - http://junit.org/
* Jacoco 0.7.5: The JUnit tests have code coverage enabled by default, using Jacoco, tested with version 0.7.5. - http://www.eclemma.org/jacoco/

The Apache Ant `build.xml` files are setup to assume that the Junit and Jacoco jars are copied into `$HOME/.ant/lib`.
``` sh
$ ls $HOME/.ant/lib
jacocoagent.jar  jacocoant.jar  junit-4.10.jar
```

### [DEPRECATED] Building with Ant

Any pull request is expected to maintain the build success of `clean, all, test`.

The top-level Ant file `edgent/build.xml` has these main targets:

* `all` (default) : Build all code and Javadoc into `target`. The build will fail on any code error or Javadoc warning or error.
* `clean` : Clean the project
* `test` : Run the JUnit tests, if any test fails the test run stops.
* `reports` : Generate JUnit and Code Coverage reports, use after executing the `test` target.
* `release` : Build a release bundle, that includes subsets of the Edgent jars that run on Java 7 (`target/java7`) and Android (`target/android`).

The build process has been tested on Linux and MacOSX.


### Code Layout

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

### Coding Conventions

Placeholder: see [EDGENT-23](https://issues.apache.org/jira/browse/EDGENT-23)

A couple of key items in the mean time:
* use spaces not hard tabs, indent is 4 spaces
* don't use wildcard imports
* don't deliver code with warnings (e.g., unused imports)

### Logging

[SLF4J](http://www.slf4j.org) is used for logging and tracing.

Search the code for org.slf4j.LoggerFactory to see a sample of its use.

### Use of Java 8 features
Edgent's primary development environment is Java 8, to take advantage of lambda expressions
since Edgent's primary api is a functional one.

**However** in order to support Android (and Java 7) other features of Java 8 are not used in the core
code. Lambdas are translated into Java 7 compatible classes using retrolambda.

Thus:
* For core code that needs to run on Android:
   * the only Java 8 feature that can be used is lambda expressions.
   * JMX functionality cannot be used.
* For test code that tests core code that runs on Android
   * Java 8 lambda expressions can be used
   * Java 8 default & static interface methods
   * Java 8 new classes and methods cannot be used.
   
In general most code is expected to work on Android (but might not yet) with the exception:
* Functionality aimed at the developer environment, such as console and development provider
* Any JMX related code.

### The ASF / GitHub Integration

The Edgent code is in ASF resident git repositories:

    https://git-wip-us.apache.org/repos/asf/incubator-edgent.git

The repositories are mirrored on GitHub:

    https://github.com/apache/incubator-edgent

Use of the normal GitHub workflow brings benefits to the team including
lightweight code reviewing, automatic regression tests, etc 
for both committers and non-committers.  

For a description of the GitHub workflow see:

    https://guides.github.com/introduction/flow/
    https://guides.github.com/activities/hello-world/

In summary:
* fork the incubator-edgent GitHub repository
* clone your fork, use lightweight per-task branches, and commit / push changes to your fork
  * descriptive branch names are good. You can also include a reference
    to the Jira issue.  e.g., mqtt-ssl-edgent-100 for issue EDGENT-100
* when ready, create a pull request.  Committers will get notified.
  * include EDGENT-XXXX (the Jira issue) in the name of your pull request
  * for early preview / feedback, create a pull request with [WIP] in the title.  
    Committers won’t consider it for merging until after [WIP] is removed.

Since the GitHub incubator-edgent repository is a mirror of the ASF repository,
the usual GitHub based merge workflow for committers isn’t supported.

Committers can use one of several ways to ultimately merge the pull request
into the repo at the ASF. One way is described here:

* http://mail-archives.apache.org/mod_mbox/incubator-edgent-dev/201603.mbox/%3C1633289677.553519.1457733763078.JavaMail.yahoo%40mail.yahoo.com%3E

Notes with the above PR merge directions:
  * use an https url unless you have a ssh key setup at GitHub:
    `$ git remote add mirror https://github.com/apache/incubator-edgent.git`

### Using Eclipse

The Edgent git repository contains Eclipse project definitions for the
top-level directories that contain code, such as api, runtime, connectors.

**The git repository does not include the 3rd party jars that Edgent depends on
and Eclipse builds of Edgent projects will fail until a gradle task is run
to make them available in your workspace.  See the steps below.**

Using the plugin Eclipse Git Team Provider allows you to import these projects
into your Eclipse workspace directly from your fork.

1. From the File menu select Import
1. From the Git folder select Projects from Git and click Next
1. Select Clone URI and click Next
1. Under Location enter the URI of your fork (the other fields will be filled in automatically) and click Next
1. If required, enter your passphrase to unlock you ssh key
1. Select the branch, usually master and click Next
1. Set the directory where your local clone will be stored and click Next (the directory `edgent` under this directory is where you can build and run tests using the Gradle targets)
1. Select Import existing Eclipse projects and click Next
1. In the Import Projects window, make sure that the Search for nested projects checkbox is selected. Click Finish to bring in all Edgent projects.  **Expect build failures until you...**
1. Run a gradle task to make all of the dependant 3rd party jars available to Eclipse. When the command finishes, refresh your Eclipse workspace so that it rebuilds the projects.
``` sh
$ ./gradlew setupExternalJars
```

The project `_edgent` exists to make the top level artifacts such as 
`build.xml` manageable via Eclipse.  Unfortunately folders for the
other projects (e.g., `api`) also show up in the `_edgent` folder and
are best ignored.

Builds and junit testing of Edgent in Eclipse is independent from the artifacts
generated by the gradle/ant build tooling.  Neither environment is affected by
the other. This is not ideal but it's where things are at at this time.
Both sets of tooling can be, and typically are, used in the same workspace.

Note. Specifics may change depending on your version of Eclipse or the Eclipse Git Team Provider.
