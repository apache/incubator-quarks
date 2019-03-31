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
package org.apache.edgent.topology.tester;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Submitter;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.TopologyElement;

import com.google.gson.JsonObject;

/**
 * A {@code Tester} adds the ability to test a org.apache.edgent.org.apache.edgent.topology in a test framework such
 * as JUnit.
 * 
 * The main feature is the ability to capture tuples from a {@link TStream} in
 * order to perform some form of verification on them. There are two mechanisms
 * to perform verifications:
 * <UL>
 * <LI>did the stream produce the correct number of tuples.</LI>
 * <LI>did the stream produce the correct tuples.</LI>
 * </UL>
 * Currently, only streams that are instances of
 * {@code TStream<String>} can have conditions or handlers attached.
 * <P>
 * A {@code Tester} modifies its {@link Topology} to achieve the above purpose.
 * </P>
 */
public interface Tester extends TopologyElement {

    /**
     * Return a condition that evaluates if {@code stream} has submitted exactly
     * {@code expectedCount} number of tuples. The function may be evaluated
     * after the {@link Submitter#submit(Object, JsonObject) submit}
     * call has returned. <BR>
     * The {@link Condition#getResult() result} of the returned
     * {@code Condition} is the number of tuples seen on {@code stream} so far.
     * <BR>
     * If the org.apache.edgent.org.apache.edgent.topology is still executing then the returned values from
     * {@link Condition#valid()} and {@link Condition#getResult()} may change as
     * more tuples are seen on {@code stream}. <BR>
     * 
     * @param stream
     *            Stream to be tested.
     * @param expectedCount
     *            Number of tuples expected on {@code stream}.
     * @return True if the stream has submitted exactly {@code expectedCount}
     *         number of tuples, false otherwise.
     */
    Condition<Long> tupleCount(TStream<?> stream, long expectedCount);

    /**
     * Return a condition that evaluates if {@code stream} has submitted at
     * least {@code expectedCount} number of tuples. The function may be
     * evaluated after the
     * {@link Submitter#submit(Object, JsonObject) submit} call has returned. <BR>
     * The {@link Condition#getResult() result} of the returned
     * {@code Condition} is the number of tuples seen on {@code stream} so far.
     * <BR>
     * If the org.apache.edgent.org.apache.edgent.topology is still executing then the returned values from
     * {@link Condition#valid()} and {@link Condition#getResult()} may change as
     * more tuples are seen on {@code stream}. <BR>
     * 
     * @param stream
     *            Stream to be tested.
     * @param expectedCount
     *            Number of tuples expected on {@code stream}.
     * @return Condition that will return true the stream has submitted at least
     *         {@code expectedCount} number of tuples, false otherwise.
     */
    Condition<Long> atLeastTupleCount(TStream<?> stream, long expectedCount);

    /**
     * Return a condition that evaluates if {@code stream} has submitted
     * tuples matching {@code values} in the same order. <BR>
     * The {@link Condition#getResult() result} of the returned
     * {@code Condition} is the tuples seen on {@code stream} so far. <BR>
     * If the org.apache.edgent.org.apache.edgent.topology is still executing then the returned values from
     * {@link Condition#valid()} and {@link Condition#getResult()} may change as
     * more tuples are seen on {@code stream}. <BR>
     * 
     * @param <T> Tuple type
     * @param stream
     *            Stream to be tested.
     * @param values
     *            Expected tuples on {@code stream}.
     * @return Condition that will return true if the stream has submitted at
     *         least tuples matching {@code values} in the same order, false
     *         otherwise.
     */
    <T> Condition<List<T>> streamContents(TStream<T> stream, @SuppressWarnings("unchecked") T... values);

    /**
     * Return a condition that evaluates if {@code stream} has submitted
     * tuples matching {@code values} in any order. <BR>
     * The {@link Condition#getResult() result} of the returned
     * {@code Condition} is the tuples seen on {@code stream} so far. <BR>
     * If the org.apache.edgent.org.apache.edgent.topology is still executing then the returned values from
     * {@link Condition#valid()} and {@link Condition#getResult()} may change as
     * more tuples are seen on {@code stream}. <BR>
     *
     * @param <T> Tuple type
     * @param stream
     *            Stream to be tested.
     * @param values
     *            Expected tuples on {@code stream}.
     * @return Condition that will return true if the stream has submitted at
     *         least tuples matching {@code values} in the any order, false
     *         otherwise.
     */
    <T> Condition<List<T>> contentsUnordered(TStream<T> stream, @SuppressWarnings("unchecked") T... values);
    
    /**
     * Return a condition that is valid only if all of {@code conditions} are valid.
     * The result of the condition is {@link Condition#valid()}
     * @param conditions Conditions to AND together.
     * @return condition that is valid only if all of {@code conditions} are valid.
     */
    Condition<Boolean> and(final Condition<?>... conditions);

    /**
     * Submit the org.apache.edgent.org.apache.edgent.topology for this tester and wait for it to complete, or reach
     * an end condition. If the org.apache.edgent.org.apache.edgent.topology does not complete or reach its end
     * condition before {@code timeout} then it is terminated.
     * <P>
     * End condition is usually a {@link Condition} returned from
     * {@link #atLeastTupleCount(TStream, long)} or
     * {@link #tupleCount(TStream, long)} so that this method returns once the
     * stream has submitted a sufficient number of tuples. <BR>
     * Note that the condition will be only checked periodically up to
     * {@code timeout}, so that if the condition is only valid for a brief
     * period of time, then its valid state may not be seen, and thus this
     * method will wait for the timeout period.
     * </P>
     * 
     * @param submitter the {@link Submitter}
     * @param config
     *            submission configuration.
     * @param endCondition
     *            Condition that will cause this method to return if it is true.
     * @param timeout
     *            Maximum time to wait for the org.apache.edgent.org.apache.edgent.topology to complete or reach its
     *            end condition.
     * @param unit
     *            Unit for {@code timeout}.
     * @return The value of {@code endCondition.valid()}.
     * 
     * @throws Exception
     *             Failure submitting or executing the org.apache.edgent.org.apache.edgent.topology.
     */
    boolean complete(Submitter<Topology, ? extends Job> submitter, JsonObject config, Condition<?> endCondition,
            long timeout, TimeUnit unit) throws Exception;
    
    /**
     * Get the {@code Job} reference for the org.apache.edgent.org.apache.edgent.topology submitted by {@code complete()}.
     * @return {@code Job} reference for the org.apache.edgent.org.apache.edgent.topology submitted by {@code complete()}.
     * Null if the {@code complete()} has not been called or the {@code Job} instance is not yet available.
     */
    Job getJob();
}
