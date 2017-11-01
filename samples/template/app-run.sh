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

APP_DIR=.

UBER_JAR=`echo ${APP_DIR}/target/*-uber.jar`

FQ_MAIN_CLASS=com.mycompany.app.TemplateApp

USAGE="usage: [--main <main-class>] [<args...>]"

if [ "$1" = "--main" ]; then
  shift;
  if [ $# = 0 ]; then
    echo ${USAGE}
    exit 1;
  fi 
  FQ_MAIN_CLASS=$1; shift
fi

# Runs the application
#
# ./app-run.sh

export CLASSPATH=${UBER_JAR}

java ${FQ_MAIN_CLASS} "$*"
