See the README.md in the samples root directory for information on building the samples.

The build generated uber jar contains all of the dependent 
Edgent jars and their transitive dependencies.

The desired sample can be run using the run-sample.sh script. e.g.,

```sh
cd utils
./run-sample.sh PeriodicSourceWithMetrics
```

For usage information:

```sh
./run-sample.sh
./run-sample.sh --list
```

If you want to run a sample from the standard jar there are two options:
a) get a local copy of all of the Edgent jars and their dependencies.
   Form a CLASSPATH to the jars and run the sample's main class.
   The get-edgent-jars.sh script can be used to get the jars from
   a maven repository (local or remote).
b) create an application package bundle.  The bundle includes the
   sample(s) jar and a copy of all of the dependent Edgent jars
   and their dependencies.  The package-app.sh script can be
   used to create this bundle.
   The package-app.sh script also creates a run-app.sh script.
   The run-app.sh script configures the CLASSPATH and runs the main class.
