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

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Predicate;

/**
 * Deadtime {@link Predicate}.
 * <p>
 * {@link #test(Object) test()} returns true on its initial call
 * and then false for any calls occurring during the following deadtime period.
 * After the end of a deadtime period, the next call to {@code test()} 
 * returns true and a new deadtime period is begun.
 * </p><p>
 * The deadtime period may be changed while the topology is running
 * via {@link #setPeriod(long, TimeUnit)}.
 * </p>
 *
 * @param <T> tuple type
 * @see Filters#deadtime(org.apache.edgent.topology.TStream, long, TimeUnit) Filters.deadtime()
 */
public class Deadtime<T> implements Predicate<T> {
    private static final long serialVersionUID = 1L;
    private long deadtimePeriodMillis;
    private long lastTrueTimeMillis;
    private volatile long nextTrueTimeMillis;

    /**
     * Create a new Deadtime Predicate
     * <p>
     * Same as {@code Deadtime(0, TimeUnit.SECONDS)}
     */
    public Deadtime() {
        setPeriod(0, TimeUnit.SECONDS);
    }
    
    /**
     * Create a new Deadtime Predicate
     * <p>
     * The first received tuple is always "accepted".
     * @param deadtimePeriod see {@link #setPeriod(long, TimeUnit) setPeriod()}
     * @param unit {@link TimeUnit} of {@code deadtimePeriod}
     */
    public Deadtime(long deadtimePeriod, TimeUnit unit) {
        setPeriod(deadtimePeriod, unit);
    }
    
    /**
     * Set the deadtime period
     * <p>
     * The end of a currently active deadtime period is shortened or extended
     * to match the new deadtime period specification.
     * </p><p>
     * The deadtime period behavior is subject to the accuracy
     * of the system's {@link System#currentTimeMillis()}.
     * A period of less than 1ms is equivalent to specifying 0.
     * </p>
     * @param deadtimePeriod the amount of time for {@code test()}
     *        to return false after returning true.
     *        Specify a value of 0 for no deadtime period.
     *        Must be &gt;= 0.
     * @param unit {@link TimeUnit} of {@code deadtimePeriod}
     */
    public synchronized void setPeriod(long deadtimePeriod, TimeUnit unit) {
        if (deadtimePeriod < 0)
            throw new IllegalArgumentException("deadtimePeriod");
        Objects.requireNonNull(unit, "unit");
        deadtimePeriodMillis = unit.toMillis(deadtimePeriod);
        nextTrueTimeMillis = lastTrueTimeMillis + deadtimePeriodMillis;
    }

    /**
     * Test the deadtime predicate.
     * @param value ignored
     * @return false if in a deadtime period, true otherwise
     */
    @Override
    public boolean test(T value) {
        long now = System.currentTimeMillis(); 
        if (now < nextTrueTimeMillis)
            return false;
        else synchronized(this) {
            lastTrueTimeMillis = now;
            nextTrueTimeMillis = now + deadtimePeriodMillis;
            return true;
        }
    }

    /**
     * Returns a String for development/debug support.  Content subject to change.
     */
    @Override
    public String toString() {
        return "nextPass after "+new Date(nextTrueTimeMillis);
    }
    
}