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
package org.apache.edgent.test.providers.dev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.edgent.graph.Graph;
import org.apache.edgent.graph.Vertex;
import org.apache.edgent.metrics.oplets.CounterOp;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.streamscope.oplets.StreamScope;
import org.apache.edgent.test.topology.TopologyAbstractTest;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

public class DevelopmentProviderTest extends TopologyAbstractTest implements DevelopmentTestSetup {

    // DevelopmentProvider inserts CounterOp metric oplets into the graph
    @Test
    public void testMetricsEverywhere() throws Exception {

        Topology t = newTopology();
        TStream<String> s = t.strings("a", "b", "c");

        // Condition inserts a sink
        Condition<Long> tc = t.getTester().tupleCount(s, 3);

        Graph g = t.graph();
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        
        // Two vertices before submission
        assertEquals(2, vertices.size());

        complete(t, tc);
  
        // At least three vertices after submission
        // (provide may have added other oplets as well)
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> verticesAfterSubmit = g.getVertices();
        assertTrue("size="+verticesAfterSubmit.size(), verticesAfterSubmit.size() >= 3);
        
        // There is exactly one vertex for a metric oplet
        int numOplets = 0;
        for (Vertex<? extends Oplet<?, ?>, ?, ?> v : verticesAfterSubmit) {
            Oplet<?,?> oplet = v.getInstance();
            if (oplet instanceof CounterOp) {
                numOplets++;
            }
        }
        assertEquals(1, numOplets);
    }

    // DevelopmentProvider inserts StreamScope oplets into the graph
    @Test
    public void testStreamScopesEverywhere() throws Exception {

        Topology t = newTopology();
        TStream<String> s = t.strings("a", "b", "c");
        s = s.map(tuple -> tuple)
            .filter(tuple -> true);

        // Condition inserts a sink
        Condition<Long> tc = t.getTester().tupleCount(s, 3);

        Graph g = t.graph();
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> vertices = g.getVertices();
        
        // Four vertices before submission
        assertEquals(4, vertices.size());

        complete(t, tc);
  
        // At least 4+3 vertices after submission
        // (provide may have added other oplets as well)
        Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> verticesAfterSubmit = g.getVertices();
        assertTrue("size="+verticesAfterSubmit.size(), verticesAfterSubmit.size() >= 7);
        
        // There are exactly 3 vertex for a StreamScope oplet
        int numOplets = 0;
        for (Vertex<? extends Oplet<?, ?>, ?, ?> v : verticesAfterSubmit) {
            Oplet<?,?> oplet = v.getInstance();
            if (oplet instanceof StreamScope) {
                numOplets++;
            }
        }
        assertEquals(3, numOplets);
    }
}
