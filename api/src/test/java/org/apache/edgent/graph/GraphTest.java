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
package org.apache.edgent.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.oplet.core.AbstractOplet;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public abstract class GraphTest extends GraphAbstractTest {

    @Test
    public void testEmptyGraph() {
        assertTrue(getGraph().getVertices().isEmpty());
    }

    @Test
    public void testGraphAccess() {
        Graph g = getGraph();
        
        TestOp<String, Integer> op = new TestOp<>();

        Vertex<TestOp<String, Integer>, String, Integer> v = g.insert(op, 1, 1);
        assertNotNull(v);
        assertSame(op, v.getInstance());

        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> c = getGraph().getVertices();
        assertNotNull(c);

        assertFalse(c.isEmpty());
        assertEquals(1, c.size());

        assertSame(v, c.toArray()[0]);
        
        assertTrue(getGraph().getEdges().isEmpty());

        try {
            c.clear();
            fail("Was able to modify org.apache.edgent.graph collection");
        } catch (UnsupportedOperationException e) {
            // ok - expected
        }
        
        
        TestOp<Integer, Void> op2 = new TestOp<>();
        Vertex<TestOp<Integer, Void>, Integer, Void> v2 = g.insert(op2, 1, 0);
        
        c = getGraph().getVertices();
        assertNotNull(c);

        assertFalse(c.isEmpty());
        assertEquals(2, c.size());

        assertSame(v, c.toArray()[0]);
        assertSame(v2, c.toArray()[1]);
        
        assertTrue(getGraph().getEdges().isEmpty());
        
        v.getConnectors().get(0).connect(v2, 0);
        
        Collection<Edge> edges = getGraph().getEdges();
        assertFalse(edges.isEmpty());
        assertEquals(1, edges.size());
        
        Edge vtov2 = (Edge) edges.toArray()[0];
        assertSame(v, vtov2.getSource());
        assertEquals(0, vtov2.getSourceOutputPort());
        
        assertSame(v2, vtov2.getTarget());
        assertEquals(0, vtov2.getTargetInputPort());
    }

    private static class TestOp<I, O> extends AbstractOplet<I, O> {

        @Override
        public void start() {
        }

        @Override
        public List<? extends Consumer<I>> getInputs() {
            return null;
        }

        @Override
        public void close() throws Exception {
        }
    }
}
