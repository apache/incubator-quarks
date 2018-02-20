#!/bin/sh

################################################################################
##
##  Licensed to the Apache Software Foundation (ASF) under one or more
##  contributor license agreements.  See the NOTICE file distributed with
##  this work for additional information regarding copyright ownership.
##  The ASF licenses this file to You under the Apache License, Version 2.0
##  (the "License"); you may not use this file except in compliance with
##  the License.  You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
##  Unless required by applicable law or agreed to in writing, software
##  distributed under the License is distributed on an "AS IS" BASIS,
##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
##  See the License for the specific language governing permissions and
##  limitations under the License.
##
################################################################################

# Checks that binary release artifacts (jars/wars) contain the appropriate
# metadata (LICENSE,NOTICE,DISCLAIMER,DEPENDENCIES)
# and only the expected set of jars are present
#
# At the moment this tool only scans a source/built workspace for jars,
# and of course more jars than are released are present.
# That's OK as the tool still caught jar content problems in
# jars that are expected to be released.
#
# The script could also used against a get-edgent-jars.sh generated
# bundle (containing jars downloaded from nexus) which is also valuable,
# however it doesn't do anything to verify there aren't extra jars
# present in nexus (because get-edgent-jars only includes expected artifacts).
# To run against get-edgent-jars j8 (or j7,android) bundle:
#     - extract get-edgent-jars bundle
#     - buildTools/check_jars.sh --findmode nfilters --check j8 <ver> <extracted-dir>/libs
# It could be helpful in validating at least that content if we
# could for example point get-edgent-jars at a Nexus staged-release area. 

. `dirname $0`/common.sh

setUsage "`basename $0` [--findmode {build|build-release|nfilters}] [--check {j8|j7|android},...] edgent-ver base-release-dir"
handleHelp "$@"

IS_RELEASE=
FIND_MODE=build
if [ "$1" == "--findmode" -a $# -gt 1 ] ; then
    FIND_MODE=$2; shift; shift
    if [ "${FIND_MODE}" = "build-release" ]; then
        FIND_MODE=build
        IS_RELEASE=1
    fi
fi

CHECK_CFG=j8,j7,android
if [ "$1" == "--check" -a $# -gt 1 ] ; then
    CHECK_CFG=$2; shift; shift
fi

INCL_SRCS_JAR=
INCL_JAVADOC_JAR=
if [ "${IS_RELEASE}" != "" ]; then
    INCL_SRCS_JAR=1
    INCL_JAVADOC_JAR=1
fi

requireArg "$@"
EDGENT_VER=$1; shift

requireArg "$@"
BASE=$1; shift
[ -d ${BASE} ] || die "release-dir \"${BASE}\" does not exist"

noExtraArgs "$@"

BUILDTOOLS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# get path of platform's expected jars list file
function getExpJarsPath() { # $1 = platform-id {"",std,j8,j7,android}
    ID="$1"
    [ "${ID}" == "" ] && ID="std"
    FILE=${BUILDTOOLS_DIR}/release_jars_${ID}.txt
    
    if [ ${ID} != "std" -a ! -f ${FILE} ] ; then
        ID="std"
        FILE=${BUILDTOOLS_DIR}/release_jars_${ID}.txt
    fi
    
    [ ! -f ${FILE} ] && die "No such release jars list file: ${FILE}"
    
    echo ${FILE} 
}

function getPackage() {  # $1 X-Y-Z... return X-Y-Z
    ID="$1"
    PKG=`echo ${ID} | sed -e 's/^\([^-]*-[^-]*-[^-]*\)-.*/$1/'`
    echo "${PKG}"
} 

function getPackagePrefix() {  # $1 X-Y-Z... return X-Y
    ID="$1"
    PREFIX=`echo ${ID} | sed -e 's/^\([^-]*-[^-]*\)-.*/\1/'`
    echo "${PREFIX}"
} 

# get list of platform's expected jars leafnames
function getExpJarsList() {  # $1 platform-id {j8,j7,android} $2 VER
    ID="$1"
    VER="$2"
    FILE=`getExpJarsPath ${ID}`
    # handle leading whitespace, ws-only lines, comment lines, trailing comments
    EXP_JARS=`cat ${FILE} | sed -e 's/^[ \t]*//' -e '/^#/d' -e 's/[ \t]*#.*$//' -e 's/[ \t]*$//' `
    EXP_JARS=`for i in ${EXP_JARS} ; do 
        echo ${i} | sed -e "s/{VER}/${VER}/g"

        # expect ...-sources.jar and ...-javadoc.jar if so directed 
        if [ "${INCL_SRCS_JAR}" != "" ]; then
            echo ${i} | sed -e "s/{VER}/${VER}-sources/g" -e 's/.war$/.jar/'
        fi
        if [ "${INCL_JAVADOC_JAR}" != "" ]; then
            PREFIX=$(getPackagePrefix ${i})
            if [ ${ID} = "j8" -o "${PREFIX}" = "edgent-android" ]; then
                echo ${i} | sed -e "s/{VER}/${VER}-javadoc/g" -e 's/.war$/.jar/'
            fi
        fi
    done`
    echo "${EXP_JARS}"
}

# function to find actual jars/wars present in a workspace build
function findBuildJars() { # $1 BASE-DIR
    BASE_DIR=$1
    
    # ACTUAL - when scanning built source tree's "target" dirs,
    # need to exclude those
    #    embedded in war (under WEB-INF)
    #    test classes jars (shouldn't be released)
    #    under component's target test-resources or classes (e.g., a war)
    #    test components (not released)
    #    connectors-websocket-server (test) component (not released)
    #    for J8, those under platforms (each platform handled separately)
    #    those under target/checkout  - release:perform created
    #    edgent-distribution-*-sources.jar (not released; our release:prepare use creates it)
    #
    ACTUAL="`find ${BASE_DIR} -name \*${EDGENT_VER}*.[jw]ar \
         | grep /target/ \
         | grep -v /WEB-INF/ \
         | grep -v -- -tests.jar \
         | grep -v /test-resources/ \
         | grep -v /classes/ \
         | grep -v /retrolambda/ \
         | grep -v '/test/.*/target/' \
         | grep -v '/target/checkout/' \
         | grep -v '/edgent-connectors-websocket-server-' \
         | grep -v 'edgent-distribution-.*-sources.jar' \
         `"
    if [ "`echo ${BASE_DIR} | grep platforms`" = "" ] ; then
        ACTUAL="`for i in ${ACTUAL} ; do echo ${i} | grep -v /platforms/ ; done`" 
    fi
    
    [ "${ACTUAL}" == "" ] && echo "WARNING: no files found under '${BASE_DIR}' for edgent version ${EDGENT_VER} " >/dev/stderr
   
    echo "${ACTUAL}"
}

function findJarsNoFilter() { # $1 BASE-DIR
    BASE_DIR=$1
    
    ACTUAL="`find ${BASE_DIR} -name \*.[jw]ar `"
    
    [ "${ACTUAL}" == "" ] && echo "WARNING: no files found under '${BASE_DIR}'  " >/dev/stderr
   
    echo "${ACTUAL}"
}

function findJars() { # $1 mode:{build,noFilter}  $2 BASE-DIR
    MODE="$1"
    BASE_DIR=$2
    if [ "build" == ${MODE} ] ; then
        findBuildJars "${BASE_DIR}"
    else
        findJarsNoFilter "${BASE_DIR}"
    fi
}
    
function checkJars() { # $1 EXP-JARS $2 ACTUAL-JAR-PATHS
    EXPECT="$1"
    ACTUAL="$2"

    FEC=0
    echo "##### Checking Jar contents ..."
    for i in ${ACTUAL} ; do
        ${BUILDTOOLS_DIR}/check_jar.sh ${i} || FEC=1
    done
    echo "##### done"
    
    # check matching lists
    echo "##### Checking correct Jars are present ..."
    ACTUAL="`for i in ${ACTUAL} ; do echo $(basename ${i}) ; done | sort`" # get basename
    EXPECT="`for i in ${EXPECT} ; do echo "${i}" ; done | sort`"
    if [ "${ACTUAL}" != "${EXPECT}" ]; then
        FEC=1
        ACT_FILE=/tmp/$$-ACTUAL
        for i in ${ACTUAL} ; do echo $i >> ${ACT_FILE} ; done 
        EXP_FILE=/tmp/$$-EXPECT
        for i in ${EXPECT} ; do echo $i >> ${EXP_FILE} ; done
        echo "##### Any unexpected Actuals will be in the left column and missing Expecteds in the right"
        (set -x; comm -3 ${ACT_FILE} ${EXP_FILE} )
        rm  ${ACT_FILE} ${EXP_FILE}
    fi
    echo "##### done"
    
    if [ ${FEC} = 0 ] ; then 
        echo "##### Checking Jars OK"
    else
        echo "##### Checking Jars FAILED"
    fi
    
    return ${FEC}
}

EC=0

echo "##### Checking jar meta-data (LICENSE,NOTICE,DISCLAIMER,DEPENDENCIES)"
echo "##### and correct jars are present (extra jars: ~TODO)"
echo

if [ "" != "$(echo $CHECK_CFG | grep j8)" ] ; then
    echo
    echo "##### Checking J8 jars ..."
    ACTUAL=`findJars ${FIND_MODE} ${BASE}`
    EXPECT=`getExpJarsList j8 ${EDGENT_VER}`
    checkJars "${EXPECT}" "${ACTUAL}" || EC=1
fi

if [ "" != "$(echo $CHECK_CFG | grep j7)" ] ; then
    echo
    echo "##### Checking J7 jars ..."
    ACTUAL=`findJars ${FIND_MODE} ${BASE}/platforms/java7`
    EXPECT=`getExpJarsList j7 ${EDGENT_VER}`
    checkJars "${EXPECT}" "${ACTUAL}" || EC=1
fi

if [ "" != "$(echo $CHECK_CFG | grep android)" ] ; then
    echo
    echo "##### Checking Android jars ..."
    ACTUAL=`findJars ${FIND_MODE} ${BASE}/platforms/android`
    EXPECT=`getExpJarsList android ${EDGENT_VER}`
    checkJars "${EXPECT}" "${ACTUAL}" || EC=1
fi

echo    
if [ ${EC} = 0 ] ; then 
    echo "##### Checking all platform Jars OK"
else
    echo "##### Checking all platform Jars FAILED (review output)"
    exit 1
fi
