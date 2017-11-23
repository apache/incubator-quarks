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

# Runs the Sample JDBC Writer or Reader
#
# ./runjdbcsample.sh writer
# ./runjdbcsample.sh reader

if [ -z "$DERBY_HOME" ]; then
    echo "\$DERBY_HOME not defined."
    exit 1;
fi
if [ ! -f $DERBY_HOME/lib/derby.jar ]; then
    echo "\$DERBY_HOME/lib/derby.jar: file not found"
    exit 1;
fi

export CLASSPATH=${UBER_JAR}:$DERBY_HOME/lib/derby.jar

app=$1; shift
if [ "$app" == "writer" ]; then
    java org.apache.edgent.samples.connectors.jdbc.SimpleWriterApp jdbc.properties
elif [ "$app" == "reader" ]; then
    java org.apache.edgent.samples.connectors.jdbc.SimpleReaderApp jdbc.properties
else
    echo "unrecognized mode '$app'"
    echo "usage: $0 writer|reader"
    exit 1
fi
