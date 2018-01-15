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

# Generates checksum files for all tar.gz and zip in the current 
# or specified directory

. `dirname $0`/common.sh

setUsage "`basename $0` [bundle-directory]"
handleHelp "$@"

BUNDLE_DIR=.
if [ $# -ge 1 ]
then
    BUNDLE_DIR=$1; shift
fi

noExtraArgs "$@"

[ -d ${BUNDLE_DIR} ] || die "Bundle directory \"${BUNDLE_DIR}\" does not exist"

function makeFileChecksums() {
    FILE="$1"
    echo "Generating checksums for $FILE..."
    [ -f "${FILE}" ] || die "Bundle file \"${FILE}\" does not exist"
    md5 -q ${FILE} > ${FILE}.md5    
    shasum -p -a 512 ${FILE} | awk '{print$1}' > ${FILE}.sha512
}

for bundle in ${BUNDLE_DIR}/*.tar.gz
do
    makeFileChecksums ${bundle}
done

for bundle in ${BUNDLE_DIR}/*.zip
do
    makeFileChecksums ${bundle}
done

echo
echo "SUCCESS: all checksum files generated"
