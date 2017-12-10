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

package org.apache.edgent.connectors.kafka.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Supplier;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

/**
 * A connector for consuming Kafka key/value records.
 */
public class KafkaConsumerConnector extends KafkaConnector {
    private static final long serialVersionUID = 1L;
    private final Supplier<Map<String,Object>> configFn;
    private String id;
    // Map<subscriber,List<topic>>>
    private final Map<KafkaSubscriber<?>,List<String>> subscribers = new HashMap<>();
    private ConsumerConnector consumer;
    private final Map<KafkaSubscriber<?>,ExecutorService> executors = new HashMap<>();
    
    // Ugh. Turns out the new KafkaConsumer present in 8.2.2 isn't baked yet
    // (e.g., its poll() just return null).

    public KafkaConsumerConnector(Supplier<Map<String, Object>> configFn) {
        this.configFn = configFn;
    }
    
    // unbaked 8.2.2 KafkaConsumer
//    private synchronized KafkaConsumer<byte[],byte[]> client() {
//        if (consumer == null)
//            consumer = new KafkaConsumer<byte[],byte[]>(configFn.get(),
//                    null, /*ConsumerRebalanceCallaback*/
//                    new ByteArrayDeserializer(), new ByteArrayDeserializer());
//        return consumer;
//    }
    
    private synchronized ConsumerConnector client() {
        if (consumer == null)
            consumer = Consumer.createJavaConsumerConnector(
                                                createConsumerConfig());
        return consumer;
    }
    
    private ConsumerConfig createConsumerConfig() {
        Map<String,Object> config = configFn.get();
        Properties props = new Properties();
        for (Entry<String,Object> e : config.entrySet()) {
            props.put(e.getKey(), e.getValue().toString());
        }
        return new ConsumerConfig(props);
    }

    public synchronized void close(KafkaSubscriber<?> subscriber) {
        trace.trace("{} closing subscriber {}", id(), subscriber);
        // TODO hmm... really want to do consumer.shutdown() first
        // to avoid InterruptedException from shutdown[Now] of
        // consumer threads (in it.next()).
        // Our issue is that we can have multiple Subscriber for a
        // single ConsumerConnection.
        // Look at streams.messaging to see how it handles this - not
        // sure it does (e.g., may have only a single operator for a
        // ConsumerConnection).
        try {
            ExecutorService executor = executors.remove(subscriber);
            if (executor != null) {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            if (executors.isEmpty()) {
                trace.info("{} closing consumer", id());
                if (consumer != null)
                    consumer.shutdown();
            }
        }
    }
    
    synchronized void addSubscriber(KafkaSubscriber<?> subscriber, String... topics) {
        List<String> topicList = new ArrayList<>(Arrays.asList(topics));
        checkSubscription(subscriber, topicList);
        {
            // In Kafka 0.8.2.2, ConsumerConnector.createMessageStreams() can
            // only be called once for a connector instance.
            // The analogous operation in Kafka 0.9.0.0 doesn't have such a
            // restriction so for now just enforce a restriction rather than
            // do the work to make this appear to work.
            if (!subscribers.isEmpty())
                throw new IllegalStateException("The KafkaConsumer connection already has a subscriber");
        }
        subscribers.put(subscriber, topicList);
    }
    
   // unbaked 8.2.2 KafkaConsumer
//   synchronized void addSubscriber(KafkaSubscriber<?> subscriber, TopicPartition... topicPartitions) {
//        checkSubscription(subscriber, (Object[]) topicPartitions);
//        isTopicSubscriptions = false;
//        for (TopicPartition topicPartition : topicPartitions) {
//            trace.info("{} addSubscriber for {}", id(), topicPartition);
//            subscribers.put(subscriber, topicPartition);
//        }
//    }
    
    private void checkSubscription(KafkaSubscriber<?> subscriber, List<String> topics) {
        if (topics.size() == 0)
            throw new IllegalArgumentException("Subscription specification is empty");
        
        // disallow dup subscriptions
        Set<String> topicSet = new HashSet<>(topics);
        if (topicSet.size() != topics.size())
            throw new IllegalArgumentException("Duplicate subscription");
        
        // check against existing subscriptions
        topicSet.clear();
        for (List<String> l : subscribers.values())
            topicSet.addAll(l);
        for (String topic : topics) {
            if (topicSet.contains(topic))
                throw new IllegalArgumentException("Duplicate subscription");
        }
    }
    
    synchronized void start(KafkaSubscriber<?> subscriber) {
        trace.info("{} starting consumer", id());
        Map<String,Integer> topicCountMap = new HashMap<>();
        int threadsPerTopic = 1;
        int totThreadCnt = 0;
        List<String> topics = subscribers.get(subscriber);
        for (String topic : topics) {
            topicCountMap.put(topic,  threadsPerTopic);
            totThreadCnt += threadsPerTopic;
        }
        
        Map<String, List<KafkaStream<byte[],byte[]>>> consumerMap =
                client().createMessageStreams(topicCountMap);
        
        ExecutorService executor = Executors.newFixedThreadPool(totThreadCnt);
        executors.put(subscriber, executor);
        
        for (Entry<String,List<KafkaStream<byte[],byte[]>>> entry : consumerMap.entrySet()) {
            String topic = entry.getKey();
            int threadNum = 0;
            for (KafkaStream<byte[],byte[]> stream : entry.getValue()) {
                final int fThreadNum = threadNum++; 
                executor.submit(() -> {
                    try {
                        trace.info("{} started consumer thread {} for topic:{}", id(), fThreadNum, topic);
                        ConsumerIterator<byte[],byte[]> it = stream.iterator();
                        while (it.hasNext()) {
                            subscriber.accept(it.next());
                        }
                    }
                    catch (Throwable t) {
                        if (t instanceof InterruptedException) {
                            // normal close() termination
                            trace.trace("{} consumer for topic:{}. got exception", id(), topic, t);
                        }
                        else
                            trace.error("{} consumer for topic:{}. got exception", id(), topic, t);
                    }
                    finally {
                        trace.info("{} consumer thread {} for topic:{} exiting.", id(), fThreadNum, topic);
                    }
                });
            }
        }
    }

    // unbaked 8.2.2 KafkaConsumer
//    synchronized void start(KafkaSubscriber<?> subscriber) {
//        List<Object> subscriptions = subscribers.get(subscriber);
//        trace.info("{} adding subscription for {}", id(), subscriptions);
//        if (subscriptions.get(0) instanceof String)
//            client().subscribe(subscriptions.toArray(new String[0]));
//        else
//            client().subscribe(subscriptions.toArray(new TopicPartition[0]));
//        
//        if (pollFuture == null) {
//            pollFuture = executor.submit(new Runnable() {
//                @Override
//                public void run() {
//                    KafkaConsumerConnector.this.run();
//                }
//            });
//        }
//    }
//    
//    private void run() {
//        trace.info("{} poll thread running", id());
//        while (true) {
//            if (Thread.interrupted()) {
//                trace.info("{} poll thread terinating", id());
//                return;
//            }
//            
//            fetchAndProcess();
//        }
//    }
//    
//    private void fetchAndProcess() {
//        Map<String, ConsumerRecords<byte[],byte[]>> map = client().poll(2*1000);
//        
//        for (Entry<String,ConsumerRecords<byte[],byte[]>> e : map.entrySet()) {
//            KafkaSubscriber<?> subscriber = subscribers.get(e.getKey());
//            if (subscriber != null) {
//                for (ConsumerRecord<byte[],byte[]> rec : e.getValue().records()) {
//                    trace.info/*trace*/("{} processing record for {}", id(), rec.topicAndPartition());
//                    subscriber.accept(rec);
//                }
//            }
//            else {
//                // must be TopicPartition based subscription
//                for (ConsumerRecord<byte[],byte[]> rec : e.getValue().records()) {
//                    subscriber = subscribers.get(rec.topicAndPartition());
//                    trace.info/*trace*/("{} processing record for {}", id(), rec.topicAndPartition());
//                    subscriber.accept(rec);
//                }
//            }
//        }
//    }
    
    String id() {
        if (id == null) {
            // include our short object Id
            id = "Kafka " + toString().substring(toString().indexOf('@') + 1);
        }
        return id;
    }
}
