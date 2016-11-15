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


# This script creates a release branch for the Apache Edgent version from gradle.properties/build_version
#
# Must be run at the root of a clone of the master ASF git repository from https://git-wip-us.apache.org/repos/asf/incubator-edgent.git

if [ $# -ne 0 ]
then
    echo Usage: buildTools/make_release_branch.sh
fi

EDGENT_VERSION=`grep build_version gradle.properties | awk '{print $2}'`
CHECK=`echo "$EDGENT_VERSION" | grep -q -E '[0-9]{1,2}\.[0-9]{1,2}\.[0-9]{1,3}$'`

if [ $? -ne 0 ]
then
    echo "Apache Edgent version needs to be in the form [0-100].[0-100].[0-999]"
    exit 1;
fi

EDGENT_ROOT=.
RELEASE_BRANCH=release${EDGENT_VERSION}
RELEASE_CLONE_DIRNAME=asfclone-edgent${EDGENT_VERSION}

echo "Updating local master branch"
git checkout master
git fetch origin
git rebase origin/master

echo "Creating release branch ${RELEASE_BRANCH}"
git push -u origin master:${RELEASE_BRANCH}

echo "Creating new clone ${RELEASE_CLONE_DIRNAME} for release work"
cd ${EDGENT_ROOT}/..
mkdir "${RELEASE_CLONE_DIRNAME}"
cd "${RELEASE_CLONE_DIRNAME}"
git clone https://git-wip-us.apache.org/repos/asf/incubator-edgent.git .

echo "Creating the RC1 tag"
git checkout ${RELEASE_BRANCH}
git tag -a apache-edgent-${EDGENT_VERSION}RC1 -m "Apache Edgent ${EDGENT_VERSION} RC1"
git push --tags
