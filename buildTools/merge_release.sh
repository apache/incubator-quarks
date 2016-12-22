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

# Merges the release branch to the master branch.
# Uses the version id from gradle.properties to identify the branch.
# Prompts before taking actions.
#
# Run from the root of the release management git clone.

. `dirname $0`/common.sh

setUsage "`basename $0`"
handleHelp "$@"

noExtraArgs "$@"

checkEdgentSourceRootGitDie
checkUsingMgmtCloneWarn || confirm "Proceed using this clone?" || exit

VER=`getEdgentVer gradle`
RELEASE_BRANCH=`getReleaseBranch $VER`

(set -x; git checkout -q master)
(set -x; git status)

confirm "Proceed to refresh the local master branch prior to merging?" || exit
(set -x; git pull origin master)

echo
echo "If you proceed to merge and there are conflicts you will need to"
echo "fix the conflicts and then commit the merge and push:"
echo "    git status  # see the conflicts"
echo "    ... fix the conflicts
echo "    git commit -m \"merged ${RELEASE_BRANCH}\""
echo "    git push origin master"
echo "If you choose not to proceed you may run this script again later."

confirm "Proceed to (no-commit) merge branch ${RELEASE_BRANCH} to master?" || exit
(set -x; git merge --no-commit --no-ff ${RELEASE_BRANCH})

echo
echo "If you choose not to proceed you will need to manually complete the"
echo "merge and push:"
echo "    git commit -m \"merged ${RELEASE_BRANCH}\""
echo "    git push origin master"

confirm "Proceed to commit the merge and push?" || exit
(set -x; git commit -m "merged ${RELEASE_BRANCH}")
(set -x; git push origin master)
