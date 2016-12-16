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

# Copy the (approved) release candidate artifacts and KEYS from the 
# ASF subversion Edgent Release Candidate repository to the
# ASF subversion Edgent Release repository.
# Prompts before taking actions.
#
# Run from the root of the release management git clone.

. `dirname $0`/common.sh

setUsage "`basename $0` <rc-num>"
handleHelp "$@"

requireArg "$@"
RC_NUM=$1; shift
checkRcNum ${RC_NUM} || usage "Not a release candidate number \"${RC_NUM}\""
RC_DIRNAME="rc${RC_NUM}"

noExtraArgs "$@"

SVN_DEV_EDGENT=~/svn/dist.apache.org/repos/dist/dev/incubator/edgent
SVN_REL_EDGENT=~/svn/dist.apache.org/repos/dist/release/incubator/edgent

checkUsingMgmtCloneWarn || confirm "Proceed using this clone?" || exit

# Get the X.Y.Z version from gradle
VER=`getEdgentVer gradle`
VER_DIRNAME=${VER}-incubating

RC_TAG=`getReleaseTag ${VER} ${RC_NUM}`
RELEASE_TAG=`getReleaseTag ${VER}`

COMMIT_MSG="Release Apache Edgent ${RELEASE_TAG} from ${RC_TAG}"

confirm "Proceed to publish release ${RELEASE_TAG} from candidate ${RC_TAG}?" || exit

echo
confirm "Proceed to update the KEYS?" || exit
(set -x; svn update ${SVN_DEV_EDGENT}/KEYS)
SVN_PARENT_DIR=`dirname ${SVN_REL_EDGENT}`
(set -x; mkdir -p ${SVN_PARENT_DIR})
(set -x; cd ${SVN_PARENT_DIR}; svn co ${EDGENT_ASF_SVN_RELEASE_URL} --depth empty)
(set -x; svn update ${SVN_REL_EDGENT}/KEYS)
(set -x; cp ${SVN_DEV_EDGENT}/KEYS ${SVN_REL_EDGENT}/KEYS)
# it's OK if nothing changed / nothing to commit...
(set -x; svn commit ${SVN_REL_EDGENT}/KEYS -m "${COMMIT_MSG}")

echo
confirm "Proceed to move the ${RC_TAG} artifacts?" || exit
(set -x; svn move \
  ${EDGENT_ASF_SVN_DEV_URL}/${VER_DIRNAME}/${RC_DIRNAME} \
  ${EDGENT_ASF_SVN_RELEASE_URL}/${VER_DIRNAME} \
  -m "${COMMIT_MSG}")

echo
echo "The ASF dev and release repositories have been updated:"
echo "    ${EDGENT_ASF_SVN_DEV_URL}"
echo "    ${EDGENT_ASF_SVN_RELEASE_URL}"
