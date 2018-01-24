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

# Create a tag on the release branch for the release candidate
# Edgent version from gradle.properties/build_version
# Prompts before taking actions unless "--nquery"
#
# Run from the root of the release management git clone.

. `dirname $0`/common.sh

dieSuperceeded  # NOTICE THIS

setUsage "`basename $0` [--nquery] <rc-num>"
handleHelp "$@"

NQUERY=
if [ "$1" == "--nquery" ]; then
  NQUERY="--nquery"; shift
fi

requireArg "$@"
RC_NUM=$1; shift
checkRcNum ${RC_NUM} || usage "Not a release candidate number \"${RC_NUM}\""

noExtraArgs "$@"

`dirname $0`/tag_release.sh ${NQUERY} --as-rcnum ${RC_NUM} 
