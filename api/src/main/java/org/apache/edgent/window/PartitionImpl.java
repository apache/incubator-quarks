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
import java.util.List;

import org.apache.edgent.function.Consumer;

@SuppressWarnings("serial")
class PartitionImpl<T, K, L extends List<T>> implements Partition<T, K, L> {
    private final L tuples;
    private final List<T> unmodifiableTuples;
    private final Window<T, K, L> window;
    private final K key;
    
    PartitionImpl(Window<T, K, L> window, L tuples, K key){
        this.window = window;
        this.tuples = tuples;
        this.unmodifiableTuples = Collections.unmodifiableList(tuples);
        this.key = key;
    }

    @Override
    public synchronized boolean insert(T tuple) {        
        
        if (getWindow().getInsertionPolicy().apply(this, tuple)) {
            getWindow().getContentsPolicy().accept(this, tuple);
            this.tuples.add(tuple);
            // Trigger
            getWindow().getTriggerPolicy().accept(this, tuple);
            return true;
        }

        return true;
    }
    
    @Override
    public synchronized void process() {
        window.getPartitionProcessor().accept(unmodifiableTuples, key);
    }

    @Override
    public synchronized L getContents() {
        return tuples;
    }

    @Override
    public Window<T, K, L> getWindow() {
        return window;
    }
    
    @Override
    public K getKey() {
        return key;
    }

    @Override
    public synchronized void evict() {
        Consumer<Partition<T, K, L>> evictDeterminer = window.getEvictDeterminer();
        evictDeterminer.accept(this);
    }
}
