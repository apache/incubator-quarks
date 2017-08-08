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
#Apache Edgent SDK samples.

This file is a work-in-progress.

The following describes Edgent Application development in general
and the Edgent SDK samples in particular.

#Edgent Application Development, Packaging and Execution.

The Edgent SDK/runtime jars are published to maven-central.
  
There are a set of Edgent jars for each supported platform: java8, java7, and android.
The artifact groupId prefix is: org.apache.edgent (for java8),
org.apache.edgent.java7 and org.apache.edgent.android.  e.g.
    org.apache.edgent.api  org.apache.edgent.api.java7

The Edgent API is most easily used by using java8 lambda expressions.
If you only want to deploy your Edgent application to a java8 environment
then your application may use any java8 features it chooses.  You compile
and run against the Edgent java8 jars.

If you want to deploy your Edgent application to a java7 or android
environment, it easiest to write your application using the Edgent APIs
with java8 lambda expressions.  You compile with java8 but constrain 
your application to using java7 features plus java8 lambda expressions.
The Retrolambda tool is used to convert your application's generated 
lass files to java7.
(the java7 and android Edgent jars were created in that manner too)
Your application would then be run against the appropriate
Edgent platform jars. Alternatively you can forgo the use of lambda
expressions and write your application in java7 and compile
and run against the appropriate Edgent platform jars.

For convenience it's easiest to build your Edgent applcation using 
maven-repository-enabled build tooling (e.g., maven, maven-enabled
Eclipse or IntelliJ).  The tooling transparently downloads the 
required Edgent jars from the maven repository.

The supplied Edgent samples poms include support for building for
a java8, java7 or android execution environent. The poms are
configured for the generation of a standard jar as well as an
uber jar.  The poms can be used as a reference in constructing 
your application's pom.

TODO: we would like to provide a maven Edgent Application archetype
that users can use to create an application project template.
Or at least a very simple sample project template (simpler than
the Edgent SDK sample's structure / poms).

If you can't or don't want to use maven-repository-enabled tooling
you will need to get a local copy of the Edgent jars and their
dependencies and add them to your compile classpath.
The Edgent supplied get-edgent-jars.sh tool can be used to
get copies of the jars from a maven repository.


##Packaging and Execution

Edgent doesn't provide any "deployment" mechanisms other than its primitive
"register jar" feature (see the IotProvider javadoc).  Generally, managing
the deployment of application and Edgent jars to edge devices is left to 
others (as an example, the IBM Watson IoT Platform has device APIs to
support "firmware" download/update).

To run your Edgent application on an edge device, your application
jar(s) need to be on the device.  Additionally, the application's 
dependent Edgent jars (and their transitive dependencies) need to
be on the device.  It's unlikely the device will be able to retrieve
the dependencies directly from a remote maven repository such as
maven central.

Here are three options for dealing with this:

a) construct an uber-jar for your application.
   The uber jar contains the application's classes and
   the application's dependent Edgent classes and their
   transitive dependencies.

   The uber jar is a standalone entity containing
   everything that's needed to run your application.

   The Edgent samples poms contain configuration information
   that generates an uber jar in addition to the standard
   application jar.  Eclipse can also export an uber jar.

b) create an application package bundle.
   The bundle contains the application's jar
   and the application's dependent Edgent jars and their
   transitive dependencies.
   The bundle is copied to the device and unpacked.
   A run script forms the appropriate CLASSPATH
   to the package's jars and starts the application.

   The Edgent supplied package-app.sh tool supports this mode.
   Eclipse can also export a similar collection
   of information.

c) separately manage the application's jars and the
   Edgent jars and their dependencies.
   Copy the application's jars to the device.
   Get a copy of the Edgent jars and their dependencies
   onto the device to be shared by any Edgent applications
   that want to use them.

   The Apache Edgent project does not release a
   binary bundle containing all of the Edgent jars
   and their dependencies.  The binary artifacts
   are only released to maven central.

   The Edgent supplied get-edgent-jars.sh tool supports this mode.
   
##get-edgent-jars.sh

The `get-edgent-jars.sh` script copies the Edgent runtime jars and their
dependencies from a local or remote maven repository.

The user may then directly use the jars in CLASSPATH specifications
for Edgent application compilation or execution.
A `classpath.sh` script generated to assist with this.

By default the script retrieves the Edgent java8 platform jars for the
script's default Edgent version.

The script creates and builds a small maven project as
part of its execution.

```sh
get-edgent-jars.sh --version 1.2.0-SNAPSHOT  # retrieve the Edgent 1.2.0-SNAPSHOT java8 jars
This command downloads the Apache Edgent jars and their transitive external dependencies.
The external dependencies have their own licensing term that you should review.
A summary of the external dependencies can be found here <TODO URL>.
Continue? [y/n] y
##### Generating maven project get-edgent-jars-project...
##### Generating dependency decls...
##### Adding dependency decls to pom...
##### Retrieving jars into local maven repo...
...
##### Copying jars...
##### Generating classpath.sh...
##### The Edgent jars are in get-edgent-jars-project/edgent-jars
##### The external jars are in get-edgent-jars-project/ext-jars
##### CLASSPATH may be set by copying get-edgent-jars-project/java8/classpath.sh and using it like:
#####    export CLASSPATH=`classpath.sh path-to-parent-of-edgent-jars-dir`
```

For more usage information:

```sh
get-edgent-jars.sh -h
```

##package-app.sh

The `package-app.sh` script creates an application bundle.
The application bundle can be copied to an edge-device,
unpacked and then used to run the application.

The application bundle contains the application's jar,
the application's dependent Edgent jars (as specified in
the application's pom) and the Edgent jars' dependencies,
and a run-app.sh script.

The application's pom specified Edgent runtime jars and 
their dependencies are retrieved from a local or remote
maven repository.

If the application was built using java8, complied against
the java8 Edgent jars, and the execution environment is
java7 or android, use the appropriate script options
to retrieve the appropriate Edgent platform jars for
execution.

The run-app.sh script configures the CLASSPATH and runs
the application.

E.g.,

```sh
cd MyApp # the project directory
package-app.sh --mainClass com.mycompany.app.MyApp --appjar target/my-app-1.0-SNAPSHOT.jar
##### get the app specific dependencies...
...
##### create target/app-run.sh...
##### create target/app-pkg.tar...
##### Copy target/app-pkg.tar to the destination system"
##### To run the app:"
#####     mkdir app-pkg"
#####     tar xf app-pkg.tar -C app-pkg"
#####     (cd app-pkg; ./app-run.sh)"
```

For more usage information:

```sh
package-app.sh -h
```

#Building the Edgent samples

By default java8 class files are generated.
Java7 and Android platform class files are produced when the appropriate
profile is specified.

```sh
./mvnw clean install  # -Pplatform-java7 or -Pplatform-android as needed
```

A standard jar and uber jar is created for each sample category
in the sample category's target directory: `<category>/target`.


##Running the samples

See the README in each sample category directory.
