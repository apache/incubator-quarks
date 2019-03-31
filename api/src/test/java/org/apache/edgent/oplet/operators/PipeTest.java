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
package org.apache.edgent.oplet.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.oplet.core.AbstractOplet;
import org.apache.edgent.oplet.core.Pipe;
import org.junit.Test;

public class PipeTest {

    @Test
    public void testHierachy() {
        assertTrue(Oplet.class.isAssignableFrom(Pipe.class));
        assertTrue(AbstractOplet.class.isAssignableFrom(Pipe.class));
    }

    @Test
    public void testDestination() {
        Pipe<String, Integer> pipe = new Pipe<String, Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void accept(String tuple) {
            }

            @Override
            public void close() throws Exception {
            };
        };

        List<? extends Consumer<String>> inputs = pipe.getInputs();
        assertEquals(1, inputs.size());
        assertSame(pipe, inputs.get(0));
    }
}
