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

## Get the Apache Edgent jars and their transitive external dependencies.
##
## By default get the Edgent java8 platform jars for the script's default Edgent version.
##
## --platform {java8|java7|android} get the specified target platform jars
## --version edgent-version get the specified version's jars (e.g., 1.2.0)
## --artifacts csv-gav-list get only the specified artifacts. Not restricted to Edgent jars.
##   The Edgent version is substituted for all instances of '{EV}'
## --file gav-file get only the specified artifacts. Not restricted to Edgent jars.
##   The Edgent version is substituted for all instances of '{EV}'
##   Lines that begin with '#' are ignored.
## --mvn mvn-cmd use mvn-cmd instead of "./mvnw"
##
## Creates the directory get-edgent-jars-project and a maven project in it

USAGE="usage: [--platform {java8|java7|android}] [--version edgent-version] [--artifacts csv-gav-list] [--file gav-file] [--mvn mvn-cmd]"

set -e

SAMPLES_DIR=`(cd $(dirname $0); pwd)`/..
MVN_CMD=${SAMPLES_DIR}/mvnw

EDGENT_PLATFORM=java8
EDGENT_VERSION=1.2.0
SLF4J_VERSION=1.7.12

PROJ_DIR=tmp-get-edgent-jars-project

if [ "$1" = "--platform" -a $# -gt 1 ]; then
    EDGENT_PLATFORM=$2; shift; shift
fi
if [ "$1" = "--version" -a $# -gt 1 ]; then
    EDGENT_VERSION=$2; shift; shift
fi
OPT_GAVS=
if [ "$1" = "--artifacts" -a $# -gt 1 ]; then
    OPT_CSV_GAVS=$2; shift; shift
    OPT_GAVS=`echo "${OPT_CSV_GAVS}" | sed -e 's/,/ /g'`
fi
if [ "$1" = "--file" -a $# -gt 1 ]; then
    OPT_GAVS_FILE=$2; shift; shift
    OPT_GAVS=`sed -e '/^#/d' < ${OPT_GAVS_FILE}`
fi
if [ "$1" = "--mvn" -a $# -gt 1 ]; then
    MVN_CMD=$2; shift; shift
fi
if [ $# != 0 ]; then
    echo "$USAGE"
    exit 1
fi

# only declare "top level" Edgent components that a user
# would directly declare/use and let these components
# (most typically the provider) pull in the rest of the
# Edgent jars (and their dependencies)
#
# Explicitly add edgent-connectors-websocket-jetty
# as there's not a direct dependency on it from connectors-websocket.
#
# Hmm... consider adding org.apache.edgent.console:edgent-console-servlets:{EV}:war
# It's bundled in edgent-console-server.jar.  Having it separately available
# would enable having the "console" in a Servler engine of the user's choosing.
# If added, may want to put it in a directory other than edgent-jars.
#
DEFAULT_GAVS=`cat << EOF
org.slf4j:slf4j-jdk14:${SLF4J_VERSION}
org.apache.edgent:edgent-analytics-math3:{EV}
org.apache.edgent:edgent-analytics-sensors:{EV}
org.apache.edgent:edgent-connectors-command:{EV}
org.apache.edgent:edgent-connectors-csv:{EV}
org.apache.edgent:edgent-connectors-file:{EV}
org.apache.edgent:edgent-connectors-http:{EV}
org.apache.edgent:edgent-connectors-iot:{EV}
org.apache.edgent:edgent-connectors-iotp:{EV}
org.apache.edgent:edgent-connectors-jdbc:{EV}
org.apache.edgent:edgent-connectors-kafka:{EV}
org.apache.edgent:edgent-connectors-mqtt:{EV}
org.apache.edgent:edgent-connectors-pubsub:{EV}
org.apache.edgent:edgent-connectors-serial:{EV}
org.apache.edgent:edgent-connectors-websocket:{EV}
org.apache.edgent:edgent-connectors-websocket-jetty:{EV}
org.apache.edgent:edgent-providers-development:{EV}
org.apache.edgent:edgent-providers-direct:{EV}
org.apache.edgent:edgent-providers-iot:{EV}
org.apache.edgent:edgent-utils-metrics:{EV}
org.apache.edgent:edgent-utils-streamscope:{EV}
EOF
`
if [ "${EDGENT_PLATFORM}" != "java8" ]; then
  DEFAULT_GAVS=`echo "${DEFAULT_GAVS}" | sed -e "s/apache.edgent/apache.edgent.${EDGENT_PLATFORM}/"`
fi
if [ "${EDGENT_PLATFORM}" == "android" ]; then
  DEFAULT_GAVS=`echo "${DEFAULT_GAVS}" | sed -e "/edgent-providers-development/d"`
  DEFAULT_GAVS=`echo "${DEFAULT_GAVS}"; echo "org.apache.edgent.android:edgent-android-hardware:{EV}"`
  DEFAULT_GAVS=`echo "${DEFAULT_GAVS}"; echo "org.apache.edgent.android:edgent-android-topology:{EV}"`
fi


function confirm () {  # [$1: question]
  while true; do
    # call with a prompt string or use a default                                                                                                                                                   
    /bin/echo -n "${1:-Are you sure?}"
    read -r -p " [y/n] " response
    case $response in
      [yY]) return `true` ;;
      [nN]) return `false` ;;
      *) echo "illegal response '$response'" ;;
    esac
  done
}

###########################
cat <<EOF
This command downloads the Apache Edgent jars and their transitive external dependencies.
The external dependencies have their own licensing term that you should review.
A summary of the external dependencies can be found here <TODO URL>.
EOF
confirm "Continue?" || exit

###########################
if [ ! -d ${PROJ_DIR} ]; then
    echo "##### Generating maven project ${PROJ_DIR}..."
    # ensure a standalone pom (no parent) to avoid unwanted inherited deps
    TMP_PROJ=${PROJ_DIR}-tmp
    mkdir ${TMP_PROJ}
    cd ${TMP_PROJ}
    ${MVN_CMD} -B archetype:generate \
        -DarchetypeGroupId=org.apache.maven.archeTypes \
        -DarchetypeArtifactId=maven-archetype-quickstart \
        -DgroupId=org.apache.edgent.tools \
        -DartifactId=${PROJ_DIR} \
        -Dversion=1.0
    cd ..
    mv ${TMP_PROJ}/${PROJ_DIR} ${PROJ_DIR}
    rmdir ${TMP_PROJ}
    cp ${PROJ_DIR}/pom.xml ${PROJ_DIR}/pom.xml.orig
else
    cp ${PROJ_DIR}/pom.xml.orig ${PROJ_DIR}/pom.xml
fi    

###########################

cd ${PROJ_DIR}

###########################

###########################
echo
echo "##### Generating dependency decls..."
ARTIFACT_GAVS="${OPT_GAVS:-${DEFAULT_GAVS}}"
ARTIFACT_GAVS=`echo "${ARTIFACT_GAVS}" | sed -e "s/{EV}/${EDGENT_VERSION}/g"`
mkdir -p target
DEP_DECLS_FILE=target/tmp-dep-decls
rm -f ${DEP_DECLS_FILE}
for i in ${ARTIFACT_GAVS}; do
    echo $i | awk -F : '{ type=""; if ($4 != "") type="  <type>" $4 "</type>\n"; printf "<dependency>\n  <groupId>%s</groupId>\n  <artifactId>%s</artifactId>\n  <version>%s</version>\n%s</dependency>\n", $1, $2, $3, type }' >> ${DEP_DECLS_FILE}
done
DEP_DECLS=`cat ${DEP_DECLS_FILE}`

###########################
echo
echo "##### Adding dependency decls to pom..."
ed pom.xml <<EOF
/<dependencies>
a
${DEP_DECLS}
.
wq
EOF

###########################
echo
echo "##### Retrieving jars into local maven repo..."
${MVN_CMD} clean compile

###########################
echo
echo "##### Copying jars..."
# if someone screws up j7 or android deps, uncomment the following and
# it will help identify wrong jars that are getting included / copied
# (and otherwise overwriting each other).
#DEBUG_DEPS=-Dmdep.prependGroupId=true
${MVN_CMD} dependency:copy-dependencies -DincludeScope=runtime ${DEBUG_DEPS}

DEPS_SRC_DIR=target/dependency
EDGENT_DEPS_DIR=${EDGENT_PLATFORM}/edgent-jars
EXT_DEPS_DIR=${EDGENT_PLATFORM}/ext-jars

rm -rf "${EDGENT_DEPS_DIR}"; mkdir -p ${EDGENT_DEPS_DIR}
rm -rf "${EXT_DEPS_DIR}"; mkdir -p ${EXT_DEPS_DIR}

cp ${DEPS_SRC_DIR}/* ${EXT_DEPS_DIR}

for i in `find ${EXT_DEPS_DIR} -name '*edgent-*.*ar'`; do
  mv $i ${EDGENT_DEPS_DIR}
done

###########################
echo
echo "##### Generating classpath.sh..."
cat << 'EOF'  > ${EDGENT_PLATFORM}/classpath.sh
#!/bin/sh
set -e
if [ "${1}" = "" -o "${1}" = "-?" -o "${1}" = "-help" ]; then 
    echo "usage: classpath.sh <path-to-parent-of-edgent-jars-dir>"
    exit 1
fi
BASEDIR=${1}
cd ${BASEDIR}
SEP=
CP=
if [ "`ls edgent-jars 2>/dev/null`" != "" ]; then
    for i in edgent-jars/*; do
        CP="${CP}${SEP}${BASEDIR}/${i}"
        SEP=":"
    done
fi
if [ "`ls ext-jars 2>/dev/null`" != "" ]; then
    for i in ext-jars/*; do
        if [[ ${i} == */slf4j-* ]] && [[ ${i} != */slf4j-api-* ]] ; then
            continue
        fi
        CP="${CP}${SEP}${BASEDIR}/${i}"
        SEP=":"
    done
fi
echo "${CP}"
EOF
chmod +x ${EDGENT_PLATFORM}/classpath.sh

###########################
echo
echo "##### The Edgent jars are in ${PROJ_DIR}/${EDGENT_DEPS_DIR}"
echo "##### The external jars are in ${PROJ_DIR}/${EXT_DEPS_DIR}"
echo "##### CLASSPATH may be set by copying ${PROJ_DIR}/${EDGENT_PLATFORM}/classpath.sh and using it like:"
echo '#####    export CLASSPATH=`classpath.sh path-to-parent-of-edgent-jars-dir`'
