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
package org.apache.edgent.connectors.wsclient.javax.websocket;

import java.util.Objects;
import java.util.Properties;

import javax.websocket.WebSocketContainer;

import org.apache.edgent.connectors.wsclient.WebSocketClient;
import org.apache.edgent.connectors.wsclient.javax.websocket.runtime.WebSocketClientBinaryReceiver;
import org.apache.edgent.connectors.wsclient.javax.websocket.runtime.WebSocketClientBinarySender;
import org.apache.edgent.connectors.wsclient.javax.websocket.runtime.WebSocketClientConnector;
import org.apache.edgent.connectors.wsclient.javax.websocket.runtime.WebSocketClientReceiver;
import org.apache.edgent.connectors.wsclient.javax.websocket.runtime.WebSocketClientSender;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.json.JsonFunctions;

import com.google.gson.JsonObject;

/**
 * A connector for sending and receiving messages to a WebSocket Server.
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
 * Jsr356WebSocketClient wsclient = new Jsr356WebSocketClient(t, properties);
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
 * The connector is written against the JSR356 {@code javax.websockets} API.
 * {@code javax.websockets} uses the {@link java.util.ServiceLoader} to load
 * an implementation of {@code javax.websocket.ContainerProvider}.
 * <p>
 * The supplied {@code connectors/javax.websocket-client} provides one
 * such implementation. To use it, include
 * {@code connectors/javax.websocket-client/lib/javax.websocket-client.jar}
 * on your classpath.
 */
public class Jsr356WebSocketClient implements WebSocketClient{
    private final Topology t;
    private final WebSocketClientConnector connector;
    private int senderCnt;
    private int receiverCnt;

    /**
     * Create a new Web Socket Client connector.
     * <p>
     * Configuration parameters:
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
     * Additional keys in {@code config} are ignored.
     * @param t the topology to add the connector to
     * @param config the connector's configuration
     */
    public Jsr356WebSocketClient(Topology t, Properties config) {
        this(t, config, null);
    }
    
    /**
     * Create a new Web Socket Client connector.
     * <p>
     * This constructor is made available in case the container created
     * by {@link Jsr356WebSocketClient#Jsr356WebSocketClient(Topology, Properties)}
     * lacks the configuration needed for a particular use case.
     * <p>
     * At topology runtime {@code containerFn.get()} will be called to
     * get a {@code javax.websocket.WebSocketContainer} that will be used to
     * connect to the WebSocket server.
     * <p>
     * Only the "ws.uri" {@code config} parameter is used.
     * @param t the topology to add the connector to
     * @param config the connector's configuration
     * @param containerFn supplier for a {@code WebSocketContainer}.  May be null.
     */
    public Jsr356WebSocketClient(Topology t, Properties config, Supplier<WebSocketContainer> containerFn) {
        Objects.requireNonNull(t, "t");
        Objects.requireNonNull(config, "config");
        this.t = t;
        this.connector = new WebSocketClientConnector(config, containerFn);
    }

    /**
     * Send a stream's JsonObject tuples as JSON in a WebSocket text message.
     * @param stream the stream
     * @return sink
     */
    public TSink<JsonObject> send(TStream<JsonObject> stream) {
        Objects.requireNonNull(stream, "stream");
        return sendText(stream, JsonFunctions.asString());
    }

    /**
     * Send a stream's String tuples in a WebSocket text message.
     * @param stream the stream
     * @return sink
     */
    public TSink<String> sendString(TStream<String> stream) {
        Objects.requireNonNull(stream, "stream");
        return sendText(stream, tuple -> tuple);
    }
    
    private <T> TSink<T> sendText(TStream<T> stream, Function<T,String> toPayload) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(toPayload, "toPayload");
        checkAddSender();
        return stream.sink(new WebSocketClientSender<T>(connector, toPayload));
    }
    
    /**
     * Send a stream's byte[] tuples in a WebSocket binary message.
     * @param stream the stream
     * @return sink
     */
    public TSink<byte[]> sendBytes(TStream<byte[]> stream) {
        Objects.requireNonNull(stream, "stream");
        return sendBinary(stream, tuple -> tuple);
    }
    
    private <T> TSink<T> sendBinary(TStream<T> stream, Function<T,byte[]> toPayload) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(toPayload, "toPayload");
        checkAddSender();
        return stream.sink(new WebSocketClientBinarySender<T>(connector, toPayload));
    }
    
    private void checkAddSender() throws IllegalStateException {
        // enforce a sender restriction to match the receiver restriction.
        if (++senderCnt > 1)
            throw new IllegalStateException("More than one sender specified");
    }

    /**
     * Create a stream of JsonObject tuples from received JSON WebSocket text messages.
     * @return the stream
     */
    public TStream<JsonObject> receive() {
        return receiveText(JsonFunctions.fromString());
    }
    
    /**
     * Create a stream of String tuples from received WebSocket text messages.
     * <p>
     * Note, the WebSocket protocol differentiates between text/String and
     * binary/byte messages.  This method only receives messages sent as text.
     * 
     * @return the stream
     */
    public TStream<String> receiveString() {
        return receiveText(tuple -> tuple);
    }
    
    private <T> TStream<T> receiveText(Function<String,T> toTuple) {
        checkAddReceiver();
        return t.events(new WebSocketClientReceiver<T>(connector, toTuple));
    }

    /**
     * Create a stream of byte[] tuples from received WebSocket binary messages.
     * <p>
     * Note, the WebSocket protocol differentiates between text/String and
     * binary/byte messages.  This method only receives messages sent as bytes.
     * 
     * @return the stream
     */
    public TStream<byte[]> receiveBytes() {
        return receiveBinary(payload -> payload);
    }
    
    private <T> TStream<T> receiveBinary(Function<byte[],T> toTuple) {
        checkAddReceiver();
        return t.events(new WebSocketClientBinaryReceiver<T>(connector, toTuple));
    }
    
    private void checkAddReceiver() throws IllegalStateException {
        // there's no good reason for the base functionality to support
        // multiple receivers so don't.  The underlying implementation
        // is simplified with this restriction.
        if (++receiverCnt > 1)
            throw new IllegalStateException("More than one receiver specified");
    }

    @Override
    public Topology topology() {
        return t;
    }
    
}
