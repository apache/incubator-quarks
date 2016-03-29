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
package quarks.topology.spi.graph;

import quarks.function.BiFunction;
import quarks.function.Function;
import quarks.function.Functions;
import quarks.oplet.window.Aggregate;
import quarks.topology.TStream;
import quarks.window.InsertionTimeList;
import quarks.window.Policies;
import quarks.window.Window;
import quarks.window.Windows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static quarks.window.Policies.alwaysInsert;
import static quarks.window.Policies.evictOlderWithProcess;
import static quarks.window.Policies.insertionTimeList;
import static quarks.window.Policies.processOnInsert;
import static quarks.window.Policies.scheduleEvictIfEmpty;

public class TWindowTimeImpl<T, K> extends AbstractTWindow<T, K> {
    private long time;
    private TimeUnit unit;
    
    TWindowTimeImpl(long time, TimeUnit unit, TStream<T> feed, Function<T, K> keyFunction){
        super(feed, keyFunction);
        this.time = time;
        this.unit = unit;
    }

    /**
     * Window behaviour here is that:
     * 
     * Tuples are always inserted into the partition.
     * 
     * If before insertion the partition is empty the
     * a eviction is scheduled 
     * 
     * After insertion the window is processed so that
     * any change triggers this continuous aggregation.
     * 
     * The evict determiner evicts any tuples that have
     * been in the window longer that the configured
     * size, and invokes the processing on any eviction.
     * Multiple tuples may have been evicted before processing.
     */
    @Override
    public <U> TStream<U> aggregate(BiFunction<List<T>,K, U> processor) {    
        processor = Functions.synchronizedBiFunction(processor);
        Window<T, K, InsertionTimeList<T>> window =
                Windows.window(
                        alwaysInsert(),
                        scheduleEvictIfEmpty(time, unit),
                        evictOlderWithProcess(time, unit),
                        processOnInsert(),
                        getKeyFunction(),
                        insertionTimeList());
        
        Aggregate<T,U,K> op = new Aggregate<T,U,K>(window, processor);
        return feeder().pipe(op); 
    }

    @Override
    public <U> TStream<U> batch(BiFunction<List<T>, K, U> batcher) {
        batcher = Functions.synchronizedBiFunction(batcher);
        Window<T, K, List<T>> window =
                Windows.window(
                        alwaysInsert(),
                        Policies.scheduleEvictOnFirstInsert(time, unit),
                        Policies.evictAllAndScheduleEvictWithProcess(time, unit),
                        (partition, tuple) -> {},
                        getKeyFunction(),
                        () -> new ArrayList<T>());
        
        Aggregate<T,U,K> op = new Aggregate<T,U,K>(window, batcher);
        return feeder().pipe(op); 
    } 
}
