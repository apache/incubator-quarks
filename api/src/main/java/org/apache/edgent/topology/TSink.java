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
package org.apache.edgent.topology;

/**
 * Termination point (sink) for a stream.
 *
 * @param <T> Tuple type
 */
public interface TSink<T> extends TopologyElement {
    /**
     * Get the stream feeding this sink.
     * The returned reference may be used for
     * further processing of the feeder stream.
     * <BR>
     * For example, {@code s.print().filter(...)}
     * <BR>
     * Here the filter is applied
     * to {@code s} so that {@code s} feeds
     * the {@code print()} and {@code filter()}.
     * 
     * @return stream feeding this sink.
     */
    public TStream<T> getFeed();
}
