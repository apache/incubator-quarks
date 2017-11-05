#!groovy

/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
node('ubuntu') {

    currentBuild.result = "SUCCESS"

    echo 'Building Branch: ' + env.BRANCH_NAME

    // Setup the required environment variables.
    def mvnHome = "${tool 'Maven 3 (latest)'}"
    env.JAVA_HOME="${tool 'JDK 1.8 (latest)'}"
    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"

    // Make sure the feature branches don't change the SNAPSHOTS in Nexus.
    def mavenGoal = "install"
    def mavenLocalRepo = ""
    if(env.BRANCH_NAME == 'develop') {
        mavenGoal = "sonar:sonar deploy"
    } else {
        mavenLocalRepo = "-Dmaven.repo.local=${env.WORKSPACE}/.repository"
    }
    def mavenFailureMode = "" // consider "--fail-at-end"? Odd ordering side effects?

    try {
        /*stage ('Cleanup') {
            echo 'Cleaning up the workspace'
            deleteDir()
        }*/

        stage ('Checkout') {
            echo 'Checking out branch ' + env.BRANCH_NAME
            checkout scm
        }

        stage ('Clean') {
            echo 'Cleaning Edgent'
            sh "${mvnHome}/bin/mvn ${mavenLocalRepo} -Pplatform-android,platform-java7,distribution clean"
        }

        stage ('Build Edgent') {
            echo 'Building Edgent'
            sh "${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} -Pplatform-android,platform-java7,distribution,toolchain -Djava8.home=${env.JAVA_HOME} -Dedgent.build.ci=true ${mavenGoal}"
        }

        stage ('Build Site') {
            if(env.BRANCH_NAME == 'develop') {
                echo 'Building Site'
                sh "${mvnHome}/bin/mvn ${mavenLocalRepo} site site:stage"
            } else {
                echo 'Building Site (skipped for non develop branch)'
            }
        }

        stage ('Build Samples') {
            echo 'Building samples'
            sh "cd samples; ${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} clean package"
            sh "cd samples/topology; ./run-sample.sh HelloEdgent"
            sh "cd samples; ${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} -Pplatform-java7 clean package"
            sh "cd samples/topology; ./run-sample.sh HelloEdgent"
        }

        stage ('Build Templates') {
            echo 'Building templates'
            sh "cd samples/template; ${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} clean package; ./app-run.sh"
            sh "cd samples/template; ${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} -Pplatform-java7 clean package; ./app-run.sh"
            sh "cd samples/template; ${mvnHome}/bin/mvn ${mavenFailureMode} ${mavenLocalRepo} -Pplatform-android clean package; ./app-run.sh"
        }

        /* There seems to be a problem with this (Here the output of the build log):

        Verifying get-edgent-jars
        [Pipeline] sh
        [edgent-pipeline_develop-JN4DHO6BQV4SCTGBDJEOL4ZIC6T36DGONHH3VGS4DCDBO6UXH4MA] Running shell script
        + cd samples/get-edgent-jars-project
        + ./get-edgent-jars.sh
        ./get-edgent-jars.sh: 111: [: java8: unexpected operator
        ./get-edgent-jars.sh: 118: ./get-edgent-jars.sh: Syntax error: "(" unexpected

        */
        /*stage ('Verify get-engent-jars') {
            if(env.BRANCH_NAME == 'develop') {
                echo 'Verifying get-edgent-jars'
                sh "cd samples/get-edgent-jars-project; ./get-edgent-jars.sh"
            } else {
                echo 'Verifying get-edgent-jars (skipped for non develop branch)'
            }
        }*/
    }


    catch (err) {
        currentBuild.result = "FAILURE"
/*            mail body: "project build error is here: ${env.BUILD_URL}" ,
            from: 'xxxx@yyyy.com',
            replyTo: 'dev@edgent.apache.org',
            subject: 'Autobuild for Branch ' env.BRANCH_NAME
            to: 'commits@edgent.apache.org'
*/
        throw err
    }

}