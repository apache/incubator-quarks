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
package quarks.topology.spi.graph;

import quarks.function.Function;
import quarks.topology.TStream;
import quarks.topology.TWindow;
import quarks.topology.Topology;

public abstract class AbstractTWindow<T, K> implements TWindow<T, K> {
    private final TStream<T> feed;
    private Function<T, K> keyFunction;
    
    AbstractTWindow(TStream<T> feed, Function<T, K> keyFunction){
        this.feed = feed;
        this.keyFunction = keyFunction;
    } 
    
    @Override
    public Topology topology() {
        return feed.topology();
    }

    @Override
    public Function<T, K> getKeyFunction() {
        return keyFunction;
    }
    @Override
    public TStream<T> feeder() {
        return feed;
    }
}
