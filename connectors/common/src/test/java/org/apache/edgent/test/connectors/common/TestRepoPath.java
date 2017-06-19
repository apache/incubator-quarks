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
package org.apache.edgent.test.connectors.common;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility for tests to get the path to something in the local git repository.
 */
public class TestRepoPath {

    /**
     * Get an absolute path to something in the local git repository.
     * <p>
     * Deals with implications of the different execution contexts:
     * eclipse/junit and ant/junit.
     * 
     * @param classpathPath the absolute path of the resource in the applications classpath.
     * @return absolute path in the repository
     */
    public static String getPath(String classpathPath) {
        try {
            URL resourceUrl = ClassLoader.getSystemResource(classpathPath);
            File resource = new File(resourceUrl.toURI());
            return resource.getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not get path of resource; " + classpathPath, e);
        }
    }

}
