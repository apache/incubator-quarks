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
package org.apache.edgent.window;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Supplier;

/**
 * Common window policies.
 *
 */
public class Policies {
    
    /**
     * A policy which schedules a future partition eviction if the partition is empty.
     * This can be used as a contents policy that is scheduling the eviction of
     * the tuple just about to be inserted.
     * @param <T> Tuple Type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @param time The time span in which tuple are permitted in the partition.
     * @param unit The units of time.
     * @return The time-based contents policy.
     */
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> scheduleEvictIfEmpty(long time, TimeUnit unit){
        return (partition, tuple) -> {          
            if(partition.getContents().isEmpty()){
                ScheduledExecutorService ses = partition.getWindow().getScheduledExecutorService();
                ses.schedule(() -> partition.evict(), time, unit);
            }
        };
    }
    
    /**
     * A policy which schedules a future partition eviction on the first insert.
     * This can be used as a contents policy that schedules the eviction of tuples
     * as a batch.
     * @param <T> Tuple Type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @param time The time span in which tuple are permitted in the partition.
     * @param unit The units of time.
     * @return The time-based contents policy.
     */
    @SuppressWarnings("serial")
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> scheduleEvictOnFirstInsert(long time, TimeUnit unit){
        
        // Can't use lambda since state is required
        return new BiConsumer<Partition<T,K,L>, T>() {
            private Set<Partition<T,K,L>> initialized_partitions = Collections.synchronizedSet(new HashSet<>());
            @Override
            public void accept(Partition<T, K, L> partition, T tuple) {
                if(!initialized_partitions.contains(partition)){
                    initialized_partitions.add(partition);
                    ScheduledExecutorService ses = partition.getWindow().getScheduledExecutorService();
                    ses.schedule(() -> partition.evict(), time, unit);
                }    
            }
        };
    }
    
    /**
     * An eviction policy which evicts all tuples that are older than a specified time.
     * If any tuples remain in the partition, it schedules their eviction after
     * an appropriate interval.
     * @param <T> Tuple Type
     * @param <K> Key type
     * @param time The timespan in which tuple are permitted in the partition.
     * @param unit The units of time.
     * @return The time-based eviction policy.
     */ 
    public static <T, K> Consumer<Partition<T, K, InsertionTimeList<T>> > evictOlderWithProcess(long time, TimeUnit unit){
        
        long timeMs = TimeUnit.MILLISECONDS.convert(time, unit);

        return (partition) -> {
            ScheduledExecutorService ses = partition.getWindow().getScheduledExecutorService();
            InsertionTimeList<T> tuples = partition.getContents();
            long evictTime = System.currentTimeMillis() - timeMs;
            
            tuples.evictOlderThan(evictTime);

            partition.process();
            
            if(!tuples.isEmpty()){
                ses.schedule(() -> partition.evict(), tuples.nextEvictDelay(timeMs), TimeUnit.MILLISECONDS);
            }
        };
    }
    
    /**
     * An eviction policy which processes the window, evicts all tuples, and 
     * schedules the next eviction after the appropriate interval.
     * @param <T> Tuple Type
     * @param <K> Key type
     * @param time The timespan in which tuple are permitted in the partition.
     * @param unit The units of time.
     * @return The time-based eviction policy.
     */ 
    public static <T, K> Consumer<Partition<T, K, List<T>> > evictAllAndScheduleEvictWithProcess(long time, TimeUnit unit){
        
        long timeMs = TimeUnit.MILLISECONDS.convert(time, unit);
        return (partition) -> {
            ScheduledExecutorService ses = partition.getWindow().getScheduledExecutorService();
            List<T> tuples = partition.getContents(); 

            partition.process();
            tuples.clear();
                        
            ses.schedule(() -> partition.evict(), timeMs, TimeUnit.MILLISECONDS);       
        };
    }
    
    
    /**
     * Returns an insertion policy that indicates the tuple
     * is to be inserted into the partition.
     * 
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * 
     * @return An insertion policy that always inserts.
     */
    public static <T, K, L extends List<T>> BiFunction<Partition<T, K, L>, T, Boolean> alwaysInsert(){
        return (partition, tuple) -> true;
    }
    
    /**
     * Returns a count-based contents policy.
     * If, when called, the number of tuples in the partition is
     * greater than equal to {@code count} then {@code partition.evict()}
     * is called.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @param count the count
     * @return A count-based contents policy.
     */
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> countContentsPolicy(final int count){
        return (partition, tuple) -> {
            if (partition.getContents().size() >= count)
                partition.evict();
        };
    }
    
    /**
     * Returns a Consumer representing an evict determiner that evict all tuples
     * from the window.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @return An evict determiner that evicts all tuples.
     */
    public static <T, K, L extends List<T>> Consumer<Partition<T, K, L> > evictAll(){
        return partition -> partition.getContents().clear();
    }
    
    /**
     * Returns an evict determiner that evicts the oldest tuple.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @return A evict determiner that evicts the oldest tuple.
     */
    public static <T, K, L extends List<T>> Consumer<Partition<T, K, L> > evictOldest(){
        return partition -> partition.getContents().remove(0);
    }
    
    /**
     * Returns a trigger policy that triggers
     * processing on every insert.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @return A trigger policy that triggers processing on every insert.
     */ 
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> processOnInsert(){
        return (partition, tuple) -> partition.process();
    }
    
    /**
     * Returns a trigger policy that triggers when the size of a partition
     * equals or exceeds a value, and then evicts its contents.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @param size partition size
     * @return A trigger policy that triggers processing when the size of 
     * the partition equals or exceets a value.
     */ 
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> processWhenFullAndEvict(final int size){
        return (partition, tuple) -> {
            if(partition.getContents().size() >= size){
                partition.process();
                partition.evict();
            }
        };
    }
    
    /**
     * A {@link BiConsumer} policy which does nothing.
     * @param <T> Tuple type
     * @param <K> Key type
     * @param <L> List type for the partition contents.
     * @return A policy which does nothing.
     */
    public static <T, K, L extends List<T>> BiConsumer<Partition<T, K, L>, T> doNothing(){
        return (partition, key) -> {};
    }
    
    public static <T> Supplier<InsertionTimeList<T>> insertionTimeList() {
        return () -> new InsertionTimeList<>();
    }
}
