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
package org.apache.edgent.test.connectors.kafka;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/*
 * Our current maven/gradle driven test config (test filtering with
 * includeTestsMatching '*Test') results in failing a project's
 * test task if the project lacks any "*Test" classes.
 * 
 * This class avoids that condition.
 */
public class KafkaStreamsSkipMeTest {
    
    @Test
    public void testSkipMe() throws Exception {
      assumeTrue(false);
    }

}
