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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.apache.edgent.test.connectors.common.TestRepoPath;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * Simple WebSocket server program to echo received messages.
 * <p>
 * See https://github.com/jetty-project/embedded-jetty-websocket-examples
 */
@ServerEndpoint(value="/echo")
public class WebSocketServerEcho {
    private final String svrName = this.getClass().getSimpleName();
    private Server server;
    private ServerConnector connector;
    private URI curEndpointURI;
    private boolean curNeedClientAuth;
    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(0);
    
    public static void main(String[] args) throws Exception {
        URI uri = new URI("ws://localhost:0");
        boolean needClientAuth = false;
        if (args.length > 0)
            uri = new URI(args[0]);
        if (args.length > 1)
            needClientAuth = "needClientAuth".equals(args[1]);
        WebSocketServerEcho srvr = new WebSocketServerEcho();
        srvr.start(uri, needClientAuth);
    }
    
    public void start(URI endpointURI) {
        start(endpointURI, false);
    }
    
    public void start(URI endpointURI, boolean needClientAuth) {
        curEndpointURI = endpointURI;
        curNeedClientAuth = needClientAuth;

        System.out.println(svrName+" "+endpointURI + " needClientAuth="+needClientAuth);

        server = createServer(endpointURI, needClientAuth);
        connector = (ServerConnector)server.getConnectors()[0];

        // Setup the basic application "context" for this application at "/"
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        try {
            // Initialize javax.websocket layer
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(this.getClass());

            // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list

            server.start();
            System.out.println(svrName+" started "+connector);
            // server.dump(System.err);            
        }
        catch (Exception e) {
            throw new RuntimeException("start", e);
        }
    }
    
    private Server createServer(URI endpointURI, boolean needClientAuth) {
        if ("ws".equals(endpointURI.getScheme())) {
            return new Server(endpointURI.getPort());
        }
        else if ("wss".equals(endpointURI.getScheme())) {
            // see http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java
            //     http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java
            
            Server server = new Server();
            
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getStorePath("serverKeyStore.jks"));
            sslContextFactory.setKeyStorePassword("passw0rd");
            sslContextFactory.setKeyManagerPassword("passw0rd");
            sslContextFactory.setCertAlias("default");
            sslContextFactory.setNeedClientAuth(needClientAuth);
            sslContextFactory.setTrustStorePath(getStorePath("serverTrustStore.jks"));
            sslContextFactory.setTrustStorePassword("passw0rd");
            
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            
            ServerConnector https= new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory,
                            HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            https.setPort(endpointURI.getPort());
            
            server.addConnector(https);
            return server;
        }
        else
            throw new IllegalArgumentException("unrecognized uri: "+endpointURI);
    }
    
    private String getStorePath(String storeLeaf) {
        return TestRepoPath.getPath("keystores/" + storeLeaf);
    }
    
    public int getPort() {
        // returns -1 if called before started
        return connector.getLocalPort();
    }
    
    /** restart a running server on the same port, etc: stop, delay, start
     * @param secDelay the amount to delay in seconds before initiating the restart 
     */
    public void restart(int secDelay) {
        // stop, schedule delay&start and return
        URI endpointURI = setPort(curEndpointURI, getPort());
        try {
            System.out.println(svrName+" restart: stop "+connector);
            connector.stop();
        } catch (Exception e) {
            throw new RuntimeException("restart", e);
        }
        System.out.println(svrName+" restart: scheduling start after "+secDelay+"sec");
        schedExecutor.schedule(() -> {
            System.out.println(svrName+" restart: starting...");
            start(endpointURI, curNeedClientAuth);
        }, secDelay, TimeUnit.SECONDS);
    }
    
    private URI setPort(URI endpointURI, int port) {
        try {
            URI uri = endpointURI;
            if (uri.getPort() != port) {
                uri = new URI(uri.getScheme(), uri.getUserInfo(),
                        uri.getHost(), port,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to create URI", e);
        }
    }
    
    public void stop() {
        if (connector != null) {
            try {
                System.out.println(svrName+" stop "+connector);
                connector.stop();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                connector = null;
            }
        }
    }
    
    @OnOpen
    public void opOpen(Session session) {
        System.out.println(svrName+" onOpen ");
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println(svrName+" onClose reason="+reason);
    }
    
    @OnMessage
    public void onStringMessage(Session session, String message) {
        System.out.println(svrName+" onStringMessage msg="+message);
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  // "echo" response
    }
    
    @OnMessage
    public void onByteMessage(Session session, ByteBuffer message) {
        System.out.println(svrName+" onByteMessage "+message.array().length+" bytes");
        try {
            session.getBasicRemote().sendBinary(message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  // "echo" response
    }
    
    @OnError
    public void onError(Throwable cause) {
        System.err.println(svrName+" onError " + cause);
        cause.printStackTrace(System.err);
    }

}