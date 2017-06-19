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
package org.apache.edgent.test.connectors.mqtt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.edgent.connectors.mqtt.MqttConfig;
import org.apache.edgent.connectors.mqtt.MqttStreams;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.test.connectors.common.ConnectorTestBase;
import org.apache.edgent.test.connectors.common.TestRepoPath;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.apache.edgent.topology.tester.Condition;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * MqttStreams tests
 * <p>
 * The MQTT server is expected to be configured as follows:
 * <ul>
 * <li>serverURL "tcp://localhost:1883"</li>
 * <li>if the server is configured for authentication requiring
 * a username/pw, it is configured for userName="testUserName" and password="testUserNamePw"</li>
 * </ul>
 */
public class MqttStreamsTestManual extends ConnectorTestBase {

    private static final int SEC_TIMEOUT = 15;
    private static final int PUB_DELAY_MSEC = 2*1000;
    private final String BASE_CLIENT_ID = "mqttStreamsTestClientId";
    private static final String uniq = simpleTS();
    private static final String TEST_USERNAME = "testUserName";
    private static final String TEST_PASSWORD = "testUserNamePw";
    protected final Map<String,String> authInfo = new HashMap<>();
    private final String msg1 = "Hello";
    private final String msg2 = "Are you there?";
    
    public String getMsg1() {
        return msg1;
    }

    public String getMsg2() {
        return msg2;
    }

    @Before
    public void setupAuthInfo() {
        authInfo.clear();
        authInfo.put("userID", TEST_USERNAME);
        authInfo.put("password", TEST_PASSWORD);
    }
    
    protected void setSslAuthInfo(String sslAuthMode) {
        String trustStore = getKeystorePath("clientTrustStore.jks");
        authInfo.put("trustStore", trustStore);
        authInfo.put("trustStorePassword", "passw0rd");
        
        if (sslAuthMode.equals("sslClientAuth")) {
            String keyStore = getKeystorePath("clientKeyStore.jks");
            authInfo.put("keyStore", keyStore);
            authInfo.put("keyStorePassword", "passw0rd");
            // authInfo.put("keyPassword", value);
            // authInfo.put("keyCertificateAlias", value);
        }
    }
    
    protected String getKeystorePath(String storeLeaf) {
        return TestRepoPath.getPath("keystores/" + storeLeaf);
    }
    
    private MqttConfig newConfig(String serverURL, String clientId) {
        MqttConfig config = new MqttConfig(serverURL, clientId);
        if (authInfo.get("userID") != null)
            config.setUserName(authInfo.get("userID"));
        if (authInfo.get("password") != null)
            config.setPassword(authInfo.get("password").toCharArray());
        if (authInfo.get("trustStore") != null)
            config.setTrustStore(authInfo.get("trustStore"));
        if (authInfo.get("trustStorePassword") != null)
            config.setTrustStorePassword(authInfo.get("trustStorePassword").toCharArray());
        if (authInfo.get("keyStore") != null)
            config.setKeyStore(authInfo.get("keyStore"));
        if (authInfo.get("keyStorePassword") != null)
            config.setKeyStorePassword(authInfo.get("keyStorePassword").toCharArray());
//        if (authInfo.get("keyPassword") != null)
//            config.setKeyPassword(authInfo.get("keyPassword").toCharArray());
//        if (authInfo.get("keyCertificateAlias") != null)
//            config.setKeyCertificateAlias(authInfo.get("keyCertificateAlias"));
        return config;
    }
       
    private static <T> byte[] serialize(T obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    public static <T> T deserialize(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            @SuppressWarnings("unchecked")
            T obj = (T) ois.readObject();
            ois.close();
            return obj;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
   
    protected String newClientId(String name) {
        String clientId = BASE_CLIENT_ID + "_" + name + "_" + uniq.replaceAll(":", "");
        System.out.println("["+simpleTS()+"] "
                + "Using MQTT clientID " + clientId);
        return clientId;
    }
    
    protected String[] getMqttTopics() {
        String csvTopics = "testTopic1,testTopic2";
        String[] topics = csvTopics.split(",");
        return topics;
    }
    
    protected String getServerURI() {
        return "tcp://localhost:1883";
    }
    
    protected String getSslServerURI() {
        return "ssl://localhost:8883";
    }
    
    protected String getSslClientAuthServerURI() {
        return "ssl://localhost:8884";
    }

    @Test
    public void testStringPublish() throws Exception {
        Topology top = newTopology("testStringPublish");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        TSink<String> sink = mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
        
        assertNotNull(sink);
    }
    
    @Test
    public void testAutoClientId() throws Exception {
        Topology top = newTopology("testAutoClientId");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test with auto-generated clientId
        MqttConfig config = newConfig(getServerURI(), null/*clientId*/);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate("some-auto-clientId", top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testQoS1() throws Exception {
        Topology top = newTopology("testQoS1");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 1;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // TODO something to verify that we actually provide
        // the QoS semantics.
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }

    @Test
    public void testQoS2() throws Exception {
        Topology top = newTopology("testQoS2");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 2;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // TODO something to verify that we actually provide
        // the QoS semantics.
        // Also improve code coverage with persistence override
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        config.setPersistence(new MemoryPersistence());
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testGenericPublish() throws Exception {
        Topology top = newTopology("testGenericPublish");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        // avoid mucking up other test subscribers expecting UTF-8 string payloads
        // and use a different topic
        String topic = getMqttTopics()[0] + "-Generic";
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<String> expMsgsAsStr = 
                msgs
                .stream()
                .map(t -> (new Msg(t, topic)).toString())
                .collect(Collectors.toList());

        TStream<Msg> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS)
                .map(t -> new Msg(t, topic));
        
        // Test publish(TStream<Msg>, topicFn, payloadFn, qosFn)
        // Test subscribe(topic, qos, message2Tuple)
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        TSink<Msg> sink = mqtt.publish(s, 
                                t -> t.getTopic(),
                                t -> serialize(t),
                                t -> qos,
                                t -> retain);
        TStream<Msg> rcvd = mqtt.subscribe(topic, qos,
                                (topicArg, payload) -> deserialize(payload));

        TStream<String> rcvdAsStr = rcvd.map(t -> t.toString());
        completeAndValidate(clientId, top, rcvdAsStr, mgen, SEC_TIMEOUT, expMsgsAsStr.toArray(new String[0]));
        
        assertNotNull(sink);
    }
    
    @Test
    public void testMultiConnector() throws Exception {
        Topology top = newTopology("testMultiConnector");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String pubClientId = newClientId(top.getName())+"_pub";
        String subClientId = newClientId(top.getName())+"_sub";
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test separate connectors for pub and sub
        
        MqttConfig config = newConfig(getServerURI(), pubClientId);
        MqttStreams mqttPub = new MqttStreams(top, () -> config);
        mqttPub.publish(s, topic, qos, retain);
        
        MqttConfig configSub = newConfig(getServerURI(), subClientId);
        MqttStreams mqttSub = new MqttStreams(top, () -> configSub);
        TStream<String> rcvd = mqttSub.subscribe(topic, qos);

        completeAndValidate(pubClientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testMultiTopicPublish() throws Exception {
        Topology top = newTopology("testMultiTopicPublish");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic1 = getMqttTopics()[0];
        String topic2 = getMqttTopics()[1];
        List<String> msgs1 = createMsgs(mgen, topic1, getMsg1(), getMsg2());
        List<String> msgs2 = createMsgs(mgen, topic2, getMsg1(), getMsg2());
        // create an interleaved list
        List<Msg> msgs = new ArrayList<>();
        for (int i = 0; i < msgs1.size(); i++) {
            msgs.add(new Msg(msgs1.get(i), topic1));
            msgs.add(new Msg(msgs2.get(i), topic2));
        }

        TStream<Msg> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test multi-topic publish(TStream<Msg>, topicFn, payloadFn, qosFn)
        //
        // Work around current limitation that we don't support 
        // multiple subscribe() on a single connection (the last subscribe()
        // receives all msgs and the other subscribes() get none).
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);
        MqttConfig config2 = newConfig(getServerURI(), clientId+"_sub2");
        MqttStreams mqttSub2 = new MqttStreams(top, () -> config2);

        mqtt.publish(s, 
                t -> t.getTopic(),
                t -> t.getMessage().getBytes(StandardCharsets.UTF_8),
                t -> qos,
                t -> retain);
        TStream<String> rcvd1 = mqtt.subscribe(topic1, qos);
        TStream<String> rcvd2 = mqttSub2.subscribe(topic2, qos);
        
        // validation
        
        rcvd1 = rcvd1.filter(tuple -> tuple.matches(mgen.pattern()));
        rcvd1.sink(t -> System.out.println("rcvd1: "+t));
        rcvd2 = rcvd2.filter(tuple -> tuple.matches(mgen.pattern()));
        rcvd2.sink(t -> System.out.println("rcvd2: "+t));
        
        Condition<Long> tc1 = top.getTester().tupleCount(rcvd1, msgs1.size());
        Condition<Long> tc2 = top.getTester().tupleCount(rcvd2, msgs2.size());
        
        List<Condition<Long>> conditions = new ArrayList<>();
        conditions.add(tc1);
        conditions.add(tc2);
        Condition<?> tc = top.getTester().and(tc1, tc2);

        Condition<List<String>> contents1 = top.getTester().streamContents(rcvd1, msgs1.toArray(new String[0]));
        Condition<List<String>> contents2 = top.getTester().streamContents(rcvd2, msgs2.toArray(new String[0]));

        complete(top, tc, SEC_TIMEOUT, TimeUnit.SECONDS);

        assertTrue(clientId + " contents1:" + contents1.getResult(), contents1.valid());
        assertTrue(clientId + " contents2:" + contents2.getResult(), contents2.valid());
        assertTrue("valid:" + tc, tc.valid());
    }
    
    @Test
    public void testMultiTopicSubscribe() throws Exception {
        Topology top = newTopology("testMultiTopicSubscribe");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic1 = getMqttTopics()[0] + "/1";
        String topic2 = getMqttTopics()[0] + "/2";
        String topics = getMqttTopics()[0] + "/+";
        List<String> msgs1 = createMsgs(mgen, topic1, getMsg1(), getMsg2());
        List<String> msgs2 = createMsgs(mgen, topic2, getMsg1(), getMsg2());
        List<String> msgs = new ArrayList<>();
        msgs.addAll(msgs1);
        msgs.addAll(msgs2);

        TStream<String> s1 = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs1), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        TStream<String> s2 = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs2), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test multi-topic subscribe
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s1, topic1, qos, retain);
        mqtt.publish(s2, topic2, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topics, qos);
        
        completeAndValidate(false/*ordered*/, clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }

    @Test(expected = IllegalStateException.class)
    public void testMultiSubscribeNeg() throws Exception {
        Topology top = newTopology("testMultiSubscribeNeg");
        int qos = 0;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];

        // Verify the current behavior of at-most-one subscribe()
        // for a MqttStreams instance
        
        MqttStreams mqtt = new MqttStreams(top, getServerURI(), clientId);

        mqtt.subscribe(topic, qos);
        mqtt.subscribe(topic, qos); // should throw
    }
    
    @Test
    public void testConnectFail() throws Exception {
        Topology top = newTopology("testConnectFail");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Code coverage test: induce connection failure
        //
        // At this point the only thing we can check is an expected
        // result of 0 msgs received.
        
        MqttConfig config = newConfig("tcp://localhost:31999", clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, new String[0]);
    }
    
    private String retainTestSetup(boolean isRetained, MsgGenerator mgen) throws Exception {

        // publish a msg to [not] retain.
        
        Topology top = newTopology("retainTestSetup"+isRetained);
        int qos = 0;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        String retainedMsg = mgen.create(topic, isRetained ? "RETAIN-THIS" : "DO-NOT-RETAIN-THIS");

        MqttConfig config = newConfig(getServerURI(), clientId+"-setup");
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.strings(retainedMsg), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        mqtt.publish(s, topic, qos, isRetained);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, retainedMsg);
        
        // N.B. the topology should be shutdown at this point but it isn't
        // and this topology will also receive and print the tuples
        // generated by the main portion of the test.
        // issue#46
        
        return retainedMsg;
    }

    @Test
    public void testRetainedFalse() throws Exception {
        Topology top = newTopology("testRetainedFalse");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean isRetained = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        
        // Test that "retain" control works
        
        // publish the msg to [not] retain. use the same mgen but a different clientId
        String retainedMsg = retainTestSetup(isRetained, mgen);
        
        System.out.println("=============== setup complete");
        
        // verify the next connect/subscribe [doesn't] sees the retain and then new msgs
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<String> expMsgsAsStr = 
                msgs
                .stream()
                .map(t -> (new Msg(t, topic)).toString())
                .collect(Collectors.toList());
        if (isRetained)
            expMsgsAsStr.add(0, (new Msg(retainedMsg, topic)).toString());

        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, false/*retain*/); 
        TStream<Msg> rcvd = mqtt.subscribe(topic, qos,
                                (topicArg, payload) -> {
                                    String m = new String(payload, StandardCharsets.UTF_8);
                                    return new Msg(m, topicArg);
                                });

        TStream<String> rcvdAsStr = rcvd.map(t -> t.toString());
        completeAndValidate(clientId, top, rcvdAsStr, mgen, SEC_TIMEOUT, expMsgsAsStr.toArray(new String[0]));
    }

    @Test
    public void testRetainedTrue() throws Exception {
        Topology top = newTopology("testRetainedTrue");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean isRetained = true;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        // Test that "retain" control works
        
        // publish the msg to [not] retain. use the same mgen but a different clientId
        String retainedMsg = retainTestSetup(isRetained, mgen);
        
        System.out.println("=============== setup complete");
        
        // verify the next connect/subscribe [doesn't] sees the retain and then new msgs
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<String> expMsgsAsStr = 
                msgs
                .stream()
                .map(t -> (new Msg(t, topic)).toString())
                .collect(Collectors.toList());
        if (isRetained)
            expMsgsAsStr.add(0, (new Msg(retainedMsg, topic)).toString());

        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, false/*retain*/); 
        TStream<Msg> rcvd = mqtt.subscribe(topic, qos,
                                (topicArg, payload) -> {
                                    String m = new String(payload, StandardCharsets.UTF_8);
                                    return new Msg(m, topicArg);
                                });

        TStream<String> rcvdAsStr = rcvd.map(t -> t.toString());
        completeAndValidate(clientId, top, rcvdAsStr, mgen, SEC_TIMEOUT, expMsgsAsStr.toArray(new String[0]));
    }
    
    @Test
    public void testConfig() throws Exception {
        Topology top = newTopology("testConfig");
        String clientId = newClientId(top.getName());
        
        // Test MqttConfig ctors/setters/getters

        MqttConfig config = new MqttConfig();
        assertEquals(null, config.getClientId());
        assertArrayEquals(null, config.getServerURLs());

        config = new MqttConfig(getServerURI(), clientId);
        
        {
            assertEquals(-1, config.getActionTimeToWaitMillis());
            config.setActionTimeToWaitMillis(31);
            assertEquals(31, config.getActionTimeToWaitMillis());
        }
        {
            assertEquals(clientId, config.getClientId());
            config.setClientId("xyzzyClient");
            assertEquals("xyzzyClient", config.getClientId());
        }
        {
            assertTrue(config.isCleanSession());
            config.setCleanSession(true);
            assertTrue(config.isCleanSession());
            config.setCleanSession(false);
            assertFalse(config.isCleanSession());
        }
        {
            assertEquals(30, config.getConnectionTimeout());
            config.setConnectionTimeout(11);
            assertEquals(11, config.getConnectionTimeout());
        }
        {
            assertEquals(60, config.getKeepAliveInterval());
            config.setKeepAliveInterval(12);
            assertEquals(12, config.getKeepAliveInterval());
        }
        {
            assertEquals(null, config.getKeyStore());
            config.setKeyStore("some/path");
            assertEquals("some/path", config.getKeyStore());
        }
        {
            assertArrayEquals(null, config.getKeyStorePassword());
            config.setKeyStorePassword("xyzzy".toCharArray());
            assertArrayEquals("xyzzy".toCharArray(), config.getKeyStorePassword());
        }
        {
            assertEquals(0, config.getIdleTimeout());
            config.setIdleTimeout(13);
            assertEquals(13, config.getIdleTimeout());
            config.setIdleTimeout(-1);
            assertEquals(0, config.getIdleTimeout());
        }
        {
            assertEquals(60, config.getSubscriberIdleReconnectInterval());
            config.setSubscriberIdleReconnectInterval(14);
            assertEquals(14, config.getSubscriberIdleReconnectInterval());
            config.setSubscriberIdleReconnectInterval(-1);
            assertEquals(0, config.getSubscriberIdleReconnectInterval());
        }
        {
            assertArrayEquals(null, config.getPassword());
            config.setPassword("xyzzy".toCharArray());
            assertArrayEquals("xyzzy".toCharArray(), config.getPassword());
        }
        {
            assertEquals(null, config.getPersistence());
            MqttClientPersistence p = new MqttDefaultFilePersistence();
            config.setPersistence(p);
            assertEquals(p, config.getPersistence());
            p = new MemoryPersistence();
            config.setPersistence(p);
            assertEquals(p, config.getPersistence());
        }
        {
            assertEquals(null, config.getTrustStore());
            config.setTrustStore("some/path");
            assertEquals("some/path", config.getTrustStore());
        }
        {
            assertArrayEquals(null, config.getTrustStorePassword());
            config.setTrustStorePassword("xyzzy".toCharArray());
            assertArrayEquals("xyzzy".toCharArray(), config.getTrustStorePassword());
        }
        {
            assertArrayEquals(new String[]{getServerURI()}, config.getServerURLs());
            String[] urls = new String[]{"tcp://localhost:30000","tcp://localhost:30001"};
            config.setServerURLs(urls);
            assertArrayEquals(urls, config.getServerURLs());
        }
        {
            assertEquals(null, config.getUserName());
            config.setUserName("joe");
            assertEquals("joe", config.getUserName());
        }
        {
            assertEquals(null, config.getWillDestination());
            assertArrayEquals(null, config.getWillPayload());
            assertEquals(0, config.getWillQOS());
            assertFalse(config.getWillRetained());
            
            config.setWill("willTopic", "I-AM-DEAD".getBytes(), 1, true);
            assertEquals("willTopic", config.getWillDestination());
            assertArrayEquals("I-AM-DEAD".getBytes(), config.getWillPayload());
            assertEquals(1, config.getWillQOS());
            assertTrue(config.getWillRetained());
        }
    }
    
    private static class PropertyTester {
        private final Properties props;
        private List<Checker> checkers = new ArrayList<>();

        private static class Checker {
            String name;
            String value;
            Supplier<String> getterFn;
            public Checker(String name, String value, Supplier<String> getterFn) {
                this.name = name;
                this.value = value;
                this.getterFn = getterFn;
            }
        }
        
        public PropertyTester(Properties props) {
            this.props = props;
        }
        
        public void add(String name, String value, Supplier<String> getterFn) {
            props.setProperty(name, value);
            checkers.add(new Checker(name, value, getterFn));
        }
        
        public void checkAll() throws Exception {
            for (Checker checker : checkers) {
                String val = checker.getterFn.get();
                //System.out.println("checking name="+checker.name+" exp="+checker.value+" act="+val);
                assertEquals(checker.name, checker.value, val);
            }
        }
    }
    
    @Test
    public void testConfigFromProperties() throws Exception {
        
        Properties props = new Properties();
        AtomicReference<MqttConfig> configRef = new AtomicReference<>();
        
        PropertyTester propTester = new PropertyTester(props);
        
        propTester.add("mqtt.actionTimeToWaitMillis", "10", 
                () -> ((Long)configRef.get().getActionTimeToWaitMillis()).toString());
        propTester.add("mqtt.cleanSession", "false",
                () -> ((Boolean)configRef.get().isCleanSession()).toString());
        propTester.add("mqtt.clientId", "xyzzy-clientId",
                () -> configRef.get().getClientId());
        propTester.add("mqtt.connectionTimeoutSec", "11", 
                () -> ((Integer)configRef.get().getConnectionTimeout()).toString());
        propTester.add("mqtt.idleTimeoutSec", "12", 
                () -> ((Integer)configRef.get().getIdleTimeout()).toString());
        propTester.add("mqtt.keepAliveSec", "13", 
                () -> ((Integer)configRef.get().getKeepAliveInterval()).toString());
        propTester.add("mqtt.keyStore", "some/path/keystore",
                () -> configRef.get().getKeyStore());
        propTester.add("mqtt.keyStorePassword", "some-keystore-password",
                () -> new String(configRef.get().getKeyStorePassword()));
//        propTester.add("mqtt.keyPassword", "some-key-password",
//                () -> new String(configRef.get().getKeyPassword()));
//        propTester.add("mqtt.keyCertificateAlias", "someKeyCertificateAlias",
//                () -> configRef.get().getKeyCertificateAlias());
        propTester.add("mqtt.password", "some-password",
                () -> new String(configRef.get().getPassword()));
//        propTester.add("mqtt.persistence", "some.persistence.classname",
//                () -> configRef.get().getPersistence());
        propTester.add("mqtt.serverURLs", "tcp://somehost:1234,ssl://somehost:5678",
                () -> String.join(",", configRef.get().getServerURLs()));
        propTester.add("mqtt.subscriberIdleReconnectIntervalSec", "14", 
                () -> ((Integer)configRef.get().getSubscriberIdleReconnectInterval()).toString());
        propTester.add("mqtt.trustStore", "some/path/truststore",
                () -> configRef.get().getTrustStore());
        propTester.add("mqtt.trustStorePassword", "some-truststore-password",
                () -> new String(configRef.get().getTrustStorePassword()));
        propTester.add("mqtt.userName", "xyzzy-username",
                () -> configRef.get().getUserName());
        
        JsonObject will = new JsonObject();
        will.addProperty("topic", "someWillTopic");
        will.addProperty("payload", "someWillPayload");
        will.addProperty("qos", 1);
        will.addProperty("retained", true);
        propTester.add("mqtt.will", will.toString(),
                () -> {
                        JsonObject actWill = new JsonObject();
                        actWill.addProperty("topic", configRef.get().getWillDestination());
                        actWill.addProperty("payload", new String(configRef.get().getWillPayload()));
                        actWill.addProperty("qos", configRef.get().getWillQOS());
                        actWill.addProperty("retained", configRef.get().getWillRetained());
                        return actWill.toString();
                      });

        configRef.set(MqttConfig.fromProperties(props));
        propTester.checkAll();
    }
    
    @Test
    public void testMultipleServerURL() throws Exception {
        Topology top = newTopology("testMultipleServerURL");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test server URL selection - supply a bogus 1st URL.
        
        String[] serverURLs = new String[] {"tcp://localhost:31999", getServerURI()};
        MqttConfig config = newConfig(serverURLs[0], clientId);
        config.setServerURLs(serverURLs);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testActionTime() throws Exception {
        Topology top = newTopology("testActionTime");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        config.setActionTimeToWaitMillis(3*1000);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testIdleSubscribe() throws Exception {
        Topology top = newTopology("testIdleSubscribe");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String pubClientId = newClientId(top.getName()+"_pub");
        String subClientId = newClientId(top.getName()+"_sub");
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());

        // Exercise idle timeouts.  We won't have any direct
        // evidence that an idle disconnect/reconnect happen
        // but the code coverage will verify the paths were
        // exercised
        
        // Hmm... this test may be prone to intermittent failure:
        // if the subscriber isn't connected when the tuples
        // are published they won't be received.  May have to
        // change to qos=1 and cleanSession=false.
        
        // delay enough to let idle disconnect/reconnect trigger
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), 7, TimeUnit.SECONDS);
        
        MqttConfig pubConfig = newConfig(getServerURI(), pubClientId);
        MqttStreams pubMqtt = new MqttStreams(top, () -> pubConfig);

        MqttConfig subConfig = newConfig(getServerURI(), subClientId);
        subConfig.setIdleTimeout(1);
        subConfig.setSubscriberIdleReconnectInterval(1);
        MqttStreams subMqtt = new MqttStreams(top, () -> subConfig);

        pubMqtt.publish(s, topic, qos, retain);

        TStream<String> rcvd = subMqtt.subscribe(topic, qos);

        completeAndValidate(subClientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testIdlePublish() throws Exception {
        Topology top = newTopology("testIdlePublish");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String pubClientId = newClientId(top.getName()+"_pub");
        String subClientId = newClientId(top.getName()+"_sub");
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());

        // Exercise idle timeouts.  We won't have any direct
        // evidence that an idle disconnect/reconnect happen
        // but the code coverage will verify the paths were
        // exercised
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        s = PlumbingStreams.blockingThrottle(
                top.collection(msgs), 4, TimeUnit.SECONDS);

        MqttConfig pubConfig = newConfig(getServerURI(), pubClientId);
        pubConfig.setIdleTimeout(2);
        MqttStreams pubMqtt = new MqttStreams(top, () -> pubConfig);

        MqttConfig subConfig = newConfig(getServerURI(), subClientId);
        MqttStreams subMqtt = new MqttStreams(top, () -> subConfig);

        pubMqtt.publish(s, topic, qos, retain);

        TStream<String> rcvd = subMqtt.subscribe(topic, qos);

        completeAndValidate(subClientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testConnectRetryPub() throws Exception {
        Topology top = newTopology("testConnectRetryPub");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String pubClientId = newClientId(top.getName()+"_pub");
        String subClientId = newClientId(top.getName()+"_sub");
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());

        // Exercise connection retry by first specifying
        // a bogus server url then a good one.
        
        // Note qos==0 so its critical for the test that the subscriber
        // is connected before the pub connects and sends.
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        final String goodServerURI = getServerURI();
        MqttConfig pubConfig = newConfig("tcp://localhost:31999", pubClientId);
        MqttStreams pubMqtt = new MqttStreams(top, 
                new Supplier<MqttConfig>() {
                    private static final long serialVersionUID = 1L;
                    private int getCnt;

                    @Override
                    public MqttConfig get() {
                        ++getCnt;
                        System.err.println("**** getCnt:"+getCnt);
                        // delay enough to induce multiple retry w/o exceeding test harness timeout
                        if (getCnt == 4)
                            pubConfig.setServerURLs(new String[]{ goodServerURI });
                        return pubConfig;
                    }
                });

        MqttConfig subConfig = newConfig(getServerURI(), subClientId);
        MqttStreams subMqtt = new MqttStreams(top, () -> subConfig);

        pubMqtt.publish(s, topic, qos, retain);

        TStream<String> rcvd = subMqtt.subscribe(topic, qos);

        // add extra TMO delay. getting intermittent TMO failures
        completeAndValidate(subClientId, top, rcvd, mgen, SEC_TIMEOUT + 5, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testConnectRetrySub() throws Exception {
        // Timing variances on shared machines can cause this test to fail
        assumeTrue(!Boolean.getBoolean("edgent.build.ci"));

        Topology top = newTopology("testConnectRetrySub");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String pubClientId = newClientId(top.getName()+"_pub");
        String subClientId = newClientId(top.getName()+"_sub");
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());

        // Exercise connection retry by first specifying
        // a bogus server url then a good one.

        // Note qos==0 so its critical for the test that the subscriber
        // is connected before the pub connects and sends.
        // Even if we pub @ qos>0, since the broker doesn't know of
        // the sub's new clientId, the msgs will get tossed.
        // (and if it did know of the clientId we'd also have to
        // subConfig.setCleanSession(false) to avoid the msgs getting
        // tossed upon connect).
        //
        // The 15s pub delay below *should* cover things.  We've seen
        // intermittent 11s sub connect delays.  Still, would be
        // better to have a more robust scheme/interlock here.
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), 15, TimeUnit.SECONDS);
        
        final String goodServerURI = getServerURI();

        MqttConfig pubConfig = newConfig(getServerURI(), pubClientId);
        MqttStreams pubMqtt = new MqttStreams(top, () -> pubConfig);
        
        MqttConfig subConfig = newConfig("tcp://localhost:31999", subClientId);
        MqttStreams subMqtt = new MqttStreams(top, 
                new Supplier<MqttConfig>() {
                    private static final long serialVersionUID = 1L;
                    private int getCnt;

                    @Override
                    public MqttConfig get() {
                        ++getCnt;
                        System.err.println("**** getCnt:"+getCnt);                            
                        // delay enough to induce retry w/o exceeding test harness timeout
                        // or missing published tuples
                       if (getCnt == 4)
                            subConfig.setServerURLs(new String[]{ goodServerURI });
                        return subConfig;
                    }
                });

        pubMqtt.publish(s, topic, qos, retain);

        TStream<String> rcvd = subMqtt.subscribe(topic, qos);

        // add extra TMO delay. getting intermittent TMO failures
        completeAndValidate(subClientId, top, rcvd, mgen, SEC_TIMEOUT + 5, msgs.toArray(new String[0]));
    }
    
    @Test
    public void testSubscribeFnThrow() throws Exception {
        Topology top = newTopology("testSubscribeFnThrow");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<String> expectMsgs = new ArrayList<>(msgs);
        expectMsgs.remove(0); // should only lose the 1st tuple
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // improve code coverage by having the subscribe fn throw
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos,
                new BiFunction<String, byte[], String>() {
                    private static final long serialVersionUID = 1L;
                    int tupCnt;

                    @Override
                    public String apply(String t, byte[] payload) {
                        String msg = new String(payload, StandardCharsets.UTF_8);
                        // caution "retained" msgs from prior tests
                        if (msg.matches(mgen.pattern())) {
                            ++tupCnt;
                            if (tupCnt == 1)
                                throw new RuntimeException("TEST message2tuple thrown exception");
                        }
                        return msg;
                    }
                });

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, expectMsgs.toArray(new String[0]));
    }
    
    @Test
    public void testPublishFnThrow() throws Exception {
        Topology top = newTopology("testPublishFnThrow");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<String> expectMsgs = new ArrayList<>(msgs);
        expectMsgs.remove(0); // should only lose the 1st tuple
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);

        // improve code coverage by having a publish tuple fn throw
        
        MqttConfig config = newConfig(getServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        mqtt.publish(s, 
                tuple -> topic,
                tuple -> tuple.getBytes(StandardCharsets.UTF_8),
                tuple -> qos,
                new Function<String,Boolean>() {
                    private static final long serialVersionUID = 1L;
                    int tupCnt;

                    @Override
                    public Boolean apply(String tuple) {
                        tupCnt++;
                        if (tupCnt == 1)
                            throw new RuntimeException("TEST RETAIN-FN THROWN");
                        return retain;
                    }
                });
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, expectMsgs.toArray(new String[0]));
    }
    
    /* 
     * See mqtt/src/test/keystores/README for info about SSL/TLS and mosquitto
     */
    
    @Test
    public void testSsl() throws Exception {
        Topology top = newTopology("testSsl");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)

        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        setSslAuthInfo("ssl");
        MqttConfig config = newConfig(getSslServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        TSink<String> sink = mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
        
        assertNotNull(sink);
    }
    
    @Test
    public void testSslClientAuth() throws Exception {
        Topology top = newTopology("testSslClientAuth");
        MsgGenerator mgen = new MsgGenerator(top.getName());
        int qos = 0;
        boolean retain = false;
        String clientId = newClientId(top.getName());
        String topic = getMqttTopics()[0];
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                top.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        // Test publish(TStream<String>, topic, qos)
        // Test TStream<String> subscribe(topic, qos)
        
        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list
        
        setSslAuthInfo("sslClientAuth");
        MqttConfig config = newConfig(getSslClientAuthServerURI(), clientId);
        MqttStreams mqtt = new MqttStreams(top, () -> config);

        TSink<String> sink = mqtt.publish(s, topic, qos, retain);
        TStream<String> rcvd = mqtt.subscribe(topic, qos);

        completeAndValidate(clientId, top, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
        
        assertNotNull(sink);
    }

}
