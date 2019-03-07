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
package org.apache.edgent.oplet.core;

import java.util.Collections;
import java.util.List;

import org.apache.edgent.function.Consumer;

public final class FanOut<T> extends AbstractOplet<T, T> implements Consumer<T> {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private List<? extends Consumer<T>> targets;
    private int n;

    @Override
    public void start() {
    }

    @Override
    public List<? extends Consumer<T>> getInputs() {
        targets = getOpletContext().getOutputs();
        n = targets.size();
        return Collections.singletonList(this);
    }
    
    @Override
    public void accept(T tuple) {
        for (int i = 0; i < n; i++)
            targets.get(i).accept(tuple);
    }

    @Override
    public void close() {
    }
}
