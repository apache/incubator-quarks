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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;


class WindowImpl<T, K, L extends List<T>> implements Window<T, K, L> {
    private final BiFunction<Partition<T, K, L>, T, Boolean> insertionPolicy;
    private final BiConsumer<Partition<T, K, L>, T> contentsPolicy;
    private final Consumer<Partition<T, K, L> > evictDeterminer;
    private final BiConsumer<Partition<T, K, L>, T> triggerPolicy;
    private BiConsumer<List<T>, K> partitionProcessor;
    
    private ScheduledExecutorService ses;
    
    protected Supplier<L> listSupplier;
    protected Function<T, K> keyFunction;
    
    private Map<K, Partition<T, K, L> > partitions = new HashMap<K, Partition<T, K, L> >();
    
    
    WindowImpl(BiFunction<Partition<T, K, L>, T, Boolean> insertionPolicy, BiConsumer<Partition<T, K, L>, T> contentsPolicy,
            Consumer<Partition<T, K, L> > evictDeterminer, BiConsumer<Partition<T, K, L>, T> triggerPolicy,
            Function<T, K> keyFunction, Supplier<L> listSupplier){
        this.insertionPolicy = insertionPolicy;
        this.contentsPolicy = contentsPolicy;
        this.evictDeterminer = evictDeterminer;
        this.triggerPolicy = triggerPolicy;
        this.keyFunction = keyFunction;
        this.listSupplier = listSupplier;
    }

    @Override
    public boolean insert(T tuple) {
        K key = keyFunction.apply(tuple);
        Partition<T, K, L> partition;
        
        synchronized (partitions) {
            partition = partitions.get(key);
            if (partition == null) {
                partition = new PartitionImpl<T, K, L>(this, listSupplier.get(), key);
                partitions.put(key, partition);
            }
        }
        
        return partition.insert(tuple);      
    }

   
    @Override
    public synchronized void registerPartitionProcessor(BiConsumer<List<T>, K> partitionProcessor){
            this.partitionProcessor = partitionProcessor;
    }

    @Override
    public BiConsumer<Partition<T, K, L>, T> getContentsPolicy() {
        return contentsPolicy;
    }

    @Override
    public BiConsumer<Partition<T, K, L>, T> getTriggerPolicy() {
        return triggerPolicy;
    }

    @Override
    public synchronized BiConsumer<List<T>, K> getPartitionProcessor() {
            return partitionProcessor;    
    }

    @Override
    public BiFunction<Partition<T, K, L>, T, Boolean> getInsertionPolicy() {
        return insertionPolicy;
    }

    @Override
    public Consumer<Partition<T, K, L> > getEvictDeterminer() {
        return evictDeterminer;
    }

    @Override
    public Function<T, K> getKeyFunction() {
        return keyFunction;
    }

    @Override
    public synchronized void registerScheduledExecutorService(ScheduledExecutorService ses) {
        this.ses = ses;
        
    }

    @Override
    public synchronized ScheduledExecutorService getScheduledExecutorService() {
        return this.ses;
    }

    @Override
    public Map<K, Partition<T, K, L>> getPartitions() {
        return partitions;
    }

}
