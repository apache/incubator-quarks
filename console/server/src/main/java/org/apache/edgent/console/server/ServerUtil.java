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

package org.apache.edgent.console.server;

import java.io.File;

public class ServerUtil {

	/**
	 *  The public constructor of this utility class for use by the HttpServer class.
	 */
    public ServerUtil() {
    }

    /**
     * Returns the File object representing the "webapps" directory
     * @return a File object or null if the "webapps" directory is not found
     */
    public File getWarFilePath() {
        return new File("target/war-resources/servlets.war");
    }
    
}
