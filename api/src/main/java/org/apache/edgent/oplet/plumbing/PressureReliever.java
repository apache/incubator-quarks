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
package org.apache.edgent.oplet.plumbing;

import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.edgent.function.BiConsumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Functions;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.Pipe;
import org.apache.edgent.window.Partition;
import org.apache.edgent.window.PartitionedState;
import org.apache.edgent.window.Policies;
import org.apache.edgent.window.Window;
import org.apache.edgent.window.Windows;

/**
 * Relieve pressure on upstream oplets by discarding tuples.
 * This oplet ensures that upstream processing is not
 * constrained by any delay in downstream processing,
 * for example by a sink oplet not being able to connect
 * to its external system.
 * When downstream processing cannot keep up with the input rate
 * this oplet maintains a defined window of the most recent
 * tuples and discards any earlier tuples using arrival order.
 * <P>
 * A window partition is maintained for each key seen
 * on the input stream. Any tuple arriving on the input
 * stream is inserted into the window. Asynchronously
 * tuples are taken from the window using FIFO and
 * submitted downstream. The submission of tuples maintains
 * order within a partition but not across partitions.
 * </P>
 * <P>
 * Tuples are  <B>discarded and not</B> submitted to the
 * output port if the downstream processing cannot keep up
 * the incoming tuple rate.
 * </P>
 * <UL>
 * <LI>For a {@link #PressureReliever(int, Function) count}
 * {@code PressureReliever} up to last (most recent) {@code N} tuples
 * are maintained in a window partition.
 * <BR> Asynchronous tuple submission removes the last (oldest) tuple in the partition
 * before submitting it downstream.
 * <BR> If when an input tuple is processed the window partition contains N tuples, then
 * the first (oldest) tuple in the partition is discarded before the input tuple is inserted into the window.
 * </UL>
 * <P>
 * Insertion of the oplet into a stream disconnects the
 * upstream processing from the downstream processing,
 * so that downstream processing is executed on a different
 * thread to the thread that processed the input tuple.
 * </P>
 * 
 * @param <T> Tuple type.
 * @param <K> Key type.
 */
public class PressureReliever<T, K> extends Pipe<T, T> {
    private static final long serialVersionUID = 1L;

    private ScheduledExecutorService executor;
    private final Window<T, K, LinkedList<T>> window;

    /**
     * Pressure reliever that maintains up to {@code count} most recent tuples per key.
     *
     * @param count Number of tuples to maintain where downstream processing cannot keep up.
     * @param keyFunction Key function for tuples.
     */
    public PressureReliever(int count, Function<T, K> keyFunction) {
        window = Windows.window(
                Policies.alwaysInsert(),
                Policies.countContentsPolicy(count),
                Policies.evictOldest(),
                new FirstSubmitter(),
                keyFunction,
                () -> new LinkedList<T>());

        // No processing of the window takes place
        window.registerPartitionProcessor((tuples, k) -> { });
    }    

    @Override
    public void initialize(OpletContext<T, T> context) {
        super.initialize(context);
        executor = context.getService(ScheduledExecutorService.class);
    }

    @Override
    public void accept(T tuple) {
        window.insert(tuple);
    }

    @Override
    public void close() throws Exception {
    }

    private class FirstSubmitter extends PartitionedState<K, AtomicBoolean>
            implements BiConsumer<Partition<T, K, LinkedList<T>>, T> {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        FirstSubmitter() {
            super(() -> new AtomicBoolean());
        }

        /**
         * Process the window (to consume the oldest tuple in the partition)
         * only if a tuple from this partition is not already being consumed.
         * 
         * @param t
         * @param v
         */
        @Override
        public void accept(Partition<T, K, LinkedList<T>> partition, T tuple) {
            submitNextTuple(partition);
        }

        private void submitNextTuple(Partition<T, K, LinkedList<T>> partition) {
            final K key = partition.getKey();
            final AtomicBoolean latch = getState(key);
            if (!latch.compareAndSet(false, true))
                return;
            
            final T firstTuple;
            synchronized (partition) {
                final LinkedList<T> contents = partition.getContents();
                if (contents.isEmpty()) {
                    latch.set(false);
                    return;
                }

                firstTuple = contents.removeFirst();
            }

            Runnable submit = Functions.delayedConsume(getDestination(), firstTuple);
            submit = Functions.runWithFinal(submit, () -> {
                latch.set(false);
                submitNextTuple(partition);
            });

            executor.execute(submit);
        }
    }
}
