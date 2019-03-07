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
package org.apache.edgent.function;

import static org.apache.edgent.function.Functions.identity;
import static org.apache.edgent.function.Functions.unpartitioned;
import static org.apache.edgent.function.Functions.zero;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FunctionsTest {
    
    int add;

    @Test
    public void testSynchronizedFunction() {
        
        Function<Integer,String> f1 = v -> Integer.toString(v);
        assertSame(f1, Functions.synchronizedFunction(f1));
        
        Function<Integer,Integer> f2 = v -> v * 2;
        assertSame(f2, Functions.synchronizedFunction(f2));
        
        int a = 7;
        Function<Integer,Integer> f3 = v -> v + a;
        assertSame(f3, Functions.synchronizedFunction(f3));
        
        int[] aa = new int[1];
        Function<Integer,Integer> f4 = v -> v + aa[0];
        assertNotSame(f4, Functions.synchronizedFunction(f4));
        
        aa[0] = 7;
        int r4 = f4.apply(2);
        assertEquals(9, r4);
        aa[0] = 13;
        r4 = f4.apply(9);
        assertEquals(22, r4);
        
        // verify a synchronized function uses the function reference
        // as the synchronization
        Function<Integer,Integer> f5 = new F5();
        add = 99;
        try {
            f5.apply(8);
            fail("Expected IllegalMonitorStateException");
        } catch (IllegalMonitorStateException e) {
            // expected
        }
        Function<Integer,Integer> f5s = Functions.synchronizedFunction(f5);
        assertNotSame(f5, f5s);
        int r5s = f5s.apply(18);
        assertEquals(117, r5s);
        
        Function<Integer,Integer> f6 = new F6();
        assertNotSame(f6, Functions.synchronizedFunction(f6));
    }
    
    class F5 implements Function<Integer,Integer> {
        private static final long serialVersionUID = 1L;

        @Override
        public Integer apply(Integer value) {
            notify();
            return add + value;
        }
    }
    static class F6 implements Function<Integer,Integer>, AutoCloseable {
        private static final long serialVersionUID = 1L;

        @Override
        public Integer apply(Integer value) {
            return value;
        }
        @Override
        public void close() throws Exception {
        }
    }
    
    @Test
    public void testSynchronizedSupplier() {
        
        Supplier<Integer> f1 = () -> 3;
        assertSame(f1, Functions.synchronizedSupplier(f1));
        
        int a = 7;
        Supplier<Integer> f2 = () -> a;
        assertSame(f2, Functions.synchronizedSupplier(f2));
        int r2 = f2.get();
        assertEquals(7, r2);

        
        int[] aa = new int[1];
        Supplier<Integer> f3 = () -> aa[0];
        assertNotSame(f3, Functions.synchronizedSupplier(f3));
        
        aa[0] = 7;
        int r3 = f3.get();
        assertEquals(7, r3);
        aa[0] = 13;
        r3 = f3.get();
        assertEquals(13, r3);
        
        // verify a synchronized function uses the function reference
        // as the synchronization
        Supplier<Integer> f4 = new S4();
        add = 127;
        try {
            f4.get();
            fail("Expected IllegalMonitorStateException");
        } catch (IllegalMonitorStateException e) {
            // expected
        }
        Supplier<Integer> f4s = Functions.synchronizedSupplier(f4);
        assertNotSame(f4, f4s);
        int r4s = f4s.get();
        assertEquals(127, r4s);
        
        Supplier<Integer> f5 = new S5();
        assertNotSame(f5,  Functions.synchronizedSupplier(f5));
    }
    class S4 implements Supplier<Integer> {
        private static final long serialVersionUID = 1L;

        @Override
        public Integer get() {
            notify();
            return add;
        }
    }
    static class S5 implements Supplier<Integer>, AutoCloseable {
        private static final long serialVersionUID = 1L;

        @Override
        public Integer get() {
            return 0;
        }
        @Override
        public void close() throws Exception {
        }
    }
    
    static void consumer(Integer x) {
    }
    
    @Test
    public void testSynchronizedConsumer() {
        
        Consumer<Integer> f1 = v -> {};
        assertSame(f1, Functions.synchronizedConsumer(f1));
        
        int a = 7;
        Consumer<Integer> f2 = v -> consumer(a + v);
        assertSame(f2, Functions.synchronizedConsumer(f2));
        
        int[] aa = new int[1];
        Consumer<Integer> f3 = v -> aa[0] = v;
        assertNotSame(f3, Functions.synchronizedConsumer(f3));
        
        aa[0] = 7;
        f3.accept(85);
        assertEquals(85, aa[0]);
        
        // verify a synchronized function uses the function reference
        // as the synchronization
        Consumer<Integer> f4 = new C4();
        add = 127;
        try {
            f4.accept(99);
            fail("Expected IllegalMonitorStateException");
        } catch (IllegalMonitorStateException e) {
            // expected
        }
        Consumer<Integer> f4s = Functions.synchronizedConsumer(f4);
        assertNotSame(f4, f4s);
        f4s.accept(2421);
        assertEquals(2421, add);
        
        Consumer<Integer> f5 = new C5();
        assertNotSame(f5,  Functions.synchronizedConsumer(f5));
    }
    class C4 implements Consumer<Integer> {

        private static final long serialVersionUID = 1L;

        @Override
        public void accept(Integer value) {
            notify();
            add = value;          
        }
        
    }
    static class C5 implements Consumer<Integer>, AutoCloseable {

        private static final long serialVersionUID = 1L;

        @Override
        public void accept(Integer value) {          
        }
        @Override
        public void close() throws Exception {
        }        
    }
    
    @Test
    public void testIdentity() {
        String s = "hello";
        assertSame(s, identity().apply(s));
        
        Integer i = 42;
        assertSame(i, identity().apply(i));
        
        Object o = new Object();
        assertSame(o, identity().apply(o));
    }
    
    @Test
    public void testZero() {
        String s = "hello";
        assertEquals(Integer.valueOf(0), zero().apply(s));
        
        Integer i = 42;
        assertEquals(Integer.valueOf(0), zero().apply(i));
        
        Object o = new Object();
        assertEquals(Integer.valueOf(0), zero().apply(o));
    }
    @Test
    public void testUnpartitioned() {
        String s = "hello";
        assertEquals(Integer.valueOf(0), unpartitioned().apply(s));
        
        Integer i = 42;
        assertEquals(Integer.valueOf(0), unpartitioned().apply(i));
        
        Object o = new Object();
        assertEquals(Integer.valueOf(0), unpartitioned().apply(o));
    }
    @Test
    public void testAlwaysTrue() {
        assertTrue(Functions.alwaysTrue().test("hello"));
        assertTrue(Functions.alwaysTrue().test(42));
        assertTrue(Functions.alwaysTrue().test(new Object()));
    }
    @Test
    public void testAlwaysFalse() {
        assertFalse(Functions.alwaysFalse().test("hello"));
        assertFalse(Functions.alwaysFalse().test(42));
        assertFalse(Functions.alwaysFalse().test(new Object()));
    }
}
