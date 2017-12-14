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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.edgent.analytics.sensors.Range.BoundType;
import org.apache.edgent.function.Predicate;

/**
 * Convenience functions and utility operations on {@link Range}.
 */
public final class Ranges {

    /** 
     * Create a Range (lowerEndpoint..upperEndpoint) (both exclusive/OPEN)
     * <p>
     * Same as {@code Range.range(BoundType.OPEN, lowerEndpoint, upperEndpoint, BoundType.OPEN)}
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> open(T lowerEndpoint, T upperEndpoint) { 
        return Range.range(lowerEndpoint, BoundType.OPEN, upperEndpoint, BoundType.OPEN);
    }

    /** 
     * Create a Range [lowerEndpoint..upperEndpoint] (both inclusive/CLOSED)
     * <p>
     * Same as {@code Range.range(BoundType.CLOSED, lowerEndpoint, upperEndpoint, BoundType.CLOSED)}
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> closed(T lowerEndpoint, T upperEndpoint) {
        return Range.range(lowerEndpoint, BoundType.CLOSED, upperEndpoint, BoundType.CLOSED); 
    }

    /** 
     * Create a Range (lowerEndpoint..upperEndpoint] (exclusive/OPEN,inclusive/CLOSED)
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> openClosed(T lowerEndpoint, T upperEndpoint) {
        return Range.range(lowerEndpoint, BoundType.OPEN, upperEndpoint, BoundType.CLOSED);
    }

    /** 
     * Create a Range [lowerEndpoint..upperEndpoint) (inclusive/CLOSED,exclusive/OPEN)
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> closedOpen(T lowerEndpoint, T upperEndpoint) {
        return Range.range(lowerEndpoint, BoundType.CLOSED, upperEndpoint, BoundType.OPEN);
    }

    /** 
     * Create a Range (lowerEndpoint..*) (exclusive/OPEN)
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> greaterThan(T lowerEndpoint) {
        return Range.range(lowerEndpoint, BoundType.OPEN, null, BoundType.OPEN);
    }

    /**
     * Create a Range [lowerEndpoint..*) (inclusive/CLOSED)
     * 
     * @param <T> Endpoint type
     * @param lowerEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> atLeast(T lowerEndpoint) {
        return Range.range(lowerEndpoint, BoundType.CLOSED, null, BoundType.OPEN);
    }

    /** 
     * Create a Range (*..upperEndpoint) (exclusive/OPEN)
     * 
     * @param <T> Endpoint type
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> lessThan(T upperEndpoint) {
        return Range.range(null, BoundType.OPEN, upperEndpoint, BoundType.OPEN);
    }

    /** 
     * Create a Range (*..upperEndpoint] (inclusive/CLOSED)
     * 
     * @param <T> Endpoint type
     * @param upperEndpoint the endpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> atMost(T upperEndpoint) {
        return Range.range(null, BoundType.OPEN, upperEndpoint, BoundType.CLOSED);
    }

    /** 
     * Create a Range [endpoint..endpoint] (both inclusive/CLOSED)
     * 
     * @param <T> Endpoint type
     * @param endpoint the endpoint
     * @return the Range
     */
    public static  <T extends Comparable<?>> Range<T> singleton(T endpoint) {
        return Range.range(endpoint, BoundType.CLOSED, endpoint, BoundType.CLOSED);
    }
    
    /**
     * Create a Predicate whose {@code test(v)} behaves like {@code ! range.test(v)}
     * <p>
     * This can be useful in a situation such as filtering to include only values
     * that are outside of a range.
     * <pre>{@code
     *     TStream<?> outsideRange = readings.filter(Ranges.outsideOf(Ranges.open(10,20)));
     * }</pre>
     * 
     * @param <T> Endpoint type
     * @param range the Range
     * @return the Predicate
     */
    public static <T extends Comparable<?>> Predicate<T> outsideOf(Range<T> range) {
        return t -> ! range.test(t);
    }
    
    /**
     * Create a Range from a Range&lt;Integer&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Integer> valueOfInteger(String str) {
        return Range.valueOf(str, v -> Integer.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;Short&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Short> valueOfShort(String str) {
        return Range.valueOf(str, v -> Short.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;Byte&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Byte> valueOfByte(String str) {
        return Range.valueOf(str, v -> Byte.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;Long&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Long> valueOfLong(String str) {
        return Range.valueOf(str, v -> Long.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;Float&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Float> valueOfFloat(String str) {
        return Range.valueOf(str, v -> Float.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;Double&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Double> valueOfDouble(String str) {
        return Range.valueOf(str, v -> Double.valueOf(v));
    }
    
    /**
     * Create a Range from a Range&lt;BigInteger&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<BigInteger> valueOfBigInteger(String str) {
        return Range.valueOf(str, v -> new BigInteger(v));
    }
    
    /**
     * Create a Range from a Range&lt;BigDecimal&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<BigDecimal> valueOfBigDecimal(String str) {
        return Range.valueOf(str, v -> new BigDecimal(v));
    }
    
    /**
     * Create a Range from a Range&lt;String&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if str includes a String
     * endpoint value containing "..".
     */
    public static Range<String> valueOfString(String str) {
        return Range.valueOf(str, v -> new String(v));
    }
    
    /**
     * Create a Range from a Range&lt;Character&gt;.toString() value.
     * @param str the String
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert
     *         the endpoint strings to the type
     */
    public static Range<Character> valueOfCharacter(String str) {
        return Range.valueOf(str, v -> Character.valueOf(v.charAt(0)));
    }

}
