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

# Refresh the clone's release branch from the "origin" git remote.
# Prompts before taking actions unless "--nquery"

. `dirname $0`/common.sh

setUsage "`basename $0` [--nquery]"
handleHelp "$@"

NQUERY=
if [ "$1" == "--nquery" ]; then
  NQUERY="--nquery"; shift
fi

noExtraArgs "$@"

checkEdgentSourceRootGitDie
checkUsingMgmtCloneWarn || [ ${NQUERY} ] || confirm "Proceed using this clone?" || exit

VER=`getEdgentVer`
RELEASE_BRANCH=`getReleaseBranch ${VER}`

(set -x; git checkout -q ${RELEASE_BRANCH})
(set -x; git status)

[ ${NQUERY} ] || confirm "Proceed to refresh branch ${RELEASE_BRANCH} from the origin?" || exit

echo "Refreshing branch ${RELEASE_BRANCH}"
(set -x; git pull origin ${RELEASE_BRANCH})
