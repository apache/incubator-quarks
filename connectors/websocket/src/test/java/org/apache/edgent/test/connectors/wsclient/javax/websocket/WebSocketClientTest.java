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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assume.assumeTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.edgent.connectors.wsclient.WebSocketClient;
import org.apache.edgent.connectors.wsclient.javax.websocket.Jsr356WebSocketClient;
import org.apache.edgent.test.connectors.common.ConnectorTestBase;
import org.apache.edgent.test.connectors.common.TestRepoPath;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.json.JsonFunctions;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;

//@Ignore("There seem to be issues with SSL related tests")
public class WebSocketClientTest extends ConnectorTestBase {
    private final static int SEC_TMO = 5;
    WebSocketServerEcho wsServer;
    boolean isExternalServer;// = true;
    int wsServerPort = !isExternalServer ? 0 : 49460;
    String wsUriPath = "/echo";  // match what WsServerEcho is using
    private final static String str1 = "one";
    private final static String str2 = "two";
    private final static String str3 = "three-post-reconnect";
    private final static String str4 = "four";
    
    public String getStr1() {
        return str1;
    }

    public String getStr2() {
        return str2;
    }

    public String getStr3() {
        return str3;
    }

    public String getStr4() {
        return str4;
    }

    @After
    public void cleanup() {
        if (wsServer != null)
            wsServer.stop();
        wsServer = null;
    }
    
    private enum ServerMode { WS, SSL, SSL_CLIENT_AUTH }
    private void startEchoer() {
        startEchoer(ServerMode.WS);
    }
    private void startEchoer(ServerMode mode) {
        try {
            if (!isExternalServer) {
                URI uri;
                if (mode==ServerMode.WS) {
                    uri = new URI("ws://localhost:0");
                    wsServer = new WebSocketServerEcho();
                }
                else {
                    uri = new URI("wss://localhost:0");
                    wsServer = new WebSocketServerEcho();
                }
                wsServer.start(uri, mode==ServerMode.SSL_CLIENT_AUTH);
                wsServerPort = wsServer.getPort();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException("startEchoer",e );
        }
    }
    private void restartEchoer(int secDelay) {
        wsServer.restart(secDelay);
    }
    
    Properties getConfig() {
        return getWsConfig();
    }

    Properties getWsConfig() {
        Properties config = new Properties();
        config.setProperty("ws.uri", getWsUri());
        return config;
    }

    Properties getWssConfig() {
        Properties config = new Properties();
        config.setProperty("ws.uri", getWssUri());
        config.setProperty("ws.trustStore", getStorePath("clientTrustStore.jks"));
        config.setProperty("ws.trustStorePassword", "passw0rd");
        config.setProperty("ws.keyStore", getStorePath("clientKeyStore.jks"));
        config.setProperty("ws.keyStorePassword", "passw0rd");
        // default: expect key to have the default alias
        return config;
    }
    
    String getWsUri() {
        int port = wsServerPort==0 ? 8080 : wsServerPort;
        return "ws://localhost:"+port+wsUriPath;
    }
    
    String getWssUri() {
        int port = wsServerPort==0 ? 443 : wsServerPort;
        return "wss://localhost:"+port+wsUriPath;
    }
    
    private String getStorePath(String storeLeaf) {
        return TestRepoPath.getPath("keystores/" + storeLeaf);
    }

    @Test
    public void testBasicStaticStuff() {
        Topology t = newTopology("testBasicStaticStuff");

        Properties config = getConfig();
        WebSocketClient wsClient1 = new Jsr356WebSocketClient(t, config);
        
        TStream<String> s1 = wsClient1.receiveString();
        assertNotNull("s1", s1);
        
        TSink<String> sink1 = wsClient1.sendString(t.strings(getStr1(), getStr2()));
        assertNotNull("sink1", sink1);
        
        WebSocketClient wsClient2 = new Jsr356WebSocketClient(t, config);
        TStream<String> s2 = wsClient2.receiveString();
        assertNotSame("s1 s2", s1, s2);
        
        TSink<String> sink2 = wsClient2.sendString(t.strings(getStr1(), getStr2()));
        assertNotSame("sink1 sink2", sink1, sink2);        
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMissingWsUri() {
        Topology t = newTopology("testMissingWsUri");
        new Jsr356WebSocketClient(t, new Properties());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMalformedWsUri() {
        Topology t = newTopology("testMalformedWsUri");
        Properties config = new Properties();
        config.setProperty("ws.uri", "localhost"); // missing scheme
        new Jsr356WebSocketClient(t, config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNotWsUri() {
        Topology t = newTopology("testNotWsUri");
        Properties config = new Properties();
        config.setProperty("ws.uri", "tcp://localhost");
        new Jsr356WebSocketClient(t, config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWssTrustStorePasswordNeg() {
        Topology t = newTopology("testWssTrustStorePasswordNeg");
        Properties config = new Properties();
        config.setProperty("ws.uri", getWssUri());
        config.setProperty("ws.trustStore", "xyzzy"); // not checked till runtime
        // missing trustStorePassword
        new Jsr356WebSocketClient(t, config);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWssKeyStorePasswordNeg() {
        Topology t = newTopology("testWssKeyStorePasswordNeg");
        Properties config = new Properties();
        config.setProperty("ws.uri", getWssUri());
        config.setProperty("ws.keyStore", "xyzzy"); // not checked till runtime
        // missing keyStorePassword
        new Jsr356WebSocketClient(t, config);
    }
    
    @Test
    public void testWssConfig() {
        Topology t = newTopology("testWssConfig");
        Properties config = new Properties();
        config.setProperty("ws.uri", getWssUri());
        config.setProperty("ws.trustStore", "xyzzy"); // not checked till runtime
        config.setProperty("ws.trustStorePassword", "xyzzy"); // not checked till runtime
        new Jsr356WebSocketClient(t, config);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testTooManySendersNeg() {
        Topology t = newTopology("testTooManySendersNeg");
        TStream<String> s1 = t.strings(getStr1(), getStr2());
        TStream<String> s2 = t.strings(getStr1(), getStr2());

        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        wsClient.sendString(s1);
        wsClient.sendString(s2); // should throw
    }
    
    @Test(expected = IllegalStateException.class)
    public void testTooManyReceiversNeg() {
        Topology t = newTopology("testTooManyReceiversNeg");

        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        @SuppressWarnings("unused")
        TStream<String> s1 = wsClient.receiveString();
        @SuppressWarnings("unused")
        TStream<String> s2 = wsClient.receiveString(); // should throw
    }
    
    @Test
    public void testJson() throws Exception {
        Topology t = newTopology("testJson");
        System.out.println("===== "+t.getName());
        
        startEchoer();  // before getConfig() so it gets the port
        
        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] {
                "{\"id\":\"" + getStr1() + "\",\"value\":27}",
                "{\"id\":\"" + getStr2() + "\",\"value\":13}"
        };
        
        TStream<JsonObject> s = t.strings(expected)
                                .map(JsonFunctions.fromString());
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.send(s);
        
        TStream<String> rcvd = wsClient.receive()
                                .map(JsonFunctions.asString());
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testString() throws Exception {
        Topology t = newTopology("testString");
        System.out.println("===== "+t.getName());
        
        startEchoer();  // before getConfig() so it gets the port
        
        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testBytes() throws Exception {
        Topology t = newTopology("testBytes");
        System.out.println("===== "+t.getName());

        startEchoer();  // before getConfig() so it gets the port
        
        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<byte[]> s = t.strings(expected)
                                .map(tup -> tup.getBytes(StandardCharsets.UTF_8));
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendBytes(s);
        
        TStream<String> rcvd = wsClient.receiveBytes()
                                .map(tup -> new String(tup, StandardCharsets.UTF_8));
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testReconnect() throws Exception {
        /*
         * It's becomming apparent that the reconnect series of tests
         * aren't reliable so skip them for ci. See jira EDGENT-122 for
         * more info.
         */
        assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology t = newTopology("testReconnect");
        System.out.println("===== "+t.getName());

        startEchoer();  // before getConfig() so it gets the port

        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2(), getStr3(), getStr4() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        
        // send one, two, restart the server to force reconnect, send the next
        AtomicInteger numSent = new AtomicInteger();
        int restartAfterTupleCnt = 2;
        CountDownLatch latch = new CountDownLatch(restartAfterTupleCnt);
        s = s.filter(tuple -> {
            if (numSent.getAndIncrement() != restartAfterTupleCnt )
                return true;
            else {
                // to keep validation sane/simple wait till the tuples are rcvd before restarting
                try { latch.await(); } catch (Exception e) {};
                restartEchoer(2/*secDelay*/);
                return true;
            }
        });
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString()
                                    .peek(tuple -> latch.countDown());

        
        completeAndValidate("", t, rcvd, SEC_TMO + 10, expected);
    }
    
    @Test
    public void testReconnectBytes() throws Exception {
        /*
         * It's becomming apparent that the reconnect series of tests
         * aren't reliable so skip them for ci. See jira EDGENT-122 for
         * more info.
         */
        assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology t = newTopology("testReconnectBytes");
        System.out.println("===== "+t.getName());

        startEchoer();  // before getConfig() so it gets the port

        Properties config = getConfig();
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2(), getStr3(), getStr4() };
        
        TStream<byte[]> s = t.strings(expected).map(tup -> tup.getBytes(StandardCharsets.UTF_8));
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        
        // send one, two, restart the server to force reconnect, send the next
        AtomicInteger numSent = new AtomicInteger();
        int restartAfterTupleCnt = 2;
        CountDownLatch latch = new CountDownLatch(restartAfterTupleCnt);
        s = s.filter(tuple -> {
            if (numSent.getAndIncrement() != restartAfterTupleCnt )
                return true;
            else {
                // to keep validation sane/simple wait till the tuples are rcvd before restarting
                try { latch.await(); } catch (Exception e) {};
                restartEchoer(2/*secDelay*/);
                return true;
            }
        });
        wsClient.sendBytes(s);
        
        TStream<String> rcvd = wsClient.receiveBytes()
                                .peek(tuple -> latch.countDown())
                                .map(tup -> new String(tup, StandardCharsets.UTF_8));
        
        completeAndValidate("", t, rcvd, SEC_TMO + 10, expected);
    }

    private class SslSystemPropMgr {
        private final Map<String,String> origProps = new HashMap<>();
        
        public void set() {
            set("javax.net.ssl.trustStore", getStorePath("clientTrustStore.jks"));
            set("javax.net.ssl.trustStorePassword", "passw0rd");
            set("javax.net.ssl.keyStore", getStorePath("clientKeyStore.jks"));
            set("javax.net.ssl.keyStorePassword", "passw0rd");
        }
        
        private void set(String prop, String defaultVal) {
            origProps.put(prop, System.setProperty(prop, defaultVal));
        }
        
        public void restore() {
            restore("javax.net.ssl.trustStore");
            restore("javax.net.ssl.trustStorePassword");
            restore("javax.net.ssl.keyStore");
            restore("javax.net.ssl.keyStorePassword");
        }
        
        private void restore(String prop) {
            String origValue = origProps.get(prop);
            if (origValue == null)
                System.getProperties().remove(prop);
            else
                System.setProperty(prop, origValue);
        }
    }
    
    @Test
    public void testSslSystemProperty() throws Exception {
        Topology t = newTopology("testSslSystemProperty");
        System.out.println("===== "+t.getName());
        
        startEchoer(ServerMode.SSL);  // before getConfig() so it gets the port
        
        Properties config = getConfig();  // no SSL config stuff
        config.setProperty("ws.uri", getWssUri());

        SslSystemPropMgr sslProps = new SslSystemPropMgr();
        try {
            // a trust store that contains the server's cert
            sslProps.set();
    
            // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
            
            WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
            
            String[] expected = new String[] { getStr1(), getStr2() };
            
            TStream<String> s = t.strings(expected);
            s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
            wsClient.sendString(s);
            
            TStream<String> rcvd = wsClient.receiveString();
            
            completeAndValidate("", t, rcvd, SEC_TMO, expected);
        }
        finally {
            sslProps.restore();
        }
    }
    
    @Test
    public void testSslClientAuthSystemProperty() throws Exception {
        Topology t = newTopology("testSslClientAuthSystemProperty");
        System.out.println("===== "+t.getName());
        
        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port
        
        Properties config = getConfig();  // no SSL config stuff
        config.setProperty("ws.uri", getWssUri());

        SslSystemPropMgr sslProps = new SslSystemPropMgr();
        try {
            // a trust store that contains the server's cert
            sslProps.set();
    
            // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
            
            WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
            
            String[] expected = new String[] { getStr1(), getStr2() };
            
            TStream<String> s = t.strings(expected);
            s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
            wsClient.sendString(s);
            
            TStream<String> rcvd = wsClient.receiveString();
            
            completeAndValidate("", t, rcvd, SEC_TMO, expected);
        }
        finally {
            sslProps.restore();
        }
    }
    
    @Test
    public void testSsl() throws Exception {

        Topology t = newTopology("testSsl");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL);  // before getConfig() so it gets the port
        
        Properties config = getWssConfig();

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
     @Test
     public void testSslReconnect() throws Exception {
         /*
          * It's becomming apparent that the reconnect series of tests
          * aren't reliable so skip them for ci. See jira EDGENT-122 for
          * more info.
          */
         assumeTrue(!Boolean.getBoolean("edgent.build.ci"));
    
         Topology t = newTopology("testSslReconnect");
         System.out.println("===== "+t.getName());
    
         startEchoer(ServerMode.SSL);  // before getConfig() so it gets the port
    
         Properties config = getWssConfig();
         WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
         
         String[] expected = new String[] { getStr1(), getStr2(), getStr3(), getStr4() };
         
         TStream<String> s = t.strings(expected);
         s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
         
         // send one, two, restart the server to force reconnect, send the next
         AtomicInteger numSent = new AtomicInteger();
         int restartAfterTupleCnt = 2;
         CountDownLatch latch = new CountDownLatch(restartAfterTupleCnt);
         s = s.filter(tuple -> {
             if (numSent.getAndIncrement() != restartAfterTupleCnt )
                 return true;
             else {
                 // to keep validation sane/simple wait till the tuples are rcvd before restarting
                 try { latch.await(); } catch (Exception e) {};
                 restartEchoer(2/*secDelay*/);
                 return true;
             }
         });
         wsClient.sendString(s);
         
         TStream<String> rcvd = wsClient.receiveString()
                                 .peek(tuple -> latch.countDown());
         
         completeAndValidate("", t, rcvd, SEC_TMO + 10, expected);
     }
    
    @Test
    public void testSslNeg() throws Exception {

        Topology t = newTopology("testSslNeg");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL);  // before getConfig() so it gets the port
        
        // since our server uses a self-signed cert, if we don't have
        // a truststore setup with it in it, the client will fail to connect
        // and ultimately the connect will fail and the test will
        // receive nothing.

        Properties config = getConfig();  // no SSL config stuff
        config.setProperty("ws.uri", getWssUri());

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, new String[0]);  //rcv nothing
    }
    
    @Test
    public void testSslClientAuth() throws Exception {

        Topology t = newTopology("testSslClientAuth");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port
        
        Properties config = getWssConfig();

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testSslClientAuthDefault() throws Exception {

        Topology t = newTopology("testSslClientAuthDefault");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port
        
        // explicitly specify client's "default" certificate
        Properties config = getWssConfig();
        config.setProperty("ws.keyCertificateAlias", "default");

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testSslClientAuthMy2ndCertNeg() throws Exception {

        Topology t = newTopology("testSslClientAuthMy2ndCertNeg");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port
        
        // explicitly specify client's "my2ndcert" certificate - unknown to server
        Properties config = getWssConfig();
        config.setProperty("ws.keyCertificateAlias", "my2ndcert");

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, new String[0]); // rcv nothing
    }
    
    @Test
    public void testSslClientAuthMy3rdCert() throws Exception {

        Topology t = newTopology("testSslClientAuthMy3rdCert");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port
        
        // explicitly specify client's "my3rdcert" certificate
        Properties config = getWssConfig();
        config.setProperty("ws.keyCertificateAlias", "my3rdcert");

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testSslClientAuthNeg() throws Exception {

        Topology t = newTopology("testSslClientAuthNeg");
        System.out.println("===== "+t.getName());

        startEchoer(ServerMode.SSL_CLIENT_AUTH);  // before getConfig() so it gets the port

        // since our server will require client auth, if we don't have
        // a keystore setup with it in it, the client will fail to connect
        // and ultimately the connect will fail and the test will
        // receive nothing.

        Properties config = getConfig();  // no SSL config stuff
        config.setProperty("ws.uri", getWssUri());
        
        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, new String[0]);  //rcv nothing
    }
    
    private void skipTestIfCantConnect(Properties config) throws Exception {
        String wsUri = config.getProperty("ws.uri");
        // Skip tests if the WebSocket server can't be contacted.
        try {
            URI uri = new URI(wsUri);
            int port = uri.getPort();
            if (port == -1)
                port = uri.getScheme().equals("ws") ? 80 : 443;
            Socket s = new Socket();
            s.connect(new InetSocketAddress(uri.getHost(), port), 5*1000/*cn-timeout-msec*/);
            s.close();
        } catch (Exception e) {
            System.err.println("Unable to connect to WebSocket server "+wsUri+" : "+e.getMessage());
            e.printStackTrace();
            assumeTrue(false);
        }
    }
    
    @Test
    public void testPublicServer() throws Exception {
        Topology t = newTopology("testPublicServer");
        System.out.println("===== "+t.getName());
        
        // startEchoer();  // before getConfig() so it gets the port
        
        Properties config = getConfig();
        config.setProperty("ws.uri", "ws://echo.websocket.org");
        skipTestIfCantConnect(config);

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testSslPublicServer() throws Exception {
        Topology t = newTopology("testSslPublicServer");
        System.out.println("===== "+t.getName());
        
        // startEchoer();  // before getConfig() so it gets the port
        
        // Check operation against a trusted CA signed server certificate.
        //
        // this public wss echo server should "just work" if you have
        // connectivity.  no additional ssl trustStore config is needed
        // as the site has a certificate signed by a recognized CA.
        
        Properties config = getConfig();
        config.setProperty("ws.uri", "wss://echo.websocket.org");
        skipTestIfCantConnect(config);

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
        
        String[] expected = new String[] { getStr1(), getStr2() };
        
        TStream<String> s = t.strings(expected);
        s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
        wsClient.sendString(s);
        
        TStream<String> rcvd = wsClient.receiveString();
        
        completeAndValidate("", t, rcvd, SEC_TMO, expected);
    }
    
    @Test
    public void testSslPublicServerBadTrustStoreSystemPropertyNeg() throws Exception {
        Topology t = newTopology("testSslPublicServerBadTrustStoreSystemPropertyNeg");
        System.out.println("===== "+t.getName());
        
        // startEchoer();  // before getConfig() so it gets the port
        
        // this public wss echo server should "just work" if you have
        // connectivity.  no additional ssl trustStore config is needed
        // as the site has a certificate signed by a recognized CA.
        
        // Set a trust store that doesn't contain the public server's cert nor CAs
        // and ultimately the connect will fail and the test will
        // receive nothing.

        Properties config = getConfig();
        config.setProperty("ws.uri", "wss://echo.websocket.org");
        skipTestIfCantConnect(config);

        SslSystemPropMgr sslProps = new SslSystemPropMgr();
        try {
            sslProps.set();
    
            // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
            
            WebSocketClient wsClient = new Jsr356WebSocketClient(t, config);
            
            String[] expected = new String[] { getStr1(), getStr2() };
            
            TStream<String> s = t.strings(expected);
            s = PlumbingStreams.blockingOneShotDelay(s, 2, TimeUnit.SECONDS);
            wsClient.sendString(s);
            
            TStream<String> rcvd = wsClient.receiveString();
            
            completeAndValidate("", t, rcvd, SEC_TMO, new String[0]);  //rcv nothing
        }
        finally {
            sslProps.restore();
        }
    }
}
