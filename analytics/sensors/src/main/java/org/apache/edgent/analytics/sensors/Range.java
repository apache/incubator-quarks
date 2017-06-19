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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.apache.edgent.function.Function;
import org.apache.edgent.function.Predicate;

import static org.apache.edgent.analytics.sensors.utils.Java7Helper.*;

/**
 * A generic immutable range of values and a way to 
 * check a value for containment in the range.
 * <p>
 * Useful in filtering in predicates.
 * <p> 
 * A Range consists of a lower and upper endpoint and a {@link BoundType}
 * for each endpoint.
 * <p>
 * {@link BoundType#CLOSED} includes the endpoint's value in the range and
 * is represented as "[" or "]" in the string form for a lower and upper bound
 * type respectively.
 * {@link BoundType#OPEN} excludes the endpoint's value in the range and 
 * is represented as "(" or ")" in the string form for a lower and upper bound
 * type respectively.  e.g. {@code "[2..5)"}
 * <p>
 * Typically, the convenience methods in {@link Ranges} are used to
 * construct a Range.
 * <p>
 * {@link #contains(Comparable) contains()} is used to check for containment:
 * e.g.
 * <pre>{@code
 * Ranges.closed(2,4).contains(2);    // returns true
 * Ranges.open(2,4).contains(2);      // returns false
 * Ranges.atLeast(2).contains(2);     // returns true
 * Ranges.greaterThan(2).contains(2); // returns false
 * Ranges.atMost(2).contains(2);      // returns true
 * Ranges.lessThan(2).contains(2);    // returns false
 * }</pre>
 * 
 * {@link #toString()} yields a convenient string representation and a
 * Range may be created from the string representation.
 * <pre>{@code
 * String s = Ranges.closed(2,4).toString(); // yields "[2..4]"
 * Range<Integer> range = Ranges.valueOfInteger(s); // yields a Range<Integer>(2,4)
 * }</pre>
 * <p>
 * Range works with Gson ({@code new Gson().toJson(Range<T>)}.
 * As as documented by Gson, for generic types you must use
 * {@code Gson.fromJson(String, java.lang.reflect.Type)} to
 * create a Range from its json.
 * <p>
 * Sample use in a TStream context:
 * <pre>{@code
 * TStream<JsonObject> jStream = ...;
 * TStream<JsonObject> filtered = Filters.deadband(jStream,
 *                     json -> json.getProperty("pressureReading").asInteger(),
 *                     Ranges.closed(10, 30));
 * }</pre>
 * 
 * @param <T> a {@link Comparable} value type
 */
public final class Range<T extends Comparable<?>> implements Predicate<T>, Serializable {
    /*
     * Useful in understanding some of the motivations/issues:
     * 
     * This is a lightweight implementation of a subset of the Guava Range.
     * Guava Range is great, but...
     * 
     * Compared to Guava Range:
     * - Guava Range isn't easily separable from the "bulky" Guave package.
     *   Guava Range itself isn't particularly lightweight (depends on numerous
     *   other Guava classes).
     *   This Range is very lightweight.
     * - Guava Range has a large set of static convenience constructors,
     *   whereas we've chosen to make them members of our Ranges class
     *   (along with "Range from Range.toString()" methods)
     * - Guava Range doesn't support unsigned byte/short/int/long type ranges.
     *     This Range adds {@link #contains(Comparable, Comparator)}
     *     and {@link #toStringUnsigned()}.
     * - Guava Range lacks a "Range from Range.toString()" function: https://github.com/google/guava/issues/1911.
     *     Ranges has valueOf*() for convenience with commonly used numeric types,
     *     and Range has valueOf() for general use. 
     * - Guava Range has issues with to/from Json with Gson: 
     *     https://github.com/google/guava/issues/1911.
     *     This Range works but as doc'd by Gson, you must use
     *     {@code Gson#fromJson(String, java.lang.reflect.Type)}</li>
     * - Guava Range's {@code apply(T value)} is documented as deprecated. 
     *     None the less there was a desire that our Range "implement Predicate"
     *     so we do.
     * - Guava Range.toString()
     *   - Guava uses some unprintable characters.
     *     Up to the latest Guava release - 19.0, Range.toString() uses \u2025 for
     *     the ".." separator and uses +/-\u221E for infinity.  That's caused problems:
     *     https://github.com/google/guava/issues/2376.
     *     Guava Range.toString() has been change to use ".." instead of \u2025.
     *     It still uses the unicode char for infinity.
     *     This Range uses ".." for the separator like the not-yet-released Guava change.
     *     For convenience to users, this Range uses "*" and no leading +/- for infinity.</li>
     *   - Guava does not decorate String or Character values with \" or \' respectively.
     *     It does not generate an escaped encoding of the
     *     range separator if it is present in a value.
     *     Hard to guess whether this may change if/when Guava adds a
     *     "Range from Range.toString()" capability.
     */
    
    private static final long serialVersionUID = 1L;
    
    private final T lowerEndpoint;  // null for infinity
    private final T upperEndpoint;  // null for infinity
    private final BoundType lbt;
    private final BoundType ubt;
    private transient int hashCode;
    
    /**
     * Exclude or include an endpoint value in the range.
     */
    public static enum BoundType {/** exclusive */ OPEN, /** inclusive */ CLOSED};
    
    /**
     * Create a new Range<T>.  Private like Guava Range.
     * <p>
     * See {@link Ranges} for a collection of convenience constructors.
     * @param lowerEndpoint null for an infinite value (and lbt must be OPEN)
     * @param lbt {@link BoundType} for the lowerEndpoint
     * @param upperEndpoint null for an infinite value (and ubt must be OPEN)
     * @param ubt {@link BoundType} for the upperEndpoint
     */
    private Range(T lowerEndpoint, BoundType lbt, T upperEndpoint, BoundType ubt) {
        this.lowerEndpoint = lowerEndpoint;
        this.upperEndpoint = upperEndpoint;
        this.lbt = lbt;
        this.ubt = ubt;
        if (lowerEndpoint != null && upperEndpoint != null) {
            if (lowerEndpoint.getClass() != upperEndpoint.getClass())
                throw new IllegalArgumentException("lowerEndpoint and upperEndpoint are not the same type");
        }
        if (lowerEndpoint == null && lbt != BoundType.OPEN)
            throw new IllegalArgumentException("endpoint is null and BoundType != OPEN");
        if (upperEndpoint == null && ubt != BoundType.OPEN)
            throw new IllegalArgumentException("endpoint is null and BoundType != OPEN");
    }

    /**
     * Create a new Range&lt;T&gt;
     * <p>
     * See {@link Ranges} for a collection of convenience constructors.
     * 
     * @param <T> a Comparable type
     * @param lowerEndpoint null for an infinite value (and lbt must be OPEN)
     * @param lbt {@link BoundType} for the lowerEndpoint
     * @param upperEndpoint null for an infinite value (and ubt must be OPEN)
     * @param ubt {@link BoundType} for the upperEndpoint
     * @return the Range
     */
    public static <T extends Comparable<?>> Range<T> range(T lowerEndpoint, BoundType lbt, T upperEndpoint, BoundType ubt) {
        // matchs Guava Range.range param order
        return new Range<T>(lowerEndpoint, lbt, upperEndpoint, ubt);
    }

    /**
     * Returns true if o is a Range having equal endpoints and bound types to this Range.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Range) {
            Range<?> r = (Range<?>) o;
            return r.lbt.equals(lbt)
                   && r.ubt.equals(ubt)
                   && (r.lowerEndpoint==null ? r.lowerEndpoint == lowerEndpoint
                                          : r.lowerEndpoint.equals(lowerEndpoint))
                   && (r.upperEndpoint==null ? r.upperEndpoint == upperEndpoint
                                          : r.upperEndpoint.equals(upperEndpoint));
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if (hashCode == 0)
            hashCode = Objects.hash(lbt, lowerEndpoint, ubt, upperEndpoint);
        return hashCode;
    }

    /**
     * @return true iff the Range's lower endpoint isn't unbounded/infinite
     */
    public boolean hasLowerEndpoint() {
        return lowerEndpoint != null;
    }
    
    /**
     * Get the range's lower endpoint.
     * @return the endpoint
     * @throws IllegalStateException if hasLowerEndpoint()==false
     */
    public T lowerEndpoint() {
        if (hasLowerEndpoint())
            return lowerEndpoint;
        throw new IllegalStateException("unbounded");
    }
    
    /**
     * @return true iff the Range's upper endpoint isn't unbounded/infinite
     */
    public boolean hasUpperEndpoint() {
        return upperEndpoint != null;
    }
    
    /**
     * Get the range's upper endpoint.
     * @return the endpoint
     * @throws IllegalStateException if hasUpperEndpoint()==false
     */
    public T upperEndpoint() {
        if (hasUpperEndpoint())
            return upperEndpoint;
        throw new IllegalStateException("unbounded");
    }
    
    /**
     * Get the BoundType for the lowerEndpoint.
     * @return the BoundType
     */
    public BoundType lowerBoundType() {
        return lbt;
    }
    
    /**
     * Get the BoundType for the upperEndpoint.
     * @return the BoundType
     */
    public BoundType upperBoundType() {
        return ubt;
    }
    
    /**
     * Determine if the Region contains the value.
     * <p>
     * {@code contains(v)} typically suffices.  This
     * can be used in cases where it isn't sufficient.
     * E.g., for unsigned byte comparisons
     * <pre>
     * Comparator&lt;Byte&gt; unsignedByteComparator = new Comparator&lt;Byte&gt;() {
     *     public int compare(Byte b1, Byte b2) {
     *         return intCompareUnsigned(byteToUnsignedInt(b1), byteToUnsignedInt(b2));
     *     }
     *     public boolean equals(Object o2) { return o2==this; }
     *     };
     * Range&lt;Byte&gt; unsignedByteRange = Ranges.valueOfByte("[0..255]");
     * unsignedByteRange.contains(byteValue, unsignedByteComparator);
     * </pre>
     * 
     * @param v the value to check for containment
     * @param cmp the Comparator to use
     * @return true if the Region contains the value
     */
    public boolean contains(T v, Comparator<T> cmp) {
        // N.B. Guava Range lacks such a method.
        if (lowerEndpoint==null) {
            int r = cmp.compare(v, upperEndpoint);
            return ubt == BoundType.OPEN ? r < 0 : r <= 0; 
        }
        if (upperEndpoint==null) {
            int r = cmp.compare(v, lowerEndpoint);
            return lbt == BoundType.OPEN ? r > 0 : r >= 0; 
        }
        int r = cmp.compare(v, upperEndpoint);
        boolean ok1 = ubt == BoundType.OPEN ? r < 0 : r <= 0;
        if (!ok1) 
            return false;
        r = cmp.compare(v, lowerEndpoint);
        return lbt == BoundType.OPEN ? r > 0 : r >= 0; 
    }
    
    /**
     * Determine if the Region contains the value.
     * <p>
     * The Comparable's compareTo() is used for the comparisons.
     * <p>
     * @param v the value to check for containment
     * @return true if the Region contains the value
     * @see #contains(Comparable, Comparator)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean contains(T v) {
        if (lowerEndpoint==null) {
            int r = ((Comparable)v).compareTo(upperEndpoint);
            return ubt == BoundType.OPEN ? r < 0 : r <= 0; 
        }
        if (upperEndpoint==null) {
            int r = ((Comparable)v).compareTo(lowerEndpoint);
            return lbt == BoundType.OPEN ? r > 0 : r >= 0; 
        }
        int r = ((Comparable)v).compareTo(upperEndpoint);
        boolean ok1 = ubt == BoundType.OPEN ? r < 0 : r <= 0;
        if (!ok1) 
            return false;
        r = ((Comparable)v).compareTo(lowerEndpoint);
        return lbt == BoundType.OPEN ? r > 0 : r >= 0; 
    }

    /**
     * Predicate.test() implementation. Identical to contains(value).
     */
    @Override
    public boolean test(T value) {
        return contains(value);
    }
    
    /**
     * Parse a String from {@link #toString()}
     * 
     * @param s the String
     * @return Four element array with the range's component Strings
     * @throws IllegalArgumentException
     */
    private static String[] parse(String s) {
        char lbc = s.charAt(0);
        if (lbc != '[' && lbc != '(')
            throw new IllegalArgumentException(s);
        String lbs = lbc=='[' ? "[" : "(";
        char ubc = s.charAt(s.length()-1);
        if (ubc != ']' && ubc != ')')
            throw new IllegalArgumentException(s);
        String ubs = ubc==']' ? "]" : ")";
        
        s = s.substring(1,s.length()-1);
        // problematic case: String endpoint with embedded ".."
        // special case Character endpoint with value "."
        String[] parts = new String[2];
        if (s.length() == "....".length()) {
            parts[0] = String.valueOf(s.charAt(0));
            parts[1] = String.valueOf(s.charAt(3));
        }
        else {
            parts = s.split("\\.\\.");
            if (parts.length != 2)
                throw new IllegalArgumentException("A range string endpoint value contains the separator sequence \"..\": " + s);
            if (parts[0].isEmpty() && parts[1].length() > 1) {
                // handles the case Range<String>(".", "anythingElse")
                // Range<String>("anything", ".") falls out OK from split
                parts[0] = parts[1].substring(0, 1);
                parts[1] = parts[1].substring(1);
            }
        }
        
        String les = parts[0];
        String ues = parts[1];

        return new String[]{ lbs, les, ues, ubs };
    }
    
    /**
     * Create a Range from a String produced by toString().
     * <p>
     * See {@link Ranges} for a collection of valueOf methods
     * for several types of {@code T}.
     *
     * @param <T> a Comparable type
     * @param toStringValue value from toString() or has the same syntax.
     * @param fromString function to create a T from its String value from
     *        the parsed toStringValue.  Should throw an IllegalArgumentException
     *        if unable to perform the conversion.
     * @return the Range
     * @throws IllegalArgumentException if unable to parse or convert to 
     *         endpoint in toStringValue to a T.
     */
    public static <T extends Comparable<?>> Range<T> valueOf(String toStringValue, Function<String,T> fromString) {
        // N.B. See note in classdoc wrt Guava Range behavior. i.e., it
        // currently lacks a "Range from Range.toString() analog".
        
        String[] parts = parse(toStringValue);
        BoundType lbt = parts[0] == "[" ? BoundType.CLOSED : BoundType.OPEN;
        String les = parts[1];
        String ues = parts[2];
        BoundType ubt = parts[3] == "]" ? BoundType.CLOSED : BoundType.OPEN;

        T lowerEndpoint = les.equals("*") ? null : fromString.apply(les);
        T upperEndpoint = ues.equals("*") ? null : fromString.apply(ues);
        
        return new Range<T>(lowerEndpoint, lbt, upperEndpoint, ubt);
    }
    
    /**
     * Yields {@code "<lowerBoundType><lowerEndpoint>..<upperEndpoint><upperBoundType>"}.
     * <p>
     * lowerBoundType is either "[" or "(" for BoundType.CLOSED or OPEN respectively.
     * upperBoundType is either "]" or ")" for BoundType.CLOSED or OPEN respectively.
     * <p>
     * The endpoint value "*" is used to indicate an infinite value.
     * Otherwise, endpoint.toString() is used to get the endpoint's value.
     * <p>
     * Likely yields an undesirable result when wanting to treat
     * a Byte, Short, Integer, or Long T in an unsigned fashion.
     * See toStringUnsigned().
     * <p>
     * No special processing is performed to escape/encode a "." present
     * in an endpoint.toString() value.  Hence Range&lt;T&gt;.toString() for
     * a {@code T} of {@code String} (of value "." or with embedded ".."),
     * or some other non-numeric type may yield values that are not amenable
     * to parsing by {@link #valueOf(String, Function)}.
     * <br>
     * .e.g.,
     * <pre>
     * "[120..156)"  // lowerEndpoint=120 inclusive, upperEndpoint=156 exclusive
     * "[120..*)"    // an "atLeast" 120 range
     * </pre> 
     */
    @Override
    public String toString() {
        return toString(endpoint -> endpoint.toString());
    }

    /**
     * Return a String treating the endpoints as an unsigned value.
     * @return String with same form as {@link #toString()}
     * @throws IllegalArgumentException if the Range is not one of
     *         Byte, Short, Integer, Long
     */
    public String toStringUnsigned() {
        return toString(endpoint -> toUnsignedString(endpoint));
    }
    
    private String toString(Function<T,String> toStringFn) {
        // N.B. See note in classdoc wrt Guava Range behavior.
        String[] parts = { "(", "*", "*", ")" };
        if (lowerEndpoint!=null) {
            parts[0] = lbt==BoundType.CLOSED ? "[" : "(";
            parts[1] = toStringFn.apply(lowerEndpoint);
        }
        if (upperEndpoint!=null) {
            parts[2] = toStringFn.apply(upperEndpoint);
            parts[3] = ubt==BoundType.CLOSED ? "]" : ")";
        }
            
        return parts[0]+parts[1]+".."+parts[2]+parts[3];
    }
    
    private static <T> String toUnsignedString(T v) {
        if (v instanceof Byte)
            return intToUnsignedString(byteToUnsignedInt((Byte)v));
        else if (v instanceof Short)
            return intToUnsignedString(shortToUnsignedInt((Short)v));
        else if (v instanceof Integer)
            return intToUnsignedString((Integer)v);
        else if (v instanceof Long)
            return longToUnsignedString((Long)v);
        throw new IllegalArgumentException("Not Range of Byte,Short,Integer, or Long"+v.getClass());
    }

}
