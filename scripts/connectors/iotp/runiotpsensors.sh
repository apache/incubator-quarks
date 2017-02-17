#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

edgent=../../..

# Runs IBM Watson IoT Plaform sample.
#
# runiotpsensors.sh path/device.cfg
#
# e.g. runiotpsensors.sh $HOME/device.cfg
#
# This connectors to your IBM Watson IoT Platform service
# as the device defined in the device.cfg.
# The format of device.cfg is the standard one for
# IBM Watson IoT Platform and a sample is in this directory
# (omitting values for the authorization tokens).

# Avoid a paho mqtt class security exception.
# The connector samples jar has dependencies on both the iotp and mqtt connectors
# and those have dependencies on different versions of the paho mqtt jar.
# Ensure the right one is used for this sample.
#
#export CLASSPATH=${edgent}/samples/lib/edgent.samples.connectors.jar
IOTP_MQTT_JAR=`ls ${edgent}/connectors/iotp/ext/org.eclipse.paho.client.mqtt*.jar`
export CLASSPATH=${IOTP_MQTT_JAR}:${edgent}/samples/lib/edgent.samples.connectors.jar

# https://github.com/ibm-watson-iot/iot-java/tree/master#migration-from-release-015-to-021
# Uncomment the following to use the pre-0.2.1 WIoTP client behavior.
#
#USE_OLD_EVENT_FORMAT=-Dcom.ibm.iotf.enableCustomFormat=false

VM_OPTS=${USE_OLD_EVENT_FORMAT}

java ${VM_OPTS} org.apache.edgent.samples.connectors.iotp.IotpSensors $1
