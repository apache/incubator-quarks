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
package org.apache.edgent.test.connectors.wsclient.javax.websocket;

import org.junit.Ignore;

/**
 * WebSocketClient connector globalization tests.
 */
//@Ignore("There seem to be issues with SSL related tests")
public class WebSocketClientGlobalTest extends WebSocketClientTest {
    private final static String globalStr1 = "一";
    private final static String globalStr2 = "二";
    private final static String globalStr3 = "三三";
    private final static String globalStr4 = "四";

    public String getStr1() {
        return globalStr1;
    }

    public String getStr2() {
        return globalStr2;
    }

    public String getStr3() {
        return globalStr3;
    }

    public String getStr4() {
        return globalStr4;
    }

}
