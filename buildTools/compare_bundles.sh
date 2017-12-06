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

# Checks that tar.gz and zip bundles have the exact same contents

. `dirname $0`/common.sh

setUsage "`basename $0` tgz-bundle1 zip-bundle2"
handleHelp "$@"

if [ $# -ge 1 ]
then
    TGZ_BUNDLE=$1; shift
fi
if [ $# -ge 1 ]
then
    ZIP_BUNDLE=$1; shift
fi

noExtraArgs "$@"

function compareBundles() {
    TGZFILE="$1"
    ZIPFILE="$2"
    echo
    echo "Unpacking and comparing bundles..."
    echo "[1] ${TGZFILE}"
    echo "[2] ${ZIPFILE}"
    
    set +e
    DIR=`mktemp -d`
    mkdir ${DIR}/bundle1 ${DIR}/bundle2
    (cd ${DIR}/bundle1; set -x; tar zxf ${TGZFILE})
    (cd ${DIR}/bundle2; set -x; unzip -q ${ZIPFILE})
    (set -x; cd ${DIR}; diff -r -q bundle1 bundle2)
    EC=$?
    (cd ${DIR}; rm -rf bundle1 bundle2)
    rmdir ${DIR}
    set -e
    if [ "${EC}" != 0 ] ; then
        echo "FAILED: bundles have the different contents"
    fi
    return ${EC}
}

ABS_TGZ_BUNDLE=$(getAbsPath "${TGZ_BUNDLE}")
ABS_ZIP_BUNDLE=$(getAbsPath "${ZIP_BUNDLE}")

compareBundles ${ABS_TGZ_BUNDLE} ${ABS_ZIP_BUNDLE} 

echo
echo "SUCCESS: bundles have the same contents"
