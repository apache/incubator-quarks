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

import static org.apache.edgent.window.Policies.alwaysInsert;
import static org.apache.edgent.window.Policies.countContentsPolicy;
import static org.apache.edgent.window.Policies.evictOldest;
import static org.apache.edgent.window.Policies.processOnInsert;

import java.util.LinkedList;
import java.util.List;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;

/**
 * Factory to create {@code Window} implementations.
 *
 */
public class Windows {  
    
    /**
     * Create a window using the passed in policies.
     *
     * @param <T> Tuple type.
     * @param <K> Key type.
     * @param <L> List type.
     *
     * @param insertionPolicy Policy indicating if a tuple should be inserted
     * into the window.
     * @param contentsPolicy Contents policy called prior to insertion of a tuple.
     * @param evictDeterminer Policy that determines action to take when
     * {@link Partition#evict()} is called.
     * @param triggerPolicy Trigger policy that is invoked after the insertion
     * of a tuple into a partition.
     * @param keyFunction Function that gets the partition key from a tuple.
     * @param listSupplier Supplier function for the {@code List} that holds
     * tuples within a partition.
     * @return Window using the passed in policies.
     */
    public static  <T, K, L extends List<T>> Window<T, K, L> window(
            BiFunction<Partition<T, K, L>, T, Boolean> insertionPolicy,
            BiConsumer<Partition<T, K, L>, T> contentsPolicy,
            Consumer<Partition<T, K, L> > evictDeterminer,
            BiConsumer<Partition<T, K, L>, T> triggerPolicy,
            Function<T, K> keyFunction,
            Supplier<L> listSupplier){
        
        return new WindowImpl<>(insertionPolicy, contentsPolicy, evictDeterminer, triggerPolicy, keyFunction, listSupplier);
    }
    
    /**
     * Return a window that maintains the last {@code count} tuples inserted
     * with processing triggered on every insert. This provides 
     * a continuous processing, where processing is invoked every
     * time the window changes. Since insertion drives eviction
     * there is no need to process on eviction, thus once the window
     * has reached {@code count} tuples, each insertion results in an
     * eviction followed by processing of {@code count} tuples
     * including the tuple just inserted, which is the definition of
     * the window.
     * 
     * @param <T> Tuple type.
     * @param <K> Key type.
     * 
     * @param count Number of tuple to maintain per partition
     * @param keyFunction Tuple partitioning key function
     * @return window that maintains the last {@code count} tuples on a stream
     */
    public static <T, K> Window<T, K, LinkedList<T>> lastNProcessOnInsert(final int count,
            Function<T, K> keyFunction) {

        Window<T, K, LinkedList<T>> window = Windows.window(
                alwaysInsert(),
                countContentsPolicy(count), 
                evictOldest(), 
                processOnInsert(), 
                keyFunction, 
                () -> new LinkedList<T>());

        return window;
    }
    
}
