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

set -e

# Checks the binary release artifact (jar/war) contain the appropriate
# metadata (LICENSE,NOTICE,DISCLAIMER,DEPENDENCIES)

. `dirname $0`/common.sh

setUsage "`basename $0` [-v] jar-or-war-path"
handleHelp "$@"

VERBOSE=0
if [ "$1" == "-v" ]
then
    VERBOSE=1; shift
fi

requireArg "$@"
FILE=$1; shift
[ -f ${FILE} ] || die "File \"${FILE}\" does not exist"

noExtraArgs "$@"

function checkExists() { # $1=base $2=path
    if [ ! -f "$1/$2" ] ; then
        echo "Error: No such file $2"
        return 1
    fi
}

function checkJar() {  # $1 abs-jar-pname
    JAR="$1"
    set +e
    DIR=`mktemp -d`
    mkdir ${DIR}/contents
    EC=0
    [ ${EC} = 0 ] && (cd ${DIR}/contents; jar xf ${JAR}) || EC=1
    BASE=${DIR}/contents
    if [ ${EC} = 0 ] ; then
        checkExists ${BASE} META-INF/LICENSE || EC=1
        checkExists ${BASE} META-INF/NOTICE || EC=1
        checkExists ${BASE} META-INF/DISCLAIMER || EC=1
        
        # ASF policy doesn't require DEPENDENCIES but our project policy does 
        checkExists ${BASE} META-INF/DEPENDENCIES || EC=1
    fi
    (cd ${DIR}; rm -rf contents)
    rmdir ${DIR}
    set -e
    if [ ${EC} != 0 ] ; then
        echo "FAILED: Jar has incorrect metadata"
    fi
    return ${EC}
}

ABS_FILE=$(getAbsPath ${FILE})

echo "Checking ${FILE} ..."

checkJar ${ABS_FILE}  # w/set -e, exits if returns non-0
