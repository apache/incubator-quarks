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

BUILDTOOLS_DIR=`dirname $0`

EDGENT_ROOT_DIR=.
BUNDLE_DIR=${EDGENT_ROOT_DIR}/build/release-edgent

EDGENT_ASF_GIT_URL=https://git-wip-us.apache.org/repos/asf/incubator-edgent.git
EDGENT_ASF_SVN_RELEASE_URL=https://dist.apache.org/repos/dist/release/incubator/edgent
EDGENT_ASF_SVN_RC_URL=https://dist.apache.org/repos/dist/dev/incubator/edgent

USAGE=

function die() {  # [$* msgs]
  [ $# -gt 0 ] && echo "Error: $*"
  exit 1
}

function setUsage() {  # $1: usage string
  USAGE=$1
}

function usage() {  #  [$*: msgs]
  [ $# -gt 0 ] && echo "Error: $*"
  echo "Usage: ${USAGE}"
  exit 1
}

function handleHelp() { # usage: handleHelp "$@"
  if [ "$1" == "-?" -o "$1" == "--help" ]; then
    usage
  fi
}

function requireArg() {  # usage: requireArgs "$@"
  if [ $# -lt 1 ] || [[ $1 =~ ^- ]]; then
    usage "missing argument"
  fi
}

function noExtraArgs() { # usage: noExtraArgs "$@"
  [ $# = 0 ] || usage "extra arguments"
}

function confirm () {  # [$1: question]
  while true; do
    # call with a prompt string or use a default                                                                                                                                                   
    /bin/echo -n "${1:-Are you sure?}"
    read -r -p " [y/n] " response
    case $response in
      [yY]) return `true` ;;
      [nN]) return `false` ;;
      *) echo "illegal response '$response'" ;;
    esac
  done
}

function checkEdgentSourceRootGitDie { # no args; dies if !ok
  [ -d "${EDGENT_ROOT_DIR}/.git" ] || die "Not an Edgent source root git directory \"${EDGENT_ROOT_DIR}\""
}

function checkUsingMgmtCloneWarn() { # no args; warns if edgent root isn't a mgmt clone
  CLONE_DIR=`cd ${EDGENT_ROOT_DIR}; pwd`
  CLONE_DIRNAME=`basename $CLONE_DIR`
  if [ ! `echo $CLONE_DIRNAME | grep -o -E '^mgmt-edgent'` ]; then
    echo "Warning: the Edgent root dir \"${EDGENT_ROOT_DIR}\" is not a release mgmt clone!"
    return 1
  else
    return 0
  fi 
} 

function getEdgentVer() {  # $1 == "gradle" | "bundle"
  MSG="getEdgentVer(): unknown mode \"$1\""
  VER=""
  if [ $1 == "gradle" ]; then
    # Get the X.Y.Z version from gradle build info
    PROPS=${EDGENT_ROOT_DIR}/gradle.properties
    VER=`grep build_version ${PROPS} | grep -o -E '\d+\.\d+\.\d+'`
    MSG="Unable to identify the version id from ${PROPS}"
  elif [ $1 == "bundle" ]; then
    # Get the X.Y.Z version from a build generated bundle's name
    BUNDLE=`echo ${BUNDLE_DIR}/*-src.tgz`
    VER=`echo ${BUNDLE} | grep -o -E '\d+\.\d+\.\d+'`
    MSG="Unable to identify the version id from bundle ${BUNDLE}"
  fi
  [ "${VER}" ] || die "${MSG}"
  echo $VER
}

function checkBundleDir() { # no args  returns true/false (0/1)
  if [ -d ${BUNDLE_DIR} ]; then
    return 0
  else
    return 1
  fi
}

function checkVerNum() {  #  $1: X.Y.Z  returns true/false (0/1)
  if [ `echo $1 | grep -o -E '^\d+\.\d+\.\d+$'` ]; then
    return 0
  else
    return 1
  fi
}

function checkVerNumDie() { #  $1: X.Y.Z  dies if not ok
  checkVerNum $1 || die "Not a X.Y.Z version number \"$1\""
}

function checkRcNum() {  # $1: rc-num   returns true/false (0/1)
  if [ `echo $1 | grep -o -E '^\d+$'` ] && [ $1 != 0 ]; then
    return 0
  else
    return 1
  fi
}

function checkRcNumDie() {  # $1: rc-num dies if not ok
  checkRcNum $1 || die "Not a release candidate number \"$1\""
}

function getReleaseBranch() { # $1: X.Y.Z version
  checkVerNumDie $1
  echo "release-$1"
}

function getReleaseTag() {  # $1: X.Y.Z  [$2: rc-num]
  VER=$1; shift
  checkVerNumDie ${VER}
  RC_SFX=""
  if [ $# -gt 0 ] && [ "$1" != "" ]; then
    RC_SFX="-RC$1"
  fi
  echo "${VER}-incubating${RC_SFX}" 
}

function getReleaseTagComment() {  # $1: X.Y.Z  [$2: rc-num]
  VER=$1; shift
  checkVerNumDie ${VER}
  RC_SFX=""
  if [ $# -gt 0 ] && [ "$1" != "" ]; then
    checkRcNumDie $1
    RC_SFX=" RC$1"
  fi
  echo "Apache Edgent ${VER}-incubating${RC_SFX}" 
}
