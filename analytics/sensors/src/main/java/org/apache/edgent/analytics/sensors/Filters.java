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
package org.apache.edgent.analytics.sensors;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Function;
import org.apache.edgent.function.Predicate;
import org.apache.edgent.topology.TStream;

/**
 * Filters aimed at sensors.
 */
public class Filters {
	
	private Filters() {}

    /**
     * Deadband filter with maximum suppression time.
     * 
     * A filter that discards any tuples that are in the deadband, uninteresting to downstream consumers.
     * <P>
     * A tuple {@code t} is passed through the deadband filter if:
     * <UL>
     * <LI>
     * {@code inBand.test(value.apply(t))} is false, that is the tuple's value is outside of the deadband
     * </LI>
     * <LI>
     * OR {@code inBand.test(value.apply(t))} is true AND the last tuple's value was outside of the deadband.
     * This corresponds to the first tuple's value inside the deadband after a period being outside it.
     * </LI>
     * <LI>
     * OR it has been more than {@code maximumSuppression} seconds (in unit {@code unit}) 
     * </LI>
     * <LI>
     * OR it is the first tuple (effectively the state of the filter starts as outside of the deadband).
     * </LI>
     * </UL>
     * 
     * @param <T> Tuple type.
     * @param <V> Value type for the deadband function.
     * 
     * @param stream Stream containing readings.
     * @param value Function to obtain the tuple's value passed to the deadband function.
     * @param inBand Function that defines the deadband.
     * @param maximumSuppression Maximum amount of time to suppress values that are in the deadband.
     * @param unit Unit for {@code maximumSuppression}.
     * @return Filtered stream.
     */
    public static <T, V> TStream<T> deadband(TStream<T> stream, Function<T, V> value, Predicate<V> inBand,
            long maximumSuppression, TimeUnit unit) {

        return stream.filter(new Deadband<>(value, inBand, maximumSuppression, unit));
    }
    
    /**
     * Deadband filter.
     * 
     * A filter that discards any tuples that are in the deadband, uninteresting to downstream consumers.
     * <P>
     * A tuple {@code t} is passed through the deadband filter if:
     * <UL>
     * <LI>
     * {@code inBand.test(value.apply(t))} is false, that is the value is outside of the deadband
     * </LI>
     * <LI>
     * OR {@code inBand.test(value.apply(t))} is true and the last value was outside of the deadband.
     * This corresponds to the first value inside the deadband after a period being outside it.
     * </LI>
     * <LI>
     * OR it is the first tuple (effectively the state of the filter starts as outside of the deadband).
     * </LI>
     * </UL>
     * <P>
     * Here's an example of how {@code deadband()} would pass through tuples for a sequence of
     * values against the shaded dead band area. Circled values are ones that are passed through
     * the filter to the returned stream.
     * <BR>
     * <UL>
     * <LI>All tuples with a value outside the dead band.</LI>
     * <LI>Two tuples with values within the dead band that are the first time values return to being in band
     * after being outside of the dead band.</LI>
     * <LI>The first tuple.</LI>
     * </UL>
     * <BR>
     * <img src="doc-files/deadband.png" alt="Deadband example">
     * 
     * @param <T> Tuple type.
     * @param <V> Value type for the deadband function.
     * 
     * @param stream Stream containing readings.
     * @param value Function to obtain the value passed to the deadband function.
     * @param inBand Function that defines the deadband.
     * @return Filtered stream.
     */
    public static <T, V> TStream<T> deadband(TStream<T> stream, Function<T, V> value, Predicate<V> inBand) {

        return stream.filter(new Deadband<>(value, inBand));
    }
    
    /**
     * Deadtime filter.
     * 
     * A filter that discards tuples for a period of time after passing
     * a tuple.
     * <p>
     * E.g., for a deadtime period of 30 minutes, after letting a tuple
     * pass through, any tuples received during the next 30 minutes are
     * filtered out.  Then the next arriving tuple is passed through and
     * a new deadtime period is begun.
     * </p><p>
     * Use {@link Deadtime} directly if you need to change the deadtime period
     * while the org.apache.edgent.org.apache.edgent.topology is running.
     * </p>
     * @param <T> tuple type
     * @param stream TStream to add deadtime filter to
     * @param deadtimePeriod the deadtime period in {@code unit}
     * @param unit the {@link TimeUnit} to apply to {@code deadtimePeriod}
     * @return the deadtime filtered stream
     * @see Deadtime
     */
    public static <T> TStream<T> deadtime(TStream<T> stream, long deadtimePeriod, TimeUnit unit) {
        return stream.filter(new Deadtime<>(deadtimePeriod, unit));
    }
    
}
