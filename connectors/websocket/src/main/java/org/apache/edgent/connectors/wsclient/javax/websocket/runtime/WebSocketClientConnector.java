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
package org.apache.edgent.connectors.wsclient.javax.websocket.runtime;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Properties;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.edgent.connectors.runtime.Connector;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.javax.websocket.EdgentSslContainerProvider;
//import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebSocketClientConnector extends Connector<Session> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientConnector.class);
    private final Properties config;
    private volatile String id;
    private volatile String sid;
    private WebSocketClientReceiver<?> msgReceiver;
    private volatile WebSocketContainer container; 
    private final Supplier<WebSocketContainer> containerFn;
    
    public WebSocketClientConnector(Properties config, Supplier<WebSocketContainer> containerFn) {
        Objects.requireNonNull(config, "config");
        this.config = config;
        checkConfig();
        this.containerFn = containerFn!=null
                ? containerFn
                : () -> getWebSocketContainer();
    }
    
    private void checkConfig() {
        requireConfig("ws.uri");
        URI uri = getEndpointURI();
        if (!("ws".equals(uri.getScheme()) || "wss".equals(uri.getScheme())))
            throw new IllegalArgumentException("ws.uri");
        if (optionalConfig("ws.trustStore"))
            requireConfig("ws.trustStorePassword");
        if (optionalConfig("ws.keyStore"))
            requireConfig("ws.keyStorePassword");
    }
    
    void setReceiver(WebSocketClientReceiver<?> msgReceiver) {
        this.msgReceiver = msgReceiver;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    protected Session doConnect(Session session) throws Exception {
        if (session == null || !session.isOpen()) {
            if (session != null)
                doClose(session);
            if (container == null)
                container = containerFn.get();
            URI uri = getEndpointURI();
            getLogger().info("{} connecting uri={}", id(), uri);
            session = container.connectToServer(this, uri);
            updateId(session);
            getLogger().info("{} connected uri={}", id(), uri);
        }
        return session;
    }

    private WebSocketContainer getWebSocketContainer() throws RuntimeException {
        
        // Ugh. Turns out there are some serious issues w/JSR356
        // as well as Jetty client impl of it wrt SSL and
        // trust and key store configurations.
        //
        // "wss" is OK unless: you need **programatic** trustStore
        // OR need clientAuth at all.
        //
        // https://github.com/eclipse/jetty.project/issues/155

        URI uri = getEndpointURI();
        
        // Use the std code for the non-problematic cases
        if ("ws".equals(uri.getScheme())
            || (config.getProperty("ws.trustStore") == null
                && config.getProperty("ws.keyStore") == null
                && System.getProperty("javax.net.ssl.keyStore") == null))
        {
            return ContainerProvider.getWebSocketContainer();
        }
        else {
            getLogger().info("##### Using ContainerProvider.getWebSocketContainer() workaround for SSL #####");
            
            return EdgentSslContainerProvider.getSslWebSocketContainer(config);
        }
    }
    
    private URI getEndpointURI() throws RuntimeException {
        String uriStr = config.getProperty("ws.uri");
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("ws.uri", e);
        }
    }
    
    private void requireConfig(String id) {
        if (config.getProperty(id) == null)
            throw new IllegalArgumentException(id);
    }
    
    private boolean optionalConfig(String id) {
        return config.getProperty(id) != null;
    }

    @Override
    protected void doDisconnect(Session session) throws Exception {
        // no disconnect from javax.websocket.Session
        doClose(session);
    }

    @Override
    protected void doClose(Session session) throws Exception {
        getLogger().debug("{} doClose {}", id(), session);
        try {
            session.close();
        }
        finally {
//            // Force lifecycle stop when done with container.
//            // This is to free up threads and resources that the
//            // JSR-356 container allocates. But unfortunately
//            // the JSR-356 spec does not handle lifecycles (yet)
//            ((LifeCycle)container).stop();
        }
    }
    
    private void updateId(Session session) {
        sid = session.getId();
        id = null;
    }

    @Override
    protected String id() {
        if (id == null) {
            // include our short object Id
            id = "WSCLIENT " + toString().substring(toString().indexOf('@') + 1)
                    + " sid=" + sid;
        }
        return id;
    }
    
    @OnError
    public void onError(Session client, Throwable t) {
        getLogger().error("{} onError {}", id(), t);
    }
    
    @OnMessage
    public void onTextMessage(String message) {
        getLogger().trace("{} onTextMessage {}", id(), message);
        if (msgReceiver != null) {
            msgReceiver.onTextMessage(message);
        }
    }
    
    @OnMessage
    public void onBinaryMessage(byte[] message) {
        getLogger().trace("{} onBinaryMessage {} bytes.", id(), message.length);
        if (msgReceiver != null) {
            msgReceiver.onBinaryMessage(message);
        }
    }

    void sendBinary(byte[] bytes) {
        while (true) {
            Session session = getConnectedSession();
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                getLogger().trace("{} sendBinary {} bytes.", id(), bytes.length);
                return;
            }
            catch (IOException e) {
                if (!session.isOpen()) {
                    connectionLost(e);  // logs error
                    getLogger().error("{} sendBinary {} bytes failed. Retrying following connection lost", id(), bytes.length);
                    // retry
                }
                else {
                    getLogger().error("{} sendBinary {} bytes failed", id(), bytes.length, e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    void sendText(String msg) {
        while (true) {
            Session session = getConnectedSession();
            try {
                session.getBasicRemote().sendText(msg);
                getLogger().trace("{} sendText {}", id(), msg);
                return;
            }
            catch (IOException e) {
                if (!session.isOpen()) {
                    connectionLost(e);  // logs error
                    getLogger().error("{} sendText {} chars failed. Retrying following connection lost", id(), msg.length());
                    // retry
                }
                else {
                    getLogger().error("{} sendText {} chars failed", id(), msg.length(), e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    private Session getConnectedSession() {
        try { 
            return client();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
