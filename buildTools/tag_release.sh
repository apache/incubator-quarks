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

# Create a tag on the release branch for the Edgent version from gradle.properties/build_version
# A "release" tag is created unless directed to create a "release candidate"
# tag via "--as-rcnum <rc-num>"
# Specify "--from-rctag <rc-num>" to create the tag on the commit
# that has the release candidate <rc-num> tag.
# Prompts before taking actions unless "--nquery"
#
# Run from the root of the release management git clone.

. `dirname $0`/common.sh

setUsage "`basename $0` [--nquery] [--as-rcnum <rc-num>] [--from-rctag <rc-num>]"
handleHelp "$@"

NQUERY=
if [ "$1" == "--nquery" ]; then
  NQUERY="--nquery"; shift
fi

RC_NUM=
if [ "$1" == "--as-rcnum" ]; then
  shift; requireArg "$@"
  RC_NUM=$1; shift
  checkRcNum ${RC_NUM} || usage "Not a release candidate number \"${RC_NUM}\""
fi

FROM_RCTAG_NUM=
if [ "$1" == "--from-rctag" ]; then
  shift; requireArg "$@"
  FROM_RCTAG_NUM=$1; shift
  checkRcNum ${FROM_RCTAG_NUM} || usage "Not a release candidate number \"${FROM_RCTAG_NUM}\""
fi

noExtraArgs "$@"

checkEdgentSourceRootGitDie
checkUsingMgmtCloneWarn || [ ${NQUERY} ] || confirm "Proceed using this clone?" || exit

VER=`getEdgentVer gradle`
RELEASE_BRANCH=`getReleaseBranch ${VER}`

TAG=`getReleaseTag ${VER} ${RC_NUM}`
TAG_COMMENT=`getReleaseTagComment ${VER} ${RC_NUM}`

TO_MSG="branch ${RELEASE_BRANCH}"

FROM_RCTAG=
if [ "${FROM_RCTAG_NUM}" ]; then
  FROM_RCTAG=`getReleaseTag ${VER} ${FROM_RCTAG_NUM}`
  TO_MSG="tag ${FROM_RCTAG}"
fi

[ ${NQUERY} ] || confirm "Proceed to add tag \"${TAG}\" to ${TO_MSG}?" || exit

echo "Creating tag ${TAG} to ${TO_MSG}"
(set -x; git checkout -q ${RELEASE_BRANCH})
if [ "$FROM_RCTAG" ]; then
  (set -x; git tag -a ${TAG} -m "${TAG_COMMENT}" ${FROM_RCTAG})
else
  (set -x; git tag -a ${TAG} -m "${TAG_COMMENT}")
fi
(set -x; git push origin ${TAG})
(set -x; git show ${TAG} -s)
