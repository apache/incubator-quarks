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
## --mvn mvn-cmd use mvn-cmd instead of "../mvnw"
##
## Creates bundles and classpath.sh in the target dir.

USAGE="usage: [--platform {java8|java7|android}] [--version edgent-version] [--artifacts csv-gav-list] [--file gav-file] [--mvn mvn-cmd]"

set -e

# project dir is where this script resides
PROJ_DIR=`(cd $(dirname $0); pwd)`

SAMPLES_DIR=`(cd $(dirname $0); pwd)`/..
MVN_CMD=${SAMPLES_DIR}/mvnw

EDGENT_PLATFORM=java8
EDGENT_VERSION=
SLF4J_VERSION=1.7.12

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
    case ${response} in
      [yY]) return `true` ;;
      [nN]) return `false` ;;
      *) echo "illegal response '$response'" ;;
    esac
  done
}

###########################
echo
echo "##### Generating dependency decls..."
ARTIFACT_GAVS="${OPT_GAVS:-${DEFAULT_GAVS}}"
mkdir -p target
DEP_DECLS_FILE=target/tmp-dep-decls
rm -f ${DEP_DECLS_FILE}
for i in ${ARTIFACT_GAVS}; do
    echo ${i} | awk -F : '{ type=""; if ($3 == "{EV}") $3="${edgent.runtime.version}"; if ($4 != "") type="  <type>" $4 "</type>\n"; printf "<dependency>\n  <groupId>%s</groupId>\n  <artifactId>%s</artifactId>\n  <version>%s</version>\n%s</dependency>\n", $1, $2, $3, type }' >> ${DEP_DECLS_FILE}
done
DEP_DECLS=`cat ${DEP_DECLS_FILE}`

###########################
echo
echo "##### Generating pom.xml..."
cd ${PROJ_DIR}
cp pom.xml.template pom.xml
ed -s pom.xml <<EOF
/INJECT_DEPENDENCIES_HERE
a
${DEP_DECLS}
.
wq
EOF

###########################
echo
echo "##### Generating the bundles..."
EDGENT_VERSION_PROPERTY=
if [ "${EDGENT_VERSION}" ]; then
  EDGENT_VERSION_PROPERTY=-Dedgent.runtime.version=${EDGENT_VERSION}
fi
PLATFORM_PROFILE=
if [ ${EDGENT_PLATFORM} != "java8" ]; then
  PLATFORM_PROFILE="-Pplatform-${EDGENT_PLATFORM}"
fi
${MVN_CMD} clean package ${EDGENT_VERSION_PROPERTY} ${PLATFORM_PROFILE}


###########################
echo
echo "##### Generating classpath.sh..."
cat << 'EOF'  > ${PROJ_DIR}/target/classpath.sh
#!/bin/sh
USAGE="usage: classpath.sh [--add-slf4j-jdk] <path-to-unpacked-bundle>"
set -e
if [ "${1}" == "--add-slf4j-jdk" ]; then
    ADD_SLF4J_IMPL=slf4j-jdk
    shift
fi
if [ $# != 1 ] || [[ ${1} == -* ]] ; then
    echo "${USAGE}"
    exit 1
fi
BASEDIR=${1}
cd ${BASEDIR}
SEP=
CP=
if [ "`ls libs 2>/dev/null`" != "" ]; then
    for i in libs/*; do
        CP="${CP}${SEP}${BASEDIR}/${i}"
        SEP=":"
    done
fi
if [ "`ls ext 2>/dev/null`" != "" ]; then
    for i in ext/*; do
        if [[ ${i} == */slf4j-* ]] && [[ ${i} != */slf4j-api-* ]] ; then
            # it's an slf4j impl
            if [[ "${ADD_SLF4J_IMPL}" == "" ]] || [[ ${i} != */${ADD_SLF4J_IMPL}* ]] ; then 
                continue
            fi
        fi
        CP="${CP}${SEP}${BASEDIR}/${i}"
        SEP=":"
    done
fi
echo "${CP}"
EOF
chmod +x target/classpath.sh

###########################
echo
echo "##### Bundle LICENSING information:"
echo
cat ${PROJ_DIR}/src/main/resources/README
echo
cat <<'EOF'
##### Using a bundle:

    copy a bundle from target and unpack it
    copy target/classpath.sh and use it to compose a classpath:

        export CLASSPATH=`./classpath.sh --add-slf4j-jdk <path-to-unpacked-bundle>`

    Omit "--add-slf4j-jdk" to omit an slf4j-jdk* implementation jar from the classpath.
EOF
