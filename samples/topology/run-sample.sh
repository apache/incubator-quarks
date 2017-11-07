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

CATEGORY=topology

UBER_JAR=target/edgent-samples-${CATEGORY}-*-uber.jar

SAMPLE_PACKAGE_BASE=org.apache.edgent.samples.${CATEGORY}
SAMPLES_FQ=`cat <<EOF 
${SAMPLE_PACKAGE_BASE}.CombiningStreamsProcessingResults
${SAMPLE_PACKAGE_BASE}.DevelopmentMetricsSample
${SAMPLE_PACKAGE_BASE}.DevelopmentSample
${SAMPLE_PACKAGE_BASE}.DevelopmentSampleJobMXBean
${SAMPLE_PACKAGE_BASE}.HelloEdgent
${SAMPLE_PACKAGE_BASE}.JobEventsSample
${SAMPLE_PACKAGE_BASE}.JobExecution
${SAMPLE_PACKAGE_BASE}.PeriodicSource
${SAMPLE_PACKAGE_BASE}.SensorsAggregates
${SAMPLE_PACKAGE_BASE}.SimpleFilterTransform
${SAMPLE_PACKAGE_BASE}.SplitWithEnumSample
${SAMPLE_PACKAGE_BASE}.TempSensorApplication
${SAMPLE_PACKAGE_BASE}.TerminateAfterNTuples
EOF
`

if [ "$1" = "--list" ] ; then
  SAMPLES=
  for i in ${SAMPLES_FQ}; do
    SAMPLE=`echo ${i} | sed -e 's/.*\.//'`
    SAMPLES="${SAMPLES} ${SAMPLE}"
  done
  echo ${SAMPLES}
  exit 0
fi
if [ "$1" = "" ] ; then
  echo $USAGE
  exit 1
fi

SAMPLE_NAME=$1
shift

SAMPLE_FQ=
for i in ${SAMPLES_FQ}; do
  SAMPLE_FQ=`echo $i | grep -- "\.${SAMPLE_NAME}\$"`
  if [ "${SAMPLE_FQ}" != "" ]; then
    break
  fi
done
if [ "${SAMPLE_FQ}" = "" ]; then
  echo unrecognized sample name \"${SAMPLE_NAME}\"
  echo ${USAGE}
  exit 1
fi

java -cp ${UBER_JAR} "${SAMPLE_FQ}" "$@"

