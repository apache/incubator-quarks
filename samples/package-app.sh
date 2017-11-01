#!/bin/sh
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

## Create a self contained application specific tar bundle that can be
## brought to a system, unpacked and run.
##
## Run from the Application project's top level directory.

USAGE="usage: `basename $0` [--platform {java8|java7|android}] [--mainClass classname] [--appjar jarname] [--add csv-paths] [--mvn mvn-cmd]"

## --platform the platform the app was built for (default: java8. options: java7, android)
##   This controls which Edgent platform jars are collected.
##   E.g., use "--platform java7" when the app is built using
##   the profile "-Pplatform-java7".
## --mainClass the main class name (default: com.mycompany.app.App)
## --appjar the application jar name (default: my-app-1.0-SNAPSHOT.jar)
## --add additional csv paths to include in the tarball (default: none)
##   Works best for paths in/under the App's project dir.
##   NOTE: anything in the App's src/main/resources dir generally
##   gets included in the App's jar.
## --mvn mvn-cmd use mvn-cmd instead of "./mvnw"

set -e

SAMPLES_DIR=`(cd $(dirname $0); pwd)`
MVN_CMD=${SAMPLES_DIR}/mvnw

MAIN_CLASS=com.mycompany.app.App
APP_JAR=my-app-1.0-SNAPSHOT.jar
ADD_PATHS=
PLATFORM=

if [ "${1}" = "--platform" -a $# -gt 1 ] ; then
  PLATFORM=${2}; shift; shift
  if [ "${PLATFORM}" = "java8" ] ; then
      PLATFORM=
  fi
fi
if [ "${1}" = "--mainClass" -a $# -gt 1 ] ; then
  MAIN_CLASS=${2}; shift; shift
fi
if [ "${1}" = "--appjar" -a $# -gt 1 ] ; then
  APP_JAR=${2}; shift; shift
fi
if [ "${1}" = "--add" -a $# -gt 1 ] ; then
  ADD_PATHS=${2}; shift; shift
  ADD_PATHS=`echo ${ADD_PATHS} | sed -e 's/,/ /g'`
fi
if [ $# != 0 ]; then
    echo "$USAGE"
    exit 1
fi

TGT_REL_APP_JAR=`basename ${APP_JAR}`  # support spec like target/my.jar

echo
echo "##### get the app specific dependencies..."
PROFILES=
if [ "${PLATFORM}" ] ; then
  PROFILES="-Pplatform-${PLATFORM}"
fi
rm -rf target/dependency
# if someone screws up j7 or android deps, uncomment the following and
# it will help identify wrong jars that are getting included / copied.
#DEBUG_DEPS=-Dmdep.prependGroupId=true
${MVN_CMD} dependency:copy-dependencies -DincludeScope=runtime ${PROFILES} ${DEBUG_DEPS}

echo
echo "##### create target/app-run.sh..."
cat >target/app-run.sh <<EOF
#!/bin/sh

set -e

USAGE="usage: \`basename \$0\` [ args ... ]"

CP=${TGT_REL_APP_JAR}
for i in dependency/\*; do
  CP=\${CP}:\${i}
done

export CLASSPATH=\${CP}
java ${MAIN_CLASS} "\$@"
EOF
chmod +x target/app-run.sh

echo
echo "##### create target/app-pkg.tar..."
D=`pwd`
tar cf target/app-pkg.tar -C target app-run.sh ${TGT_REL_APP_JAR} dependency -C ${D} ${ADD_PATHS}

echo
echo "##### Copy target/app-pkg.tar to the destination system"
echo "##### To run the app:"
echo "#####     mkdir app-pkg"
echo "#####     tar xf app-pkg.tar -C app-pkg"
echo "#####     (cd app-pkg; ./app-run.sh)"
