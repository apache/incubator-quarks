# edgent.IoTFRangeSensor

See the Recipe this was created for [here](https://developer.ibm.com/recipes/tutorials/apache-quarks-on-pi-to-watson-iot-foundation/).  If that link doesn't work, try [here](https://developer.ibm.com/recipes/tutorials/apache-edgent-on-pi-to-watson-iot-foundation/). 

## Requirements: 
* You must have Pi4J installed on the device (if you are running outside of a Raspberry Pi, you will have to download the JARs and include them in your classpath)
* You must have Edgent downloaded and built
* You will need to have an HC-SR04 Range sensor hooked up with your EchoPin set to pin 18 and your TripPin at pin 16 (see these instructions on hardware setup: http://www.modmypi.com/blog/hc-sr04-ultrasonic-range-sensor-on-the-raspberry-pi). To use a simulated sensor, pass in true as your second argument. 
* You will need to have an LED hooked up to pin 12 (See these instructions to set up an LED, however use pin 12 as your control pin: https://projects.drogon.net/raspberry-pi/gpio-examples/tux-crossing/gpio-examples-1-a-single-led/). To use a simulated LED, pass in true as your third argument. 
* You will need to have your device registered with Watson IoTF and a device.cfg file, or you can use a quickstart version by passing in "quickstart" as your first argument.
 
To compile, export your Edgent install and PI4J libraries (on Raspberry Pi, the default Pi4J location is `/opt/pi4j/lib`):

`$ EDGENT=<edgent-root-dir>`

`$ export PI4J_LIB=<Pi4J-libs>  # directory where pi4j-core.jar resides`

`$ cd $EDGENT`

`$ ./gradlew  # builds the range sensor sample only if the PI4J_LIB environment variable is set`

To run: 
 
`$ CP=$EDGENT/build/distributions/java8/samples/lib/edgent.samples.scenarios.jar`

`$ MAIN=org.apache.edgent.samples.scenarios.iotp.range.sensor.IotpRangeSensor`


`$ java -cp $CP $MAIN <device cfg file> <simulatedSensor?> <simulatedLED?>`

To run with a device.cfg file, range sensor, and LED:

`$ java -cp $CP $MAIN device.cfg false false`

To run in fully simulated mode (no sensors and using IoTF quickstart): 

`$ java -cp $CP $MAIN quickstart true true`