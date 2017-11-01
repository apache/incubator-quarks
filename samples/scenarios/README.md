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
# edgent.IoTFRangeSensor

See the Recipe this was created for [here](https://developer.ibm.com/recipes/tutorials/apache-quarks-on-pi-to-watson-iot-foundation/).  If that link doesn't work, try [here](https://developer.ibm.com/recipes/tutorials/apache-edgent-on-pi-to-watson-iot-foundation/). 

See the README.md in the samples root directory for information on building the samples.

The build generated uber jar contains all of the dependent 
Edgent jars and their transitive dependencies.

The desired sample can be run using the run-sample.sh script. e.g.,

```sh
cd scenarios
./run-sample.sh IotpRangeSensor quickstart true true  # see below
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


## Requirements: 
* You will need to have an HC-SR04 Range sensor hooked up with your EchoPin set to pin 18 and your TripPin at pin 16 (see these instructions on hardware setup: http://www.modmypi.com/blog/hc-sr04-ultrasonic-range-sensor-on-the-raspberry-pi). To use a simulated sensor, pass in true as your second argument. 
* You will need to have an LED hooked up to pin 12 (See these instructions to set up an LED, however use pin 12 as your control pin: https://projects.drogon.net/raspberry-pi/gpio-examples/tux-crossing/gpio-examples-1-a-single-led/). To use a simulated LED, pass in true as your third argument. 
* You will need to have your device registered with Watson IoTF and a device.cfg file, or you can use a quickstart version by passing in "quickstart" as your first argument.


`./run-sample.sh IotpRangeSensor <device cfg file> <simulatedSensor?> <simulatedLED?>`

To run with a device.cfg file, range sensor, and LED:

`./run-sample.sh IotpRangeSensor  device.cfg false false`

To run in fully simulated mode (no sensors and using IoTF quickstart): 

`./run-sample.sh IotpRangeSensor  quickstart true true`
