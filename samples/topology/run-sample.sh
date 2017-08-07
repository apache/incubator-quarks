#!/usr/bin/env bash
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

USAGE="usage: `basename $0` [--list] simple-main-class-name [sample-args]"

#UBER_JAR=target/edgent-samples-topology-1.2.0-SNAPSHOT-uber.jar
UBER_JAR=target/edgent-samples-topology-*-uber.jar

SAMPLE_PACKAGE=org.apache.edgent.samples.topology
SAMPLES=`cat <<EOF 
CombiningStreamsProcessingResults
DevelopmentMetricsSample
DevelopentSample
DevelopmentSampleJobMXBean
HelloEdgent
JobEventsSample
JobExecution
PeriodicSource
SensorsAggregates
SimpleFilterTransform
SplitWithEnumSample
TerminateAfterNTuples
EOF
`

if [ "$1" = "" ] ; then
  echo $USAGE
  exit 1
fi
if [ "$1" = "--list" ] ; then
  echo ${SAMPLES}
  exit 0
fi

SAMPLE_NAME=$1
shift

if [ $# != 0 ] ; then
  echo $USAGE
  exit 1
fi

java -cp ${UBER_JAR} "${SAMPLE_PACKAGE}.${SAMPLE_NAME}" "$*"

