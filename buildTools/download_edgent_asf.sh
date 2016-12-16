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

# Download the collection of files associated with an Apache Edgent
# Release or Release Candidate from the Apache Distribution area:
# https://dist.apache.org/repos/dist/release/incubator/edgent
# or https://dist.apache.org/repos/dist/dev/incubator/edgent
# respectively.
#
# Prompts before taking actions unless "--nquery"
# Prompts to perform signature validation (using buildTools/check_sigs.sh)
# unless --nvalidate or --validate is specified.

. `dirname $0`/common.sh

setUsage "`basename $0` [--nquery] [--validate|--nvalidate] <version> [<rc-num>]"
handleHelp "$@"

NQUERY=
if [ "$1" == "--nquery" ]; then
  NQUERY="--nquery"; shift
fi

VALIDATE=-1  # query
if [ "$1" == "--validate" ]; then
  VALIDATE=1; shift
elif [ "$1" == "--nvalidate" ]; then
  VALIDATE=0; shift
fi

requireArg "$@"
VER=$1; shift
checkVerNum $VER || usage "Not a X.Y.Z version number \"$VER\""

RC_NUM=
if [ $# -gt 0 ]; then
  RC_NUM=$1; shift
  checkRcNum ${RC_NUM} || usage "Not a release candidate number \"${RC_NUM}\""
fi

noExtraArgs "$@"

# Release or Release Candidate mode
IS_RC=
if [ ${RC_NUM} ]; then
  IS_RC=1
fi

BASE_URL=${EDGENT_ASF_SVN_RELEASE_URL}
if [ ${IS_RC} ]; then
  BASE_URL=${EDGENT_ASF_SVN_RC_URL}
fi

RC_SFX=
if [ ${IS_RC} ]; then
    RC_SFX=rc${RC_NUM}
fi

DST_BASE_DIR=downloaded-edgent-${VER}${RC_SFX}
DST_VER_DIR=${DST_BASE_DIR}/${VER}-incubating
if [ ${IS_RC} ]; then
  DST_VER_DIR=${DST_VER_DIR}/${RC_SFX}
fi
[ -d ${DST_BASE_DIR} ] && die "${DST_BASE_DIR} already exists"

[ ${NQUERY} ] || confirm "Proceed to download to ${DST_BASE_DIR} from ${BASE_URL}?" || exit

echo Downloading to ${DST_BASE_DIR} ...

# make a template structure of everything we're going to retrieve
mkdir -p ${DST_BASE_DIR}
(cd ${DST_BASE_DIR}; touch KEYS)

mkdir -p ${DST_VER_DIR}
(cd ${DST_VER_DIR}; touch LICENSE README RELEASE_NOTES apache-edgent-${VER}-incubating-src.tgz{,.asc,.md5,.sha} )

mkdir -p ${DST_VER_DIR}/binaries
(cd ${DST_VER_DIR}/binaries; touch LICENSE apache-edgent-${VER}-incubating-bin.tgz{,.asc,.md5,.sha} )

# download everything identified in the template tree
ORIG_DIR=`pwd`
cd `pwd`/${DST_BASE_DIR}
for i in `find . -type f`; do
  echo ======= $i
  uri=`echo $i | sed -e s?^./??`  # strip leading "./"
  url=${BASE_URL}/$uri
  d=`dirname $i`
  # OSX lacks wget by default
  echo "(cd $d; curl -f -O $url)"
  (cd $d; curl -f -O $url)
done
cd ${ORIG_DIR}

echo
echo Done Downloading to ${DST_BASE_DIR}

[ ${VALIDATE} == 0 ] && exit
[ ${VALIDATE} == 1 ] || [ ${NQUERY} ] || confirm "Do you want to check the bundle signatures?" || exit

echo
echo "If the following bundle gpg signature checks fail, you may need to"
echo "import the project's list of signing keys to your keyring"
echo "    $ gpg ${DST_BASE_DIR}/KEYS            # show the included keys"
echo "    $ gpg --import ${DST_BASE_DIR}/KEYS"

echo
echo "Verifying the source bundle signatures..."
$BUILDTOOLS_DIR/check_sigs.sh ${DST_VER_DIR}

echo
echo "Verifying the binary bundle signatures..."
$BUILDTOOLS_DIR/check_sigs.sh ${DST_VER_DIR}/binaries
