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

quarks=../../..

# Runs IBM Watson IoT Plaform sample.
#
# runiotfsensors.sh path/device.cfg
#
# e.g. runiotfsensors.sh $HOME/device.cfg
#
# This connectors to your IBM Watson IoT Platform service
# as the device defined in the device.cfg.
# The format of device.cfg is the standard one for
# IBM Watson IoT Platform and a sample is in this directory
# (omitting values for the authorization tokens).

export CLASSPATH=${quarks}/samples/lib/quarks.samples.connectors.jar

java quarks.samples.connectors.iotf.IotfSensors $1
