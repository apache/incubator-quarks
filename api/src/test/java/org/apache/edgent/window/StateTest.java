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
package org.apache.edgent.window;

import static org.junit.Assert.assertEquals;

import org.apache.edgent.function.Supplier;
import org.apache.edgent.window.PartitionedState;
import org.junit.Test;


public class StateTest {
    
    /**
     * Test PartitionedState with immutable state.
     */
    @Test
    public void partitionedImmutableStateTest() {
        
        TestState<Integer> state = new TestState<>(() -> 73);
 
        assertEquals(73, state.getState("A").intValue());
        assertEquals(73, state.getState("B").intValue());
        
        assertEquals(73, state.removeState("A").intValue());
        // and it reverts back to the initial value.
        assertEquals(73, state.getState("A").intValue());
        
        assertEquals(73, state.setState("B", 102).intValue());
        assertEquals(102, state.getState("B").intValue());
        
        assertEquals(73, state.getState("A").intValue());
    }
    
    /**
     * Test PartitionedState with mutable state, basically
     * checking that the state does not get confused.
     */
    @Test
    public void partitionedMutableStateTest() {
        
        TestState<int[]> state = new TestState<>(() -> new int[1]);
 
        assertEquals(0, state.getState("A")[0]);
        assertEquals(0, state.getState("B")[0]);
        
        // change A, must not change B
        state.getState("A")[0] = 73;
        assertEquals(73, state.getState("A")[0]);
        assertEquals(0, state.getState("B")[0]);
        
        // change B, must not change A
        state.getState("B")[0] = 102;
        assertEquals(73, state.getState("A")[0]);
        assertEquals(102, state.getState("B")[0]);
        
        assertEquals(73, state.removeState("A")[0]);
        assertEquals(0, state.getState("A")[0]);
        
        int[] newB = new int[1];
        newB[0] = 9214;
        assertEquals(102, state.setState("B", newB)[0]);
        assertEquals(9214, state.getState("B")[0]);
    }
    
    
    private static class TestState<S> extends PartitionedState<String, S> {

        protected TestState(Supplier<S> initialState) {
            super(initialState);
        }
        @Override
        public S getState(String key) {
            return super.getState(key);
        }
        @Override
        public S removeState(String key) {
            return super.removeState(key);
        }
        @Override
        public S setState(String key, S state) {
            return super.setState(key, state);
        } 
    }
}
