/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

import groovy.io.FileType

def jarFile = new File(basedir, "target/without-plugin-1.0-SNAPSHOT.jar")
def testJarFile = new File(basedir, "target/without-plugin-1.0-SNAPSHOT-tests.jar")
def testJarAscFile = new File(basedir, "target/without-plugin-1.0-SNAPSHOT-tests.jar.asc")
def sourceReleaseFile = new File(basedir, "target/without-plugin-1.0-SNAPSHOT-source-release.zip")
def sourceReleaseAscFile = new File(basedir, "target/without-plugin-1.0-SNAPSHOT-source-release.zip.asc")

// The jar file should exist
assert jarFile.exists() && jarFile.isFile()

// The test-jar and it's signature should also exist
assert testJarFile.exists() && testJarFile.isFile()
assert testJarAscFile.exists() && testJarAscFile.isFile()

// The source release zip and it's signature should exist
assert sourceReleaseFile.exists() && sourceReleaseFile.isFile()
assert sourceReleaseAscFile.exists() && sourceReleaseAscFile.isFile()

// The local repo should contain all expected files.
def jarLocalRepo = new File("target/maven-repos/local/org/apache/edgent/plugins/it/without-plugin/1.0-SNAPSHOT")
assert jarLocalRepo.exists()
def foundTestJarInLocal = false
def foundTestJarAscInLocal = false
def foundSourceReleaseZipInLocal = false
def foundSourceReleaseZipAscInLocal = false
jarLocalRepo.eachFileRecurse (FileType.FILES) { file ->
    println file.name
    if(file.name.endsWith("tests.jar")) {
        foundTestJarInLocal = true
    }
    if(file.name.endsWith("tests.jar.asc")) {
        foundTestJarAscInLocal = true
    }
    if(file.name.endsWith("source-release.zip")) {
        foundSourceReleaseZipInLocal = true
    }
    if(file.name.endsWith("source-release.zip.asc")) {
        foundSourceReleaseZipAscInLocal = true
    }
}
assert foundTestJarInLocal && foundTestJarAscInLocal && foundSourceReleaseZipInLocal && foundSourceReleaseZipAscInLocal

// The remote repo should also contain all of them.
def jarRemoteRepo = new File("target/maven-repos/remote/org/apache/edgent/plugins/it/without-plugin/1.0-SNAPSHOT")
assert jarRemoteRepo.exists()
def foundTestJarInRemote = false
def foundTestJarAscInRemote = false
def foundSourceReleaseZipInRemote = false
def foundSourceReleaseZipAscInRemote = false
jarRemoteRepo.eachFileRecurse (FileType.FILES) { file ->
    println file.name
    if(file.name.endsWith("tests.jar")) {
        foundTestJarInRemote = true
    }
    if(file.name.endsWith("tests.jar.asc")) {
        foundTestJarAscInRemote = true
    }
    if(file.name.endsWith("source-release.zip")) {
        foundSourceReleaseZipInRemote = true
    }
    if(file.name.endsWith("source-release.zip.asc")) {
        foundSourceReleaseZipAscInRemote = true
    }
}
assert foundTestJarInRemote && foundTestJarAscInLocal && foundSourceReleaseZipInLocal && foundSourceReleaseZipAscInLocal
