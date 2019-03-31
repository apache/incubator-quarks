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

import java.util.Set;

/**
 * Represents an edge between two Vertices.
 */
public interface Edge {
    
    /**
     * Returns the source vertex.
     * @return the source vertex.
     */
	Vertex<?, ?, ?> getSource();

    /**
     * Returns the source output port index.
     * @return the source output port index.
     */
	int getSourceOutputPort();
	
    /**
     * Returns the target vertex.
     * @return the target vertex.
     */
	Vertex<?, ?, ?> getTarget();

    /**
     * Returns the target input port index.
     * @return the target input port index.
     */
	int getTargetInputPort();
	
    /**
     * Returns the set of tags associated with this edge.
     * 
     * @return set of tag values.
     */
    Set<String> getTags(); 
    
    /**
     * Returns the alias associated with this edge.
     * 
     * @return the alias. null if none.
     */
    String getAlias(); 
}
