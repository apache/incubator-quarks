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

import org.apache.edgent.graph.Vertex;
import org.apache.edgent.oplet.Oplet;
import org.apache.edgent.runtime.etiao.graph.ExecutableVertex;

/**
 * A {@code VertexType} in a org.apache.edgent.graph.
 * <p>
 * A {@code VertexType} has an {@link InvocationType} instance.
 * 
 * @param <I> Data type the oplet consumes on its input ports.
 * @param <O> Data type the oplet produces on its output ports.
 */
public class VertexType<I, O> {

    /**
     * Vertex identifier, unique within the {@code GraphType} this vertex 
     * belongs to.
     */
    private final String id;

    /**
     * The oplet invocation that is being executed.
     */
    private final InvocationType<I, O> invocation;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public VertexType(Vertex<? extends Oplet<?, ?>, ?, ?> value, IdMapper<String> ids) {
        this.id = (value instanceof ExecutableVertex) ?
            ids.add(value, ((ExecutableVertex) value).getInvocationId()) :
            // Can't get an id from the vertex, generate unique value
            ids.add(value);
        this.invocation = new InvocationType(value.getInstance());
    }

    public VertexType() {
        this.id = null;
        this.invocation = null;
    }

    public String getId() {
        return id;
    }

    public InvocationType<I, O> getInvocation() {
        return invocation;
    }
}
