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

USAGE="usage: `basename $0`"

set -e

if [ "${JAVA_HOME}" = "" ]; then
  echo JAVA_HOME not set
  exit 1
fi

EDGENT_SAMPLES_DIR=`pwd`/..

TOPOLOGY_SAMPLES_JAR=`echo ${EDGENT_SAMPLES_DIR}/topology/target/edgent-samples-topology-*-uber.jar`

sed -e "s,{EDGENT_SAMPLES_DIR},${EDGENT_SAMPLES_DIR},g" \
    -e "s,{TOPOLOGY_SAMPLES_JAR},${TOPOLOGY_SAMPLES_JAR},g" \
    -e "s,{JAVA_HOME},${JAVA_HOME},g" \
    <startapp.cron.template >startapp.cron

echo created startapp.cron