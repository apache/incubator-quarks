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

package org.apache.edgent.test.connectors.rabbitmq;

import org.apache.edgent.connectors.rabbitmq.RabbitmqConfigKeyConstants;
import org.apache.edgent.connectors.rabbitmq.RabbitmqConsumer;
import org.apache.edgent.connectors.rabbitmq.RabbitmqProducer;
import org.apache.edgent.test.connectors.common.ConnectorTestBase;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * A {@link RabbitmqConsumer} manual test case.
 * Please follow there steps
 *
 * step 1 :
 * Install RabbitMQ server.
 * For Mac os x, you can use homebrew to install it, the simple command is : `brew install rabbitmq`
 * For other system, please follow RabbitMQ's offical document :
 *  <a href="http://www.rabbitmq.com/download.html">http://www.rabbitmq.com/download.html</a>
 *
 * step 2 :
 * Start the RabbitMQ server.
 * For Mac os x, if you install it with homebrew, you can start it with : `brew services start rabbitmq`
 * For other system, please follow RabbitMQ's offical document :
 *  <a href="http://www.rabbitmq.com/download.html">http://www.rabbitmq.com/download.html</a>
 * Note : the default port which RabbitMQ server listen is 5672
 *        and it will start a web console which listen port 15672
 *        so you can access it with your web browser, just enter : http://localhost:15672/
 *        you will see a authorization page, the default user name and password are both `guest`
 *
 * step 3 :
 * Create a test queue use the web console.
 * after starting the server, you can access `http://localhost:15672/#/queues`
 * then you will find a button named `Add a new queue`, click it and input `testQueue` into `name` field
 * and click `Add queue` button to create the queue.
 *
 * step 4 :
 * run this test case to test the publish and subscribe method.
 */
public class RabbitmqStreamsTestManual extends ConnectorTestBase {

    private static final int PUB_DELAY_MSEC = 15*1000; // have seen 12sec 1st test's consumer startup delay
    private static final int SEC_TIMEOUT = 20;

    private final String msg1 = "Hello";
    private final String msg2 = "Are you there?";

    public String getMsg1() {
        return msg1;
    }

    public String getMsg2() {
        return msg2;
    }

    private Map<String, Object> initRabbitmqConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_HOST, "127.0.0.1");
        config.put(RabbitmqConfigKeyConstants.RABBITMQ_CONFIG_KEY_PORT, 5672);

        return config;
    }

    @Test
    public void testSimple() throws Exception {
        Topology t = newTopology("testSimple");
        Map<String, Object> configMap = initRabbitmqConfig();
        MsgGenerator generator = new MsgGenerator(t.getName());
        String queue = "testQueue";

        List<String> msgs = createMsgs(generator, queue, getMsg1(), getMsg2());

        TStream<String> stream = PlumbingStreams.blockingOneShotDelay(
            t.collection(msgs), PUB_DELAY_MSEC, TimeUnit.MILLISECONDS);

        RabbitmqConsumer consumer = new RabbitmqConsumer(t, () -> configMap);

        TStream<String> receivedStream = consumer.subscribe((byte[] bytes) -> new String(bytes), queue);

        RabbitmqProducer producer = new RabbitmqProducer(t, () -> configMap);

        TSink<String> sink = producer.publish(stream, queue, (String s) -> s.getBytes());

        completeAndValidate("", t, receivedStream, generator, SEC_TIMEOUT, msgs.toArray(new String[0]));

        assertNotNull(sink);
    }


}
