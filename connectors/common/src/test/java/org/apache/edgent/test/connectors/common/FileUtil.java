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

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * File utilities for tests
 */
public class FileUtil {

  /**
   * Create a temp file with the specified name, extension and contents.
   * @param name filename prefix 
   * @param extension filename extension
   * @param lines content for the file
   * @return {@code Path} to temp file
   * @throws Exception on failure
   */
  public static Path createTempFile(String name, String extension, String[] lines) throws Exception {
      Path tmpFile = Files.createTempFile(name, extension);
      tmpFile.toFile().deleteOnExit();
      
      try (BufferedWriter bw = 
            new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(tmpFile.toFile()), StandardCharsets.UTF_8)))
      {
        for (int i = 0; i < lines.length; i++) {
            bw.write(lines[i]);
            bw.write("\n");
        }
        return tmpFile;
      }
      catch (Exception e) {
        tmpFile.toFile().delete();
        throw e;
      }
  }
  
  /**
   * Validate that the file contains the specified content.
   * @param path file to validate
   * @param lines the expected content
   * @throws Exception on failure
   */
  public static void validateFile(Path path, String[] lines) throws Exception {
    List<String> actLines = new ArrayList<>();
    try (BufferedReader reader = 
          new BufferedReader(new InputStreamReader(
            new FileInputStream(path.toFile()), StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = reader.readLine()) != null) {
        actLines.add(line);
      }
      assertArrayEquals(lines, actLines.toArray(new String[actLines.size()]));
    }
  }

}
