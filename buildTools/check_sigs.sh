#!/bin/sh -e

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

# Checks the signatures of all bundles in the build/release-edgent directory
# Or checks the bundles in the specified directory

if [ $1 == "-?" || $1 == "help" || $# -gt 1 ]
then
    echo "Usage: check_sigs.sh [bundle-directory]"
    exit 1
fi

# Assumes run from the root of the edgent git repo
EDGENT_ROOT=.

BUNDLE_DIR="${EDGENT_ROOT}/build/release-edgent"
if [ $# -ge 1 ]
then
    BUNDLE_DIR=$1
fi

if [ ! -d ${BUNDLE_DIR} ]
then
    echo "Bundle directory '${BUNDLE_DIR}' does not exist" 
    exit 1
fi

function checkFile() {
    FILE="$1"
    
    HASH=`md5 -q "${FILE}"`
    CHECK=`cat "${FILE}.md5"`

    if [ "$HASH" != "$CHECK" ]
    then
        echo "${FILE} MD5 incorrect"
        exit 1;
    else
       echo "${FILE} MD5 OK";
    fi
    
    HASH=`shasum -p -a 512 "${FILE}" | awk '{print$1}'`
    CHECK=`cat "${FILE}.sha"`

    if [ "$HASH" != "$CHECK" ]
    then
        echo "${FILE} SHA incorrect"
        exit 1;
    else
       echo "${FILE} SHA OK";
    fi

    gpg --verify "${FILE}.asc"

}

for bundle in "${BUNDLE_DIR}/*.tgz"
do
    checkFile $bundle
done

echo "SUCCESS: all checksum and signature files OK"
