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
package org.apache.edgent.topology.plumbing;

import org.apache.edgent.function.Predicate;

/**
 * A generic "valve" {@link Predicate}.
 * <p>
 * A valve is either open or closed.
 * When used as a Predicate to {@code TStream.filter()},
 * filter passes tuples only when the valve is open.
 * </p><p>
 * A valve is typically used to dynamically control whether or not
 * some downstream tuple processing is enabled.  A decision to change the
 * state of the valve may be a result of local analytics or an external
 * command.
 * <br>
 * E.g., in a simple case, a Valve might be used to control
 * whether or not logging or publishing of tuples is enabled.
 * </p>
 * <pre>{@code
 * TStream<JsonObject> stream = ...;
 * 
 * Valve<JsonObject> valve = new Valve<>(false);
 * stream.filter(valve).sink(someTupleLoggingConsumer);
 *                                 
 * // from some analytic or device command handler...
 *     valve.setOpen(true);
 * }</pre>
 *
 * @param <T> tuple type
 */
public class Valve<T> implements Predicate<T> {
    private static final long serialVersionUID = 1L;
    private transient boolean isOpen;

    /**
     * Create a new Valve Predicate
     * <p>
     * Same as {@code Valve(true)}
     */
    public Valve() {
        this(true);
    }
    
    /**
     * Create a new Valve Predicate
     * <p>
     * @param isOpen the initial state
     */
    public Valve(boolean isOpen) {
        setOpen(isOpen);
    }
    
    /**
     * Set the valve state
     * @param isOpen true to open the valve
     */
    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
    
    /**
     * Get the valve state
     * @return the state, true if the valve is open, false otherwise
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Test the state of the valve, {@code value} is ignored.
     * @return true when the valve is open, false otherwise
     */
    @Override
    public boolean test(T value) {
        return isOpen;
    }

    /**
     * Returns a String for development/debug support.  Content subject to change.
     */
    @Override
    public String toString() {
        return "isOpen="+isOpen;
    }
    
}