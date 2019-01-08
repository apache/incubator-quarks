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

# Creates a branch for the release.
# Uses the version id from gradle.properties.
# Prompts before taking actions unless "--nquery".
#
# Run from the root of the release management git clone.
#
# Prior to running this, create a new release management clone
# from the ASF git repository.  The name of the clone's directory should
# start with "mgmt-edgent" as the builtTools scripts check for that
# to help keep one on the right path, e.g.,
#
#   git clone https://gitbox.apache.org/repos/asf/incubator-edgent.git mgmt-edgent<version>

. `dirname $0`/common.sh

dieSuperceeded  # NOTICE THIS

setUsage "`basename $0` [--nquery]"
handleHelp "$@"

NQUERY=
if [ "$1" == "--nquery" ]; then
  NQUERY="--nquery"; shift
fi

noExtraArgs "$@"

checkEdgentSourceRootGitDie
checkUsingMgmtCloneWarn || [ ${NQUERY} ] || confirm "Proceed using this clone?" || exit

VER=`getEdgentVer gradle`
RELEASE_BRANCH=`getReleaseBranch $VER`

(set -x; git checkout -q master)
(set -x; git status)
[ ${NQUERY} ] || confirm "Proceed to create release branch ${RELEASE_BRANCH}?" || exit

echo "Creating release branch ${RELEASE_BRANCH}"
# don't just use "git push -u origin master:${RELEASE_BRANCH}" as some suggested
# to *create* the branch as that changes the local master to track the new
# remote branch. yikes.
(set -x; git checkout -b ${RELEASE_BRANCH})
(set -x; git push -u origin ${RELEASE_BRANCH}) 
