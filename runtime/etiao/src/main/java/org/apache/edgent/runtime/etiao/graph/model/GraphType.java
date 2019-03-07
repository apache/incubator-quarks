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
package org.apache.edgent.runtime.etiao.graph.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.apache.edgent.graph.Edge;
import org.apache.edgent.graph.Graph;
import org.apache.edgent.graph.Vertex;
import org.apache.edgent.oplet.Oplet;

/**
 * A generic directed org.apache.edgent.graph of vertices, connectors and edges.
 * <p>
 * The org.apache.edgent.graph consists of {@link VertexType} objects, each having
 * 0 or more input and/or output {@link EdgeType} objects.
 * {@link EdgeType} objects connect an output connector to
 * an input connector.
 * <p>
 * A vertex has an associated {@link Oplet}.
 */
public class GraphType {
    /**
     * List of all vertices in this org.apache.edgent.graph.
     */
    private final List<VertexType<?,?>> vertices;

    /**
     * List of all edges in this org.apache.edgent.graph.
     */
    private final List<EdgeType> edges;

    /**
     * Create an instance of {@link GraphType}.
     * @param graph the associated Graph
     */
    public GraphType(Graph graph) {
        this(graph, null);
    }

    /**
     * Create an instance of {@link GraphType} using the specified 
     * {@link IdMapper} to generate unique object identifiers.
     * @param g the associated Graph
     * @param ids the id mapper
     */
    public GraphType(Graph g, IdMapper<String> ids) {
        if (ids == null) {
            ids = new GraphType.Mapper();
        }
        ArrayList<VertexType<?,?>> vertices = 
                new ArrayList<VertexType<?,?>>();
        ArrayList<EdgeType> edges = new ArrayList<EdgeType>();
        
        for (Vertex<? extends Oplet<?,?>, ?, ?> v : g.getVertices()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            VertexType<?,?> vertex = new VertexType(v, ids);
            vertices.add(vertex);
        }

        for (Edge e : g.getEdges()) {
            edges.add(new EdgeType(e, ids));
        }

        this.vertices = vertices;
        this.edges = edges;
    }

    /**
     * Default constructor of {@link GraphType}.
     */
    public GraphType() {
        this.vertices = null;
        this.edges = null;
    }

    public List<VertexType<?,?>> getVertices() {
        return vertices;
    }

    public List<EdgeType> getEdges() {
        return edges;
    }

    static class Mapper implements IdMapper<String> {
        private int lastId = 0;
        // Map using reference-equality in place of object-equality.
        private IdentityHashMap<Object,String> ids = new IdentityHashMap<Object,String>();
        
        @Override
        public String add(Object o) {
            if (o == null)
                throw new NullPointerException();
    
            synchronized (ids) {
                String id = ids.get(o);
                if (id == null) {
                    id = String.valueOf(lastId++);
                    ids.put(o, id);
                }
                return id;
            }
        }
    
        @Override
        public String getId(Object o) {
            if (o == null)
                throw new NullPointerException();
    
            synchronized (ids) {
                return ids.get(o);
            }
        }

        @Override
        public String add(Object o, String id) {
            if (o == null || id == null)
                throw new NullPointerException();
            
            synchronized (ids) {
                if (ids.containsKey(o)) {
                    throw new IllegalStateException();
                }
                else {
                    ids.put(o, id);
                }
                return id;
            }
        }
    }
}
