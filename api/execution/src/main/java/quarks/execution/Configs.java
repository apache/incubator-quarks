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
package quarks.execution;

/**
 * Configuration property names.
 * 
 * Configuration is passed as a JSON collection of name/value
 * pairs when an executable is 
 * {@linkplain quarks.execution.Submitter#submit(Object, com.google.gson.JsonObject) submitted}.
 * <p>
 * The configuration JSON representation is summarized in the following table:
 *
 * <table border=1 cellpadding=3 cellspacing=1>
 * <caption>Summary of configuration properties</caption>
 * <tr>
 *    <td align=center><b>Attribute name</b></td>
 *    <td align=center><b>Type</b></td>
 *    <td align=center><b>Description</b></td>
 *  </tr>
 * <tr>
 *    <td>{@link #JOB_NAME jobName}</td>
 *    <td>String</td>
 *    <td>The name of the job.</td>
 *  </tr>
 * </table>
 * </p>
 */
public interface Configs {
    /**
     * JOB_NAME is used to identify the submission configuration property 
     * containing the job name.
     * The value is {@value}.
     */
    String JOB_NAME = "jobName";
}
