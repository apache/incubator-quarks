## Development of Quarks

This describes development of Quarks itself, not how to develop Quarks applications.
*ADD LINK TO GETTING STARTED*

The Quarks community welcomes contributions, please *Get Involved*!
*ADD LINK TO GET INVOLVED*

### Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8 and Linux. 
* Apache Ant 1.9.4: The build uses Ant, the version it has been tested with is 1.9.4. - https://ant.apache.org/
* JUnit 4.10: Java unit tests are written using JUnit, tested at version 4.10. - http://junit.org/
* Jacoco 0.7.5: The JUnit tests have code coverage enabled by default, using Jacoco, tested with version 0.7.5. - http://www.eclemma.org/jacoco/

The Apache Ant `build.xml` files are setup to assume that the Junit and Jacoco jars are copied into `$HOME/.ant/lib`.
```
> ls $HOME/.ant/lib
jacocoagent.jar  jacocoant.jar  junit-4.10.jar
```

These are optional

* Android SDK - Allows building of Android specific modules. Set the environment variable `ANDROID_SDK_PLATFORM` to
the location of the Android platform so that ${ANDROID_SDK_PLATFORM}/android.jar points to a valid jar.


### Building

The primary build process is using Ant, any pull request is expected to maintain
the build success of `clean, all, test`.

The top-level Ant file `quarks/build.xml` has these main targets:

* `all` (default) : Build all code and Javadoc into `target`. The build will fail on any code error or Javadoc warning or error.
* `clean` : Clean the project
* `test` : Run the JUnit tests, if any test fails the test run stops. Takes around five minutes.
* `reports` : Generate JUnit and Code Coverage reports, use after executing the `test` target.
* `release` : Build a release bundle, that includes subsets of the Quarks jars that run on Java 7 (`target/java7`) and Android (`target/android`).

The build process has been tested on Linux and MacOSX.

To build on Windows probably needs some changed, please get involved and contribute them!

### Test reports

Running the reports target produces two reports:
* `reports/junit/index.html` - JUnit test report
* `reports/coverage/index.html` - Code coverage report.

### Code Layout

The code is broken into a number of projects and modules within those projects defined by directories under `quarks`.
Each top level directory is a project and contains one or more modules:

* `api` - The APIs for Quarks. In general there is a strict split between APIs and
implementations to allow multiple implementations of an API, such as for different device types or different approaches.
* `spi` - Common implementation code that may be shared by multiple implementations of an API.
There is no requirement for an API implementation to use the provided spi code.
* `runtime` - Implementations of APIs for executing Quarks applications at runtime.
Initially a single runtime is provided, `etiao` - *EveryThing Is An Oplet* -
A micro-kernel that executes Quarks applications by being a very simple runtime where all
functionality is provided as *oplets*, execution objects that process streaming data.
So a Quarks application becomes a graph of connected oplets, and items such as fan-in or fan-out,
metrics etc. are implemented by inserting additional oplets into the application's graph.
* `providers` - Providers bring the Quarks modules together to allow Quarks applications to
be developed and run.
* `connectors` - Connectors to files, HTTP, MQTT, Kafka, JDBC, etc. Connectors are modular so that deployed
applications need only include the connectors they use, such as only MQTT. Quarks applications
running at the edge are expected to connect to back-end systems through some form of message-hub,
such as an MQTT broker, Apache Kafka, a cloud based IoT service, etc.
* `apps` - Applications for use in an Internet of Things environment.
* `analytics` - Analytics for use by Quarks applications.
* `utils` - Optional utilities for Quarks applications.
* `console` - Development console that allows visualization of the streams within an Quarks application during development.
* `samples` - Sample applications, from Hello World to some sensor simulation applications.
* `android` - Code specific to Android.
* `test` - SVT

### Use of Java 8 features
Quarks primary development environment is Java 8, to take advantage of lambda expressions
since Quarks' primary api is a functional one.

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

### The ASF / Github Integration

The Quarks code is in ASF resident git repositories.  
The repositories are mirrored on github:

    https://github.com/apache/incubator-quarks

Use of the normal github workflow brings benefits to the team including 
lightweight code reviewing, automatic regression tests, etc 
for both committers and non-committers.  

For a description of the github workflow see:

    https://guides.github.com/activities/hello-world/

In summary:
    * fork the incubator-quarks github repository
    * clone your fork, use lightweight per-task branches, and commit / push changes to your fork
      * name your branch quarks-xxxx - typically xxxx is a Jira ticket number
    * when ready, create a pull request.  Committers will get notified.
      * include QUARKS-XXXX in the name of your pull request
      * for early preview / feedback, create a pull request with [WIP] in the title.  
        Committers won’t consider it for merging until after [WIP] is removed.

Since the github incubator-quarks repository is a mirror of the ASF repository,
the usual github based merge workflow for committers isn’t supported.

Committers can use one of several ways to ultimately merge the pull request
into the repo at the ASF. One way is described here:

    http://mail-archives.apache.org/mod_mbox/incubator-quarks-dev/201603.mbox/%3C1633289677.553519.1457733763078.JavaMail.yahoo%40mail.yahoo.com%3E

Notes with the above PR merge directions:
  * seems like it should be using  `git merge --squash pr-<N>`
  * use an https url unless you have a ssh key setup at github:
    `git remote add mirror https://github.com/apache/incubator-quarks.git`

### Using Eclipse

The repo contains Eclipse project definitions for the top-level directories that
contain code, such as api, runtime, connectors.

Using the plugin Eclipse Git Team Provider allows you to import these projects
into your Eclipse workspace directly from your fork.

1. From the File menu select Import
1. From the Git folder select Projects from Git and click Next
1. Select Clone URI and click Next
1. Under Location enter the URI of your fork (the other fields will be filled in automatically) and click Next
1. If required, enter your passphrase to unlock you ssh key
1. Select the branch, usually master and click Next
1. Set the directory where your local clone will be stored and click Next (the directory quarks under this directory is where you can build and run tests using the Ant targets)
1. Select Import existing Eclipse projects and click Next
1. In the Import Projects window, make sure that the Search for nested projects checkbox is selected. Click Finish to bring in all Quarks projects

Note. Specifics may change depending on your version of Eclipse or the Eclipse Git Team Provider.

