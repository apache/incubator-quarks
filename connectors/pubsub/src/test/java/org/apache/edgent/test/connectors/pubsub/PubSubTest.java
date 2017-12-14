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
package org.apache.edgent.test.connectors.pubsub;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.edgent.connectors.pubsub.PublishSubscribe;
import org.apache.edgent.connectors.pubsub.service.ProviderPubSub;
import org.apache.edgent.connectors.pubsub.service.PublishSubscribeService;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.Action;
import org.apache.edgent.execution.Job.State;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.apache.edgent.topology.tester.Tester;
import org.junit.Test;

public class PubSubTest {

    private final String[] strs = new String[] { "A", "B", "C" };

    public String[] getStrs() {
        return strs;
    }

    /**
     * Test without a pub-sub service so no
     * cross job connections will be made.
     * @throws Exception on failure
     */
    @Test
    public void testNoService() throws Exception {
        DirectProvider dp = new DirectProvider();

        TStream<String> publishedStream = createPublisher(dp, "t1", String.class, getStrs());
        Tester testPub = publishedStream.topology().getTester();
        Condition<Long> tcPub = testPub.tupleCount(publishedStream, 3);

        TStream<String> subscribedStream = createSubscriber(dp, "t1", String.class);
        Tester testSub = subscribedStream.topology().getTester();
        Condition<Long> tcSub = testSub.tupleCount(subscribedStream, 0); // Expect none

        Job js = dp.submit(subscribedStream.topology()).get();
        Job jp = dp.submit(publishedStream.topology()).get();

        Thread.sleep(1500);

        assertTrue(tcPub.valid());
        assertTrue(tcSub.valid());

        js.stateChange(Action.CLOSE);
        jp.stateChange(Action.CLOSE);
    }
    
    private <T> TStream<T> createPublisher(DirectProvider dp, String topic, Class<? super T> streamType, @SuppressWarnings("unchecked") T...values) {
        Topology publisher = dp.newTopology("Pub");
        TStream<T> stream = publisher.of(values);
        PublishSubscribe.publish(stream, topic, streamType);
        return stream;
    }
    
    private <T> TStream<T> createSubscriber(DirectProvider dp, String topic, Class<T> streamType) {
        Topology subscriber = dp.newTopology("Sub");
        return PublishSubscribe.subscribe(subscriber, topic, streamType);     
    }

    @Test(timeout=10000)
    public void testProviderServiceSingleSubscriber() throws Exception {
        DirectProvider dp = new DirectProvider();

        dp.getServices().addService(PublishSubscribeService.class, new ProviderPubSub());

        TStream<String> publishedStream = createPublisher(dp, "t1", String.class, getStrs());
        Tester testPub = publishedStream.topology().getTester();
        Condition<List<String>> tcPub = testPub.streamContents(publishedStream, getStrs());

        TStream<String> subscribedStream = createSubscriber(dp, "t1", String.class);
        Tester testSub = subscribedStream.topology().getTester();
        Condition<List<String>> tcSub = testSub.streamContents(subscribedStream, getStrs()); // Expect all tuples

        Job js = dp.submit(subscribedStream.topology()).get();
        // Give the subscriber a chance to setup.
        while (js.getCurrentState() != State.RUNNING)
            Thread.sleep(50);
        
        Job jp = dp.submit(publishedStream.topology()).get();
        
        while (!tcSub.valid() || !tcPub.valid())
            Thread.sleep(50);

        assertTrue(tcPub.valid());
        assertTrue(tcSub.valid());

        js.stateChange(Action.CLOSE);
        jp.stateChange(Action.CLOSE);
    }
    
    @Test(timeout=10000)
    public void testProviderServiceMultipleSubscriber() throws Exception {
        DirectProvider dp = new DirectProvider();

        dp.getServices().addService(PublishSubscribeService.class, new ProviderPubSub());

        TStream<String> publishedStream = createPublisher(dp, "t1", String.class, getStrs());
        Tester testPub = publishedStream.topology().getTester();
        Condition<List<String>> tcPub = testPub.streamContents(publishedStream, getStrs());
        
        TStream<String> subscribedStream1 = createSubscriber(dp, "t1", String.class);
        Tester testSub1 = subscribedStream1.topology().getTester();
        Condition<List<String>> tcSub1 = testSub1.streamContents(subscribedStream1, getStrs());
        
        TStream<String> subscribedStream2 = createSubscriber(dp, "t1", String.class);
        Tester testSub2 = subscribedStream2.topology().getTester();
        Condition<List<String>> tcSub2 = testSub2.streamContents(subscribedStream2, getStrs());
        
        TStream<String> subscribedStream3 = createSubscriber(dp, "t1", String.class);
        Tester testSub3 = subscribedStream3.topology().getTester();
        Condition<List<String>> tcSub3 = testSub3.streamContents(subscribedStream3, getStrs());

        
        Job js1 = dp.submit(subscribedStream1.topology()).get();
        Job js2 = dp.submit(subscribedStream2.topology()).get();
        Job js3 = dp.submit(subscribedStream3.topology()).get();
        
        // Give the subscribers a chance to setup.
        while (
                (js1.getCurrentState() != State.RUNNING) &&
                (js2.getCurrentState() != State.RUNNING) &&
                (js3.getCurrentState() != State.RUNNING))
            Thread.sleep(50);
                
        Job jp = dp.submit(publishedStream.topology()).get();
          
        while (!tcSub1.valid() || !tcSub2.valid() || !tcSub3.valid() || !tcPub.valid())
            Thread.sleep(50);

        assertTrue(tcPub.valid());
        assertTrue(tcSub1.valid());
        assertTrue(tcSub2.valid());
        assertTrue(tcSub3.valid());

        js1.stateChange(Action.CLOSE);
        js2.stateChange(Action.CLOSE);
        js3.stateChange(Action.CLOSE);
        jp.stateChange(Action.CLOSE);
    }
    
    @Test(timeout=10000)
    public void testProviderServiceMultiplePublisher() throws Exception {
        DirectProvider dp = new DirectProvider();

        dp.getServices().addService(PublishSubscribeService.class, new ProviderPubSub());

        TStream<Integer> publishedStream1 = createPublisher(dp, "i1", Integer.class, 1,2,3,82);
        Tester testPub1 = publishedStream1.topology().getTester();
        Condition<List<Integer>> tcPub1 = testPub1.streamContents(publishedStream1, 1,2,3,82);
        
        TStream<Integer> publishedStream2 = createPublisher(dp, "i1", Integer.class, 5,432,34,99);
        Tester testPub2 = publishedStream2.topology().getTester();
        Condition<List<Integer>> tcPub2 = testPub2.streamContents(publishedStream2, 5,432,34,99);
 
        TStream<Integer> publishedStream3 = createPublisher(dp, "i1", Integer.class, 35,456,888,263,578);
        Tester testPub3 = publishedStream3.topology().getTester();
        Condition<List<Integer>> tcPub3 = testPub3.streamContents(publishedStream3, 35,456,888,263,578);
 

        TStream<Integer> subscribedStream = createSubscriber(dp, "i1", Integer.class);
        Tester testSub = subscribedStream.topology().getTester();
        Condition<List<Integer>> tcSub = testSub.contentsUnordered(subscribedStream,
                1,2,3,82,5,432,34,99,35,456,888,263,578); // Expect all tuples

        Job js = dp.submit(subscribedStream.topology()).get();
        // Give the subscriber a chance to setup.
        while (js.getCurrentState() != State.RUNNING)
            Thread.sleep(50);
        
        Job jp1 = dp.submit(publishedStream1.topology()).get();
        Job jp2 = dp.submit(publishedStream2.topology()).get();
        Job jp3 = dp.submit(publishedStream3.topology()).get();
        
        while (!tcSub.valid() || !tcPub1.valid() || !tcPub2.valid()  || !tcPub3.valid())
            Thread.sleep(50);

        assertTrue(tcPub1.getResult().toString(), tcPub1.valid());
        assertTrue(tcPub2.getResult().toString(), tcPub2.valid());
        assertTrue(tcPub3.getResult().toString(), tcPub3.valid());
        assertTrue(tcSub.getResult().toString(), tcSub.valid());

        js.stateChange(Action.CLOSE);
        jp1.stateChange(Action.CLOSE);
        jp2.stateChange(Action.CLOSE);
        jp3.stateChange(Action.CLOSE);
    }
}
