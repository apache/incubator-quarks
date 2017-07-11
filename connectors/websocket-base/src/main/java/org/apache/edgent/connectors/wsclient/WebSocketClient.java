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
package org.apache.edgent.connectors.wsclient;

import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TopologyElement;

import com.google.gson.JsonObject;

/**
 * A generic connector for sending and receiving messages to a WebSocket Server.
 * <p>
 * A connector is bound to its configuration specified 
 * {@code javax.websockets} WebSocket URI.
 * <p> 
 * A single connector instance supports sinking at most one stream
 * and sourcing at most one stream.
 * <p>
 * Sample use:
 * <pre>{@code
 * // assuming a properties file containing at least:
 * // ws.uri=ws://myWsServerHost/myService
 *  
 * String propsPath = <path to properties file>; 
 * Properties properties = new Properties();
 * properties.load(Files.newBufferedReader(new File(propsPath).toPath()));
 *
 * Topology t = ...;
 * Jsr356WebSocketClient wsclient = new SomeWebSocketClient(t, properties);
 * 
 * // send a stream's JsonObject tuples as JSON WebSocket text messages
 * TStream<JsonObject> s = ...;
 * wsclient.send(s);
 * 
 * // create a stream of JsonObject tuples from received JSON WebSocket text messages
 * TStream<JsonObject> r = wsclient.receive();
 * r.print();
 * }</pre>
 * <p>
 * Note, the WebSocket protocol differentiates between text/String and 
 * binary/byte messages.
 * A receiver only receives the messages of the type that it requests.
 * <p> 
 * Implementations are strongly encouraged to support construction from
 * Properties with the following configuration parameters:
 * <ul>
 * <li>ws.uri - "ws://host[:port][/path]", "wss://host[:port][/path]"
 *   the default port is 80 and 443 for "ws" and "wss" respectively.
 *   The optional path must match the server's configuration.</li>
 * <li>ws.trustStore - optional. Only used with "wss:".
 *     Path to trust store file in JKS format.
 *     If not set, the standard JRE and javax.net.ssl system properties
 *     control the SSL behavior.
 *     Generally not required if server has a CA-signed certificate.</li>
 * <li>ws.trustStorePassword - required if ws.trustStore is set</li>
 * <li>ws.keyStore - optional. Only used with "wss:" when the
 *     server is configured for client auth.
 *     Path to key store file in JKS format.
 *     If not set, the standard JRE and javax.net.ssl system properties
 *     control the SSL behavior.</li>
 * <li>ws.keyStorePassword - required if ws.keyStore is set.</li>
 * <li>ws.keyPassword - defaults to ws.keyStorePassword value</li>
 * <li>ws.keyCertificateAlias - alias for certificate in key store. defaults to "default"</li>
 * </ul>
 */
public interface WebSocketClient extends TopologyElement {

    /**
     * Send a stream's JsonObject tuples as JSON in a WebSocket text message.
     * @param stream the stream
     * @return sink
     */
    TSink<JsonObject> send(TStream<JsonObject> stream);

    /**
     * Send a stream's String tuples in a WebSocket text message.
     * @param stream the stream
     * @return sink
     */
    TSink<String> sendString(TStream<String> stream);
    
    /**
     * Send a stream's byte[] tuples in a WebSocket binary message.
     * @param stream the stream
     * @return sink
     */
    TSink<byte[]> sendBytes(TStream<byte[]> stream);

    /**
     * Create a stream of JsonObject tuples from received JSON WebSocket text messages.
     * @return the stream
     */
    TStream<JsonObject> receive();
    
    /**
     * Create a stream of String tuples from received WebSocket text messages.
     * <p>
     * Note, the WebSocket protocol differentiates between text/String and
     * binary/byte messages.  This method only receives messages sent as text.
     * 
     * @return the stream
     */
    TStream<String> receiveString();

    /**
     * Create a stream of byte[] tuples from received WebSocket binary messages.
     * <p>
     * Note, the WebSocket protocol differentiates between text/String and
     * binary/byte messages.  This method only receives messages sent as bytes.
     * 
     * @return the stream
     */
    TStream<byte[]> receiveBytes();
    
}
