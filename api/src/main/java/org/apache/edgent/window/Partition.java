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

import java.io.Serializable;
import java.util.List;

/**
 * A partition within a {@code Window}. The contents of the list
 * returned by {@code getContents} is stable when synchronizing 
 * on the partition object. For example:
 * 
 * <pre>{@code
 * Partition<Integer, Integer, ArrayList<Integer>> part = ...;
 * synchronized(part){
 *  List<Integer> = part.getContents();
 *  // stable operation on contents of partition
 * }
 * }</pre>
 *
 * @param <T> Type of tuples in the partition.
 * @param <K> Type of the partition's key.
 * @param <L> Type of the list holding the partition's tuples.
 * 
 * @see Window
 */
public interface Partition<T, K, L extends List<T>> extends Serializable{    
    /**
     * Offers a tuple to be inserted into the partition.
     * @param tuple Tuple to be offered.
     * @return True if the tuple was inserted into this partition, false if it was rejected.
     */
    boolean insert(T tuple);
        
    /**
     * Invoke the WindowProcessor's processWindow method. A partition processor
     * must be registered prior to invoking process().
     */
    void process();
    
    /** 
     * Calls the partition's evictDeterminer.
     */
    void evict();
    
    /**
     * Retrieves the window contents.
     * @return list of partition contents
     */
    L getContents();
    
    /**
     * Return the window in which this partition is contained.
     * @return the partition's window
     */
    Window<T, K, L> getWindow();
    
    /**
     * Returns the key associated with this partition
     * @return The key of the partition.
     */
    K getKey();
}
