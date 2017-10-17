An Edgent Application template project.

Clone this template project to start your Edgent application project.

The project's pom supports

- building for java8, java7 or android execution environments
- building a standard jar and uber jar

Edit the pom for your application.  Adjust it for your application's maven coordinates.
The pom has potential Edgent dependenacies present and commented out.
Include the Edgent Providers, Analytics, Utils, and Connectors used by your application.

Read `../README.md` for general information about Edgent Application development.

The template includes a maven wrapper script to eliminate the need to
manually download and install maven.

# Building the project
```sh
./mvnw clean package  # add -Pplatform-java7 or -Pplatform-android as needed
```

# Running the application

You can copy `app-run.sh` and the generated `target/*-uber.jar` to the 
edge device and then run it
```sh
./app-run.sh
```

# Using package-app.sh with the project

Adjust the main class name and application jar path below for your application.
```sh
PLATFORM=  # add "--platform java7"  or  "--platform android" as appropriate
../package-app.sh $PLATFORM --mainClass com.mycompany.app.TemplateApp --appjar target/my-app-1.0-SNAPSHOT.jar
```

# Using get-edgent-jars-project

If you don't want to use the generated uber jar or `package-app.sh`
approaches, you can copy the application's standard jar and a
`get-edgent-jars-project` generated jar bundle to the edge device.
See `samples/get-edgent-jars-project`.
