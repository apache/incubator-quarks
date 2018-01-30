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

# Creates a management clone in which to execute the release process.
# Records the version number of the release under construction so that
# other buildTools don't require an arg / or query
#
# Run from a directory where you want to create the management clone,
# or if using --skipClone, run from the root of the release management git clone.
# Creates edgent.release.properties


. `dirname $0`/common.sh

setUsage "`basename $0` [--createClone] X.Y.Z"
handleHelp "$@"

MK_CLONE=
if [ "$1" == "--createClone" ]; then
  MK_CLONE=1; shift
fi

requireArg "$@"
VER=$1; shift
checkVerNumDie ${VER}

noExtraArgs "$@"

CLONE_NAME="mgmt-edgent-${VER}"
if [ "${MK_CLONE}" != "" ]; then
  confirm "Proceed to create management clone ${CLONE_NAME}?" || exit
  (set -x; git clone ${EDGENT_ASF_GIT_URL} ${CLONE_NAME})
  cd ${CLONE_NAME}
else
  checkUsingMgmtCloneWarn || confirm "Proceed using this clone?" || exit
  if [ -f ${RELEASE_PROP_FILE} ]; then
    confirm "The release property file (${RELEASE_PROP_FILE}) already exists, continue to overwrite it?" || exit
  fi 
fi

createReleaseProperties ${VER}
echo "The release property file has been created for release version ${VER}"
