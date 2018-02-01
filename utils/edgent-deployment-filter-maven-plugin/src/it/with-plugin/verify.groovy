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

def jarFile = new File(basedir, "target/with-plugin-1.0-SNAPSHOT.jar")
def testJarFile = new File(basedir, "target/with-plugin-1.0-SNAPSHOT-tests.jar")

// The jar file should exist
assert jarFile.exists() && jarFile.isFile()

// The test-jar should also exist
assert testJarFile.exists() && testJarFile.isFile()

// The local repo should contain the test-jar.
def jarLocalRepo = new File("target/maven-repos/local/org/apache/edgent/plugins/it/with-plugin/1.0-SNAPSHOT")
assert jarLocalRepo.exists()
def foundTestJarInLocal = false
jarLocalRepo.eachFileRecurse (FileType.FILES) { file ->
    println file.name
    if(file.name.endsWith("tests.jar")) {
        foundTestJarInLocal = true
    }
}
assert foundTestJarInLocal

// The remote repo shouldn't contain it.
def jarRemoteRepo = new File("target/maven-repos/remote/org/apache/edgent/plugins/it/with-plugin/1.0-SNAPSHOT")
assert jarRemoteRepo.exists()
def foundTestJarInRemote = false
jarRemoteRepo.eachFileRecurse (FileType.FILES) { file ->
    println file.name
    if(file.name.endsWith("tests.jar")) {
        foundTestJarInRemote = true
    }
}
assert !foundTestJarInRemote
