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
package org.apache.edgent.test.analytics.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.apache.edgent.analytics.sensors.utils.Java7Helper.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

import org.apache.edgent.analytics.sensors.Range;
import org.apache.edgent.analytics.sensors.Ranges;
import org.apache.edgent.function.Function;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Test Range and Ranges
 */
public class RangeTest {
    
    private <T extends Comparable<?>> void testContains(Range<T> range, T v, Boolean expected) {
        assertEquals("range"+range+".contains(range"+v+")", expected, range.contains(v));
    }
    
    private <T extends Comparable<?>> void testPredicate(Range<T> range, T v, Boolean expected) {
        assertEquals("range"+range+".test(range"+v+")", expected, range.test(v));
    }
    
    private <T extends Comparable<?>> void testToString(Range<T> range, String expected) {
        assertEquals("range.toString()", expected, range.toString());
    }
    
    private <T extends Comparable<?>> void testToStringUnsigned(Range<T> range, String expected) {
        assertEquals("range.toString()", expected, range.toStringUnsigned());
    }

    private <T extends Comparable<?>> void testValueOf(Function<String,Range<T>> valueOf, Range<T> expected) {
        assertEquals("Ranges.valueOf(\""+expected.toString()+"\")", 
                valueOf.apply(expected.toString()), expected);
    }
    
    private <T extends Comparable<?>> void testEquals(Range<T> r1, Range<T> r2, Boolean expected) {
        assertEquals("range"+r1+".equals(range"+r2+")", expected, r1.equals(r2));
    }
    
    private <T extends Comparable<?>> void testHashCode(Range<T> range, int hashCode, Boolean expected) {
        if (expected)
            assertEquals("range"+range+".hashCode()", hashCode, range.hashCode());
        else
            assertFalse("range"+range+".hashCode()", hashCode == range.hashCode());
    }
    
    private <T extends Comparable<?>> void testJson(Range<T> r1, Type typeOfT) {
        String json = new Gson().toJson(r1);
        Range<T> r2 = new Gson().fromJson(json, typeOfT);
        assertEquals("json="+json+" typeOfT="+typeOfT, r1, r2);
    }
    
    /*
     * Thoroughly test all aspects of Range/Ranges for Integers
     */
    
    @Test
    public void testContainsOpen() {
        testContains(Ranges.open(2,4), 1, false);
        testContains(Ranges.open(2,4), 2, false);
        testContains(Ranges.open(2,4), 3, true);
        testContains(Ranges.open(2,4), 4, false);
        testContains(Ranges.open(2,4), 5, false);
    }

    @Test
    public void testContainsClosed() {
        testContains(Ranges.closed(2,4), 1, false);
        testContains(Ranges.closed(2,4), 2, true);
        testContains(Ranges.closed(2,4), 3, true);
        testContains(Ranges.closed(2,4), 4, true);
        testContains(Ranges.closed(2,4), 5, false);
    }

    @Test
    public void testContainsOpenClosed() {
        testContains(Ranges.openClosed(2,4), 1, false);
        testContains(Ranges.openClosed(2,4), 2, false);
        testContains(Ranges.openClosed(2,4), 3, true);
        testContains(Ranges.openClosed(2,4), 4, true);
        testContains(Ranges.openClosed(2,4), 5, false);
    }

    @Test
    public void testContainsClosedOpen() {
        testContains(Ranges.closedOpen(2,4), 1, false);
        testContains(Ranges.closedOpen(2,4), 2, true);
        testContains(Ranges.closedOpen(2,4), 3, true);
        testContains(Ranges.closedOpen(2,4), 4, false);
        testContains(Ranges.closedOpen(2,4), 5, false);
    }

    @Test
    public void testContainsGreaterThan() {
        testContains(Ranges.greaterThan(2), 1, false);
        testContains(Ranges.greaterThan(2), 2, false);
        testContains(Ranges.greaterThan(2), 3, true);
    }

    @Test
    public void testContainsAtLeast() {
        testContains(Ranges.atLeast(2), 1, false);
        testContains(Ranges.atLeast(2), 2, true);
        testContains(Ranges.atLeast(2), 3, true);
    }

    @Test
    public void testContainsLessThan() {
        testContains(Ranges.lessThan(2), 1, true);
        testContains(Ranges.lessThan(2), 2, false);
        testContains(Ranges.lessThan(2), 3, false);
    }

    @Test
    public void testContainsAtMost() {
        testContains(Ranges.atMost(2), 1, true);
        testContains(Ranges.atMost(2), 2, true);
        testContains(Ranges.atMost(2), 3, false);
    }

    @Test
    public void testContainsSingleton() {
        testContains(Ranges.singleton(2), 1, false);
        testContains(Ranges.singleton(2), 2, true);
        testContains(Ranges.singleton(2), 3, false);
    }
    
    @Test
    public void testPredicate() {
        testPredicate(Ranges.closed(2,4), 1, false);
        testPredicate(Ranges.closed(2,4), 2, true);
        testPredicate(Ranges.closed(2,4), 3, true);
        testPredicate(Ranges.closed(2,4), 4, true);
        testPredicate(Ranges.closed(2,4), 5, false);
    }

    @Test
    public void testEquals() {
        testEquals(Ranges.closed(2,4), Ranges.closed(2,4), true);
        testEquals(Ranges.closed(2,4), Ranges.closed(2,3), false);
        testEquals(Ranges.closed(3,4), Ranges.closed(2,4), false);
        testEquals(Ranges.atMost(2), Ranges.atMost(2), true);
        testEquals(Ranges.atMost(2), Ranges.atMost(3), false);
        testEquals(Ranges.atLeast(2), Ranges.atLeast(2), true);
        testEquals(Ranges.atLeast(2), Ranges.atLeast(3), false);
        testEquals(Ranges.closed(2,2), Ranges.singleton(2), true);
    }

    @Test
    public void testHashCode() {
        testHashCode(Ranges.atMost(2), Ranges.atMost(2).hashCode(), true);
        testHashCode(Ranges.atMost(2), 0, false);
        testHashCode(Ranges.atMost(2), Ranges.atMost(3).hashCode(), false);
        testHashCode(Ranges.atLeast(2), Ranges.atMost(2).hashCode(), false);
    }
    
    @Test
    public void testEndpointAccess() {
        // {lower,upper}Endpoint(),
        // has{Lower,Upper}Endpoint()
        // {lower,upper}BoundType()
        Range<Integer> r1 = Ranges.openClosed(2,4);
        assertEquals(Range.BoundType.OPEN, r1.lowerBoundType());
        assertTrue(r1.hasLowerEndpoint());
        assertEquals(2, r1.lowerEndpoint().intValue());
        assertTrue(r1.hasUpperEndpoint());
        assertEquals(4, r1.upperEndpoint().intValue());
        assertEquals(Range.BoundType.CLOSED, r1.upperBoundType());

        Range<Integer> r2 = Ranges.openClosed(2,4);
        assertNotSame(r1, r2);
        
        Range<Integer> r3 = Ranges.atMost(2);
        assertFalse(r3.hasLowerEndpoint());
        try {
            r3.lowerEndpoint();
            assertTrue(false);
        }
        catch (IllegalStateException e) {
            // expected
        }
        
        r3 = Ranges.atLeast(2);
        assertFalse(r3.hasUpperEndpoint());
        try {
            r3.upperEndpoint();
            assertTrue(false);
        }
        catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testToString() {
        testToString(Ranges.open(2,4), "(2..4)");
        testToString(Ranges.closed(2,4), "[2..4]");
        testToString(Ranges.openClosed(2,4), "(2..4]");
        testToString(Ranges.closedOpen(2,4), "[2..4)");
        testToString(Ranges.greaterThan(2), "(2..*)");
        testToString(Ranges.atLeast(2), "[2..*)");
        testToString(Ranges.lessThan(2), "(*..2)");
        testToString(Ranges.atMost(2), "(*..2]");
    }

    @Test
    public void testValueOf() {
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.open(2, 4));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.closed(2, 4));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.openClosed(2, 4));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.closedOpen(2, 4));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.greaterThan(2));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.atLeast(2));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.lessThan(2));
        testValueOf(s -> Ranges.valueOfInteger(s), Ranges.atMost(2));
    }

    
    /*
     * Thoroughly test contains() and valueOf() for other types of T
     */

    @Test
    public void testContainsOtherByte() {
        testContains(Ranges.open((byte)2,(byte)4), (byte)1, false);
        testContains(Ranges.open((byte)2,(byte)4), (byte)2, false);
        testContains(Ranges.open((byte)2,(byte)4), (byte)3, true);
        testContains(Ranges.open((byte)2,(byte)4), (byte)4, false);
        testContains(Ranges.open((byte)2,(byte)4), (byte)5, false);
    }

    @Test
    public void testContainsOtherShort() {
        testContains(Ranges.open((short)2,(short)4), (short)1, false);
        testContains(Ranges.open((short)2,(short)4), (short)2, false);
        testContains(Ranges.open((short)2,(short)4), (short)3, true);
        testContains(Ranges.open((short)2,(short)4), (short)4, false);
        testContains(Ranges.open((short)2,(short)4), (short)5, false);
    }

    @Test
    public void testContainsOtherLong() {
        testContains(Ranges.open(2L,4L), 1L, false);
        testContains(Ranges.open(2L,4L), 2L, false);
        testContains(Ranges.open(2L,4L), 3L, true);
        testContains(Ranges.open(2L,4L), 4L, false);
        testContains(Ranges.open(2L,4L), 5L, false);
    }

    @Test
    public void testContainsOtherFloat() {
        testContains(Ranges.open(2f,4f), 1f, false);
        testContains(Ranges.open(2f,4f), 2f, false);
        testContains(Ranges.open(2f,4f), 2.001f, true);
        testContains(Ranges.open(2f,4f), 3.999f, true);
        testContains(Ranges.open(2f,4f), 4f, false);
        testContains(Ranges.open(2f,4f), 5f, false);
    }

    @Test
    public void testContainsOtherDouble() {
        testContains(Ranges.open(2d,4d), 1d, false);
        testContains(Ranges.open(2d,4d), 2d, false);
        testContains(Ranges.open(2d,4d), 2.001d, true);
        testContains(Ranges.open(2d,4d), 3.999d, true);
        testContains(Ranges.open(2d,4d), 4d, false);
        testContains(Ranges.open(2d,4d), 5d, false);
    }

    @Test
    public void testContainsOtherBigInteger() {
        testContains(Ranges.open(BigInteger.valueOf(2),BigInteger.valueOf(4)), BigInteger.valueOf(1), false);
        testContains(Ranges.open(BigInteger.valueOf(2),BigInteger.valueOf(4)), BigInteger.valueOf(2), false);
        testContains(Ranges.open(BigInteger.valueOf(2),BigInteger.valueOf(4)), BigInteger.valueOf(3), true);
        testContains(Ranges.open(BigInteger.valueOf(2),BigInteger.valueOf(4)), BigInteger.valueOf(4), false);
        testContains(Ranges.open(BigInteger.valueOf(2),BigInteger.valueOf(4)), BigInteger.valueOf(5), false);
    }

    @Test
    public void testContainsOtherBigDecimal() {
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(1), false);
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(2), false);
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(2.001), true);
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(3.999), true);
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(4), false);
        testContains(Ranges.open(new BigDecimal(2),new BigDecimal(4)), new BigDecimal(5), false);
    }

    @Test
    public void testContainsOtherString() {
        testContains(Ranges.open("b","d"), "a", false);
        testContains(Ranges.open("b","d"), "b", false);
        testContains(Ranges.open("b","d"), "bc", true);
        testContains(Ranges.open("b","d"), "c", true);
        testContains(Ranges.open("b","d"), "cd", true);
        testContains(Ranges.open("b","d"), "d", false);
        testContains(Ranges.open("b","d"), "de", false);
        testContains(Ranges.open("b","d"), "e", false);
    }

    @Test
    public void testContainsOtherCharacter() {
        testContains(Ranges.open('b','d'), 'a', false);
        testContains(Ranges.open('b','d'), 'b', false);
        testContains(Ranges.open('b','d'), 'c', true);
        testContains(Ranges.open('b','d'), 'd', false);
        testContains(Ranges.open('b','d'), 'e', false);
    }

    @Test
    public void testValueOfOtherT() {
        testValueOf(s -> Ranges.valueOfShort(s), Ranges.open((short)2, (short)4));
        testValueOf(s -> Ranges.valueOfByte(s), Ranges.open((byte)2, (byte)4));
        testValueOf(s -> Ranges.valueOfLong(s), Ranges.open(2L, 4L));
        testValueOf(s -> Ranges.valueOfFloat(s), Ranges.open(2.128f, 4.25f));
        testValueOf(s -> Ranges.valueOfDouble(s), Ranges.open(2.128d, 4.25d));
        testValueOf(s -> Ranges.valueOfBigInteger(s), Ranges.open(BigInteger.valueOf(2), BigInteger.valueOf(4)));
        testValueOf(s -> Ranges.valueOfBigDecimal(s), Ranges.open(new BigDecimal(2.5), new BigDecimal(4.25)));
        testValueOf(s -> Ranges.valueOfString(s), Ranges.open("ab", "fg"));
        testValueOf(s -> Ranges.valueOfString(s), Ranges.open("ab", "."));
        testValueOf(s -> Ranges.valueOfString(s), Ranges.open(".", "ab"));
        testValueOf(s -> Ranges.valueOfCharacter(s), Ranges.open('a', 'f'));
        testValueOf(s -> Ranges.valueOfCharacter(s), Ranges.open('a', '.'));
        testValueOf(s -> Ranges.valueOfCharacter(s), Ranges.open('.', '.'));
        testValueOf(s -> Ranges.valueOfCharacter(s), Ranges.open('.', 'f'));

        // problem cases Range<String> with embedded ".."
        try { 
            testValueOf(s -> Ranges.valueOfString(s), Ranges.open("ab..c", "fg"));
            assertTrue(false);
        } catch (IllegalArgumentException e) { /* expected */ }
        try { 
            testValueOf(s -> Ranges.valueOfString(s), Ranges.open("ab", "fg..h"));
            assertTrue(false);
        } catch (IllegalArgumentException e) { /* expected */ }
        try { 
            testValueOf(s -> Ranges.valueOfString(s), Ranges.open("ab..c", "fg..h"));
            assertTrue(false);
        } catch (IllegalArgumentException e) { /* expected */ }
    }
    
    /*
     * Test unsigned handling.
     * toUnsignedString() and compare(T, Comparator<T>)
     */
    @Test
    public void testToUnsignedString() {
        testToStringUnsigned(Ranges.open((byte)0,(byte)255), "(0..255)");
        testToStringUnsigned(Ranges.closed((byte)0,(byte)255), "[0..255]");
        testToStringUnsigned(Ranges.open((short)0,(short)0xFFFF), "(0..65535)");
        testToStringUnsigned(Ranges.open(0,0xFFFFFFFF), "(0.."+ intToUnsignedString(0xFFFFFFFF)+")");
        testToStringUnsigned(Ranges.open(0L,0xFFFFFFFFFFFFFFFFL), "(0.."+longToUnsignedString(0xFFFFFFFFFFFFFFFFL)+")");
    }

    @Test
    public void testContainsUnsigned() {
        // Unsigned Byte ======================
        Comparator<Byte> unsignedByteComparator = new Comparator<Byte>() {
            public int compare(Byte v1, Byte v2) {
                return intCompareUnsigned(byteToUnsignedInt(v1), byteToUnsignedInt(v2));
            }
            public boolean equals(Object o2) { return o2==this; }
            };
            
        Range<Byte> byteRange = Ranges.closed((byte)5, (byte)255); // intend unsigned
        assertFalse(byteRange.contains((byte)6));  // not <= -1
        assertTrue(byteRange.contains((byte)6, unsignedByteComparator));
        assertFalse(byteRange.contains((byte)0xF0)); // not >= 5
        assertTrue(byteRange.contains((byte)0xF0, unsignedByteComparator));
        
        // Unsigned Short ======================
        Comparator<Short> unsignedShortComparator = new Comparator<Short>() {
            public int compare(Short v1, Short v2) {
                return intCompareUnsigned(shortToUnsignedInt(v1), shortToUnsignedInt(v2));
            }
            public boolean equals(Object o2) { return o2==this; }
            };
        Range<Short> shortRange = Ranges.closed((short)5, (short)0xFFFF); // intend unsigned
        assertFalse(shortRange.contains((short)6));  // not <= -1
        assertTrue(shortRange.contains((short)6, unsignedShortComparator));
        assertFalse(shortRange.contains((short)0xFFF0)); // not >= 5
        assertTrue(shortRange.contains((short)0xFFF0, unsignedShortComparator));
        
        // Unsigned Integer ======================
        Comparator<Integer> unsignedIntegerComparator = new Comparator<Integer>() {
            public int compare(Integer v1, Integer v2) {
                return intCompareUnsigned(v1, v2);
            }
            public boolean equals(Object o2) { return o2==this; }
            };
        Range<Integer> intRange = Ranges.closed(5, 0xFFFFFFFF); // intend unsigned
        assertFalse(intRange.contains(6));  // not <= -1
        assertTrue(intRange.contains(6, unsignedIntegerComparator));
        assertFalse(intRange.contains(0xFFFFFFF0)); // not >= 5
        assertTrue(intRange.contains(0xFFFFFFF0, unsignedIntegerComparator));
        
        // Unsigned Long ======================
        Comparator<Long> unsignedLongComparator = new Comparator<Long>() {
            public int compare(Long v1, Long v2) {
                return longCompareUnsigned(v1, v2);
            }
            public boolean equals(Object o2) { return o2==this; }
            };
        Range<Long> longRange = Ranges.closed(5L, 0xFFFFFFFFFFFFFFFFL); // intend unsigned
        assertFalse(longRange.contains(6L));  // not <= -1
        assertTrue(longRange.contains(6L, unsignedLongComparator));
        assertFalse(longRange.contains(0xFFFFFFFFFFFFFFF0L));  // not >= 5
        assertTrue(longRange.contains(0xFFFFFFFFFFFFFFF0L, unsignedLongComparator));
    }

    @Test
    public void testJsonAllTypes() {
        // json = new Gson().toJson(Range<T>);
        // range = new Gson().fromJson(json, typeOfT);
        testJson(Ranges.closed(1, 10), new TypeToken<Range<Integer>>(){}.getType());
        testJson(Ranges.closed((short)1, (short)10), new TypeToken<Range<Short>>(){}.getType());
        testJson(Ranges.closed((byte)1, (byte)10), new TypeToken<Range<Byte>>(){}.getType());
        testJson(Ranges.closed(1L, 10L), new TypeToken<Range<Long>>(){}.getType());
        testJson(Ranges.closed(1f, 10f), new TypeToken<Range<Float>>(){}.getType());
        testJson(Ranges.closed(1d, 10d), new TypeToken<Range<Double>>(){}.getType());
        testJson(Ranges.closed(BigInteger.valueOf(1), BigInteger.valueOf(10)), new TypeToken<Range<BigInteger>>(){}.getType());
        testJson(Ranges.closed(BigDecimal.valueOf(1), BigDecimal.valueOf(10)), new TypeToken<Range<BigDecimal>>(){}.getType());
        testJson(Ranges.closed("ab", "fg"), new TypeToken<Range<String>>(){}.getType());
        testJson(Ranges.closed("ab..c", "fg"), new TypeToken<Range<String>>(){}.getType());
        testJson(Ranges.closed('a', 'f'), new TypeToken<Range<Character>>(){}.getType());
        testJson(Ranges.closed('.', 'f'), new TypeToken<Range<Character>>(){}.getType());
        testJson(Ranges.closed('.', '.'), new TypeToken<Range<Character>>(){}.getType());
        testJson(Ranges.closed('a', '.'), new TypeToken<Range<Character>>(){}.getType());
    }

}
