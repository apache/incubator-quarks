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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.kafka.KafkaConsumer;
import org.apache.edgent.connectors.kafka.KafkaProducer;
import org.apache.edgent.test.connectors.common.ConnectorTestBase;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.junit.Test;

/**
 * Test the Kafka connector.
 * <p>
 * The tests expect a Kafka/Zookeeper running on the local host at their
 * default ports: 9092 and 2181 respectively.
 * <p>
 * The following system properties may be used to override that:
 * <ul>
 *   <li>org.apache.edgent.test.connectors.kafka.bootstrap.servers=localhost:9092</li>
 *   <li>org.apache.edgent.test.connectors.kafka.zookeeper.connect=localhost:2181</li>
 * </ul>
 * <p>
 * Setting up a Kafka/Zookeeper config on the default localhost ports is simple
 * and well documented at https://kafka.apache.org/quickstart.  This should do it:
 * <p>
 * After downloading kafka:
 * <pre>{@code
 * tar zxf ~/Downloads/kafka_2.11-0.10.1.0.tgz
 * cd kafka_2.11-0.10.1.0/
 * 
 * # start the servers (best in separate windows)
 * bin/zookeeper-server-start.sh config/zookeeper.properties
 * bin/kafka-server-start.sh config/server.properties
 * }</pre>
 * 
 * <p>
 * Create the test's topics:
 * <pre>{@code
 * # create our kafka test and sample topics
 * bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic testTopic1
 * bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic testTopic2
 * bin/kafka-topics.sh --list --zookeeper localhost:2181
 * 
 * # quick verify
 * bin/kafka-console-producer.sh --broker-list localhost:9092 --topic testTopic1
 * hi
 * there
 * ^D
 * bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic testTopic1 --from-beginning
 * ... you should see the "hi" and "there" messages.
 * }</pre>
 */
public class KafkaStreamsTestManual extends ConnectorTestBase {
    private static final int PUB_DELAY_MSEC = 4*1000;
    private static final int SEC_TIMEOUT = 10;
    private final String BASE_GROUP_ID = "kafkaStreamsTestGroupId";
    private final String uniq = simpleTS();
    private final String msg1 = "Hello";
    private final String msg2 = "Are you there?";
    
    public String getMsg1() {
        return msg1;
    }

    public String getMsg2() {
        return msg2;
    }

    private String[] getKafkaTopics() {
        String csvTopics = System.getProperty("org.apache.edgent.test.connectors.kafka.csvTopics", "testTopic1,testTopic2");
        String[] topics = csvTopics.split(",");
        return topics;
    }
    
    private String getKafkaBootstrapServers() {
        return System.getProperty("org.apache.edgent.test.connectors.kafka.bootstrap.servers", "localhost:9092");
    }
    
    private String getKafkaZookeeperConnect() {
        return System.getProperty("org.apache.edgent.test.connectors.kafka.zookeeper.connect", "localhost:2181");
    }
    
    private String newGroupId(String name) {
        String groupId = BASE_GROUP_ID + "_" + name + "_" + uniq.replaceAll(":", "");
        System.out.println("["+simpleTS()+"] "
                + "Using Kafka consumer group.id " + groupId);
        return groupId;
    }

    private Map<String,Object> newConsumerConfig(String groupId) {
        Map<String,Object> config = new HashMap<>();
        // unbaked 8.8.2 KafkaConsumer
//        config.put("bootstrap.servers", getKafkaBootstrapServers());
        config.put("zookeeper.connect", getKafkaZookeeperConnect());
        config.put("group.id", groupId);
        return config;
    }
    
    private Map<String,Object> newProducerConfig() {
        Map<String,Object> config = new HashMap<>();
        config.put("bootstrap.servers", getKafkaBootstrapServers());
        return config;
    }
    
    private static class Rec {
        String topic;
        int partition;
        String key;
        String value;
        Rec(String topic, int partition, String key, String value) {
            this.topic = topic;
            this.key = key;
            this.value = value;
        }
        public String toString() {
            return "topic:"+topic+" partition:"+partition+" key:"+key+" value:"+value;
        }
    }

    @Test
    public void testSimple() throws Exception {
        Topology t = newTopology("testSimple");
        MsgGenerator mgen = new MsgGenerator(t.getName());
        String topic = getKafkaTopics()[0];
        String groupId = newGroupId(t.getName());
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        
        TStream<String> s = PlumbingStreams.blockingOneShotDelay(
                        t.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        Map<String,Object> pConfig = newProducerConfig();
        KafkaProducer producer = new KafkaProducer(t, () -> pConfig);
        
        TSink<String> sink = producer.publish(s, topic);
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        TStream<String> rcvd = consumer.subscribe(
                    rec -> rec.value(),
                    topic);

        completeAndValidate("", t, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
        
        assertNotNull(sink);
    }

    @Test
    public void testWithKey() throws Exception {
        Topology t = newTopology("testWithKey");
        MsgGenerator mgen = new MsgGenerator(t.getName());
        String topic = getKafkaTopics()[0];
        String groupId = newGroupId(t.getName());
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<Rec> recs = new ArrayList<>();
        int i = 0;
        for (String msg : msgs) {
            recs.add(new Rec(topic, 0, "key-" + ++i, msg));
        }
        List<String> expected = new ArrayList<>();
        for (Rec rec : recs) {
            expected.add(rec.toString());
        }
        
        // Test publish with key
        // Also exercise ConsumerRecord accessors
        
        TStream<Rec> s = PlumbingStreams.blockingOneShotDelay(
                t.collection(recs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        Map<String,Object> pConfig = newProducerConfig();
        KafkaProducer producer = new KafkaProducer(t, () -> pConfig);
        
        producer.publish(s,
                    tuple -> tuple.key,
                    tuple -> tuple.value,
                    tuple -> tuple.topic,
                    tuple -> tuple.partition);
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        TStream<String> rcvd = consumer.subscribe(
                    rec -> new Rec(rec.topic(),
                                rec.partition(),
                                rec.key(),
                                rec.value()).toString(),
                    topic);

        completeAndValidate("", t, rcvd, mgen, SEC_TIMEOUT, expected.toArray(new String[0]));
    }

    @Test
    public void testPubSubBytes() throws Exception {
        Topology t = newTopology("testPubSubBytes");
        MsgGenerator mgen = new MsgGenerator(t.getName());
        String topic = getKafkaTopics()[0];
        String groupId = newGroupId(t.getName());
        List<String> msgs = createMsgs(mgen, topic, getMsg1(), getMsg2());
        List<Rec> recs = new ArrayList<>();
        int i = 0;
        for (String msg : msgs) {
            recs.add(new Rec(topic, 0, "key-" + ++i, msg));
        }
        List<String> expected = new ArrayList<>();
        for (Rec rec : recs) {
            expected.add(rec.toString());
        }
        
        // Test publishBytes() / subscribeBytes()
        
        TStream<Rec> s = PlumbingStreams.blockingOneShotDelay(
                t.collection(recs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        Map<String,Object> pConfig = newProducerConfig();
        KafkaProducer producer = new KafkaProducer(t, () -> pConfig);
        
        producer.publishBytes(s,
                    tuple -> tuple.key.getBytes(StandardCharsets.UTF_8),
                    tuple -> tuple.value.getBytes(StandardCharsets.UTF_8),
                    tuple -> tuple.topic,
                    tuple -> tuple.partition);
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        TStream<String> rcvd = consumer.subscribeBytes(
                    rec -> new Rec(rec.topic(),
                                rec.partition(),
                                new String(rec.key(), StandardCharsets.UTF_8),
                                new String(rec.value(), StandardCharsets.UTF_8)).toString(),
                    topic);

        completeAndValidate("", t, rcvd, mgen, SEC_TIMEOUT, expected.toArray(new String[0]));
    }

    @Test
    public void testMultiPub() throws Exception {
        Topology t = newTopology("testMultiPub");
        MsgGenerator mgen = new MsgGenerator(t.getName());
        String topic1 = getKafkaTopics()[0];
        String topic2 = getKafkaTopics()[1];
        String groupId = newGroupId(t.getName());
        List<String> msgs1 = createMsgs(mgen, topic1, getMsg1(), getMsg2());
        List<String> msgs2 = createMsgs(mgen, topic2, getMsg1(), getMsg2());
        List<String> msgs = new ArrayList<>(msgs1);
        msgs.addAll(msgs2);
        
        // Multiple publish() on a single connection.
        // Also multi-topic subscribe().
        
        TStream<String> s1 = PlumbingStreams.blockingOneShotDelay(
                t.collection(msgs1), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        TStream<String> s2 = PlumbingStreams.blockingOneShotDelay(
                t.collection(msgs2), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        Map<String,Object> pConfig = newProducerConfig();
        KafkaProducer producer = new KafkaProducer(t, () -> pConfig);
        
        TSink<String> sink1 = producer.publish(s1, topic1);
        TSink<String> sink2 = producer.publish(s2, topic2);
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        TStream<String> rcvd = consumer.subscribe(
                    rec -> rec.value(),
                    topic1, topic2);

        completeAndValidate(false/*ordered*/, "", t, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
        
        assertNotNull(sink1);
        assertNotNull(sink2);
        assertNotSame(sink1, sink2);
    }

    @Test(expected=IllegalStateException.class)
    public void testMultiSubNeg() throws Exception {
        Topology t = newTopology("testMultiSubNeg");
        MsgGenerator mgen = new MsgGenerator(t.getName());
        String topic1 = getKafkaTopics()[0];
        String topic2 = getKafkaTopics()[1];
        String groupId = newGroupId(t.getName());
        List<String> msgs1 = createMsgs(mgen, topic1, getMsg1(), getMsg2());
        List<String> msgs2 = createMsgs(mgen, topic2, getMsg1(), getMsg2());
        
        // Multiple subscribe() on a single connection.
        // Currently, w/Kafka0.8.2.2, we only support a single
        // subscriber on the connection and an IllegalStateException
        // is thrown.
        // This restriction will be removed when we migrate to Kafka 0.9.0.0
        
        TStream<String> s1 = PlumbingStreams.blockingOneShotDelay(
                t.collection(msgs1), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        TStream<String> s2 = PlumbingStreams.blockingOneShotDelay(
                t.collection(msgs2), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);
        
        Map<String,Object> pConfig = newProducerConfig();
        KafkaProducer producer = new KafkaProducer(t, () -> pConfig);
        
        producer.publish(s1, topic1);
        producer.publish(s2, topic2);
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        @SuppressWarnings("unused")
        TStream<String> rcvd1 = consumer.subscribe(
                    rec -> rec.value(),
                    topic1);
        
        @SuppressWarnings("unused")
        TStream<String> rcvd2 = consumer.subscribe(
                    rec -> rec.value(),
                    topic2);
        
        // TODO see "single subscribe" restriction above
        
//        // TODO union() is NYI
////        TStream<String> rcvd = rcvd1.union(rcvd2);
////
////        completeAndValidate(false/*ordered*/, "", t, rcvd, mgen, SEC_TIMEOUT, msgs.toArray(new String[0]));
//        
//        Condition<Long> tc1 = t.getTester().tupleCount(rcvd1, msgs1.size());
//        Condition<Long> tc2 = t.getTester().tupleCount(rcvd2, msgs2.size());
//        
//        List<Condition<Long>> conditions = new ArrayList<>();
//        conditions.add(tc1);
//        conditions.add(tc2);
//        Condition<?> tc = tc1.and(tc2);
//
//        Condition<List<String>> contents1 = t.getTester().streamContents(rcvd1, msgs1.toArray(new String[0]));
//        Condition<List<String>> contents2 = t.getTester().streamContents(rcvd2, msgs2.toArray(new String[0]));
//
//        complete(t, tc, SEC_TIMEOUT, TimeUnit.SECONDS);
//
//        assertTrue(groupId + " contents1:" + contents1.getResult(), contents1.valid());
//        assertTrue(groupId + " contents2:" + contents2.getResult(), contents2.valid());
//        assertTrue("valid:" + tc, tc.valid());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoTopicSubNeg() throws Exception {
        Topology t = newTopology("testNoTopicSubNeg");
        String groupId = newGroupId(t.getName());
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        consumer.subscribe(rec -> rec.value()/*, "topic1"*/);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDupTopicSub1Neg() throws Exception {
        Topology t = newTopology("testDupTopicSub1Neg");
        String groupId = newGroupId(t.getName());
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        consumer.subscribe(rec -> rec.value(), "topic1", "topic1");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDupTopicSub2Neg() throws Exception {
        Topology t = newTopology("testDupTopicSub2Neg");
        String groupId = newGroupId(t.getName());
        
        Map<String,Object> cConfig = newConsumerConfig(groupId);
        KafkaConsumer consumer = new KafkaConsumer(t, () -> cConfig);
        
        consumer.subscribe(rec -> rec.value(), "topic1");
        consumer.subscribe(rec -> rec.value(), "topic1");
    }

}
