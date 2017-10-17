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

# Edgent Application Development, Packaging and Execution.

To develop Edgent applications you will utilize the 
Edgent SDK/runtime jars and package your application
artifacts for deploying to an edge device for execution.

The Edgent SDK/runtime jars are published to maven-central.
Alternatively, you can build the Edgent SDK yourself from a source release
and the resulting jars will be added to your local maven repository.
  
There are a set of Edgent jars for each supported platform: java8, java7, and android.
The maven artifact groupId prefixes are:

- `org.apache.edgent`  - for java8,
- `org.apache.edgent.java7`
- `org.apache.edgent.android`

e.g., the groupIds for the Edgent API artifacts are
`org.apache.edgent.api` and  `org.apache.edgent.api.java7` for
Java 8 and Java 7 respectively.

See `JAVA_SUPPORT.md` for more information on artifact coordinates, etc.

The Edgent API is most easily used by using Java8 lambda expressions.
If you only want to deploy your Edgent application to a java8 environment
then your application may use any java8 features it chooses.  You compile
and run against the Edgent java8 jars.

If you want to deploy your Edgent application to a java7 or android
environment, it's still easiest to write your application using the Edgent APIs
with java8 lambda expressions.  You compile with java8 but constrain 
your application to using java7 features plus java8 lambda expressions.
The Retrolambda tool is used to convert your application's generated 
class files to java7.
The Edgent java7 and android platform jars were created in that manner too.
Your application would then be run against the appropriate
Edgent platform jars. 

Alternatively you can forgo the use of lambda
expressions and write your application in java7 and compile
and run against the appropriate Edgent platform jars.

For convenience it's easiest to build your Edgent application using 
maven-repository-enabled build tooling (e.g., maven, maven-enabled
Eclipse or IntelliJ).  The tooling transparently downloads the 
required Edgent jars from the maven repository if they aren't
already present in your local maven repository.

The supplied Edgent samples poms include support for building for
a java8, java7 or android execution environment. The poms are
configured for the generation of a standard jar as well as an
uber jar for a sample application.

You can clone the `samples/template` project as a starting point
for your Edgent application.  See `samples/template/README.md`.

TODO: we would like to provide a maven Edgent Application archetype
that users can use to create an application project template.

If you can't or don't want to use maven-repository-enabled tooling
you will need to get a local copy of the Edgent jars and their
dependencies and add them to your compile classpath.  This case
is covered in the following sections.


## Packaging and Execution

Edgent doesn't provide any "deployment" mechanisms other than its primitive
"register jar" feature (see the `IotProvider` javadoc).  Generally, managing
the deployment of application and Edgent jars to edge devices is left to 
others (as an example, the IBM Watson IoT Platform has device APIs to
support "firmware" download/update).

To run your Edgent application on an edge device, your application
jar(s) need to be on the device.  Additionally, the application's 
dependent Edgent jars (and their transitive dependencies) need to
be on the device.  It's unlikely the device will be able to retrieve
the dependencies directly from a remote maven repository such as
maven central.

Here are three options for dealing with this.

### Create an uber-jar for your application

The uber jar is a standalone entity containing
everything that's needed to run your application.

The uber jar contains the application's classes and
the application's dependent Edgent classes and their
transitive dependencies.

The Edgent samples template project's pom and
the Edgent samples poms contain configuration information
that generates an uber jar in addition to the standard
application jar.  Eclipse can also export an uber jar.

### Create an application package bundle

The bundle is a standalone entity containing
everything that's needed to run your application.

The bundle contains the application's jar
and the application's dependent Edgent jars and their
transitive dependencies.
   
The bundle is copied to the device and unpacked.
A run script forms the appropriate `CLASSPATH`
to the package's jars and starts the application.

The Edgent supplied `package-app.sh` tool supports this mode.
Eclipse can also export a similar collection
of information.

### Separately manage the application and Edgent jars

Copy the application's jars to the device.
Get a copy of the Edgent jars and their dependencies
onto the device to be shared by any Edgent applications
that want to use them.

The Apache Edgent project does not release a
binary bundle containing all of the Edgent jars
and their dependencies.  The binary artifacts
are only released to maven central.

See `samples/get-edgent-jars-project` for a tool 
to get a copy of the Edgent jars.

## package-app.sh

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

If the application's execution environment is
java7 or android, use the appropriate script options
to retrieve the appropriate Edgent platform jars for
execution.

The generated run-app.sh script configures the CLASSPATH
and runs the application.

E.g.,

``` sh
cd MyApp # the application's project directory
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

``` sh
package-app.sh -h
```
