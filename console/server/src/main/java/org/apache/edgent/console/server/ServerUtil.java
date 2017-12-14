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

import java.io.*;

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
        File war = new File("target/war-resources/servlets.war");
        if(!war.exists()) {
            // Eventually create the directory for serving the war.
            if(!war.getParentFile().exists()) {
                if(!war.getParentFile().mkdirs()) {
                    throw new RuntimeException("Could not create output directory at " + war.getAbsolutePath());
                }
            }

            // Dump the servlet.war into the output directory.
            InputStream stream = null;
            FileOutputStream fileOutputStream = null;
            try {
                stream = ServerUtil.class.getResourceAsStream("/resources/servlets.war");
                if(stream == null) {
                    throw new RuntimeException("Could not get resource '/resources/servlets.war' from classpath.");
                }

                int readBytes;
                byte[] buffer = new byte[4096];
                fileOutputStream = new FileOutputStream(war);
                while ((readBytes = stream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, readBytes);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not dump resource 'resources/servlets.war' from classpath.", e);
            } finally {
                if(stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Ignore.
                    }
                }
                if(fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        // Ignore.
                    }
                }
            }
        }

        return war;
    }

}
