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

CONNECTOR_SAMPLES_DIR=../..

UBER_JAR=`echo ${CONNECTOR_SAMPLES_DIR}/target/edgent-samples-connectors-*-uber.jar`

# Runs IBM Watson IoT Platform IotpAppClient sample.
#
# run-iotp-app-client.sh [useGW] <app-cfg-path>  # see iotp-app-client.cfg
#
# Connects to WIoTP and sends device commands to the 
# IotpDeviceSample or IotpGWDeviceSample device samples.
#
# This connects to your IBM Watson IoT Platform service
# as the Application defined in a application config file.
# The file format is the standard one for IBM Watson IoT Platform.
#
# Note, the config file also contains some additional information for this application.
# A sample iot-app-client.cfg is in the scripts/connectors/iotp directory.


export CLASSPATH=${UBER_JAR}

# https://github.com/ibm-watson-iot/iot-java/tree/master#migration-from-release-015-to-021
# Uncomment the following to use the pre-0.2.1 WIoTP client behavior.
#
#USE_OLD_EVENT_FORMAT=-Dcom.ibm.iotf.enableCustomFormat=false

VM_OPTS=${USE_OLD_EVENT_FORMAT}

java ${VM_OPTS} org.apache.edgent.samples.connectors.iotp.IotpAppClient $* 
