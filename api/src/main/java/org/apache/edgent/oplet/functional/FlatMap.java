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
package org.apache.edgent.oplet.functional;

import static org.apache.edgent.function.Functions.closeFunction;

import org.apache.edgent.function.Function;
import org.apache.edgent.oplet.core.Pipe;

/**
 * 
 * Map an input tuple to 0-N output tuples.
 * 
 * Uses a function that returns an iterable
 * to map the input tuple. The return value
 * of the function's apply method is
 * iterated through with each returned
 * value being submitted as an output tuple.
 * 
 * 
 * @param <I>
 *            Data container type for input tuples.
 * @param <O>
 *            Data container type for output tuples.
 */
public class FlatMap<I, O> extends Pipe<I, O> {
	private static final long serialVersionUID = 1L;
	
	private Function<I, Iterable<O>> function;

    public FlatMap(Function<I, Iterable<O>> function) {
        this.function = function;
    }

    @Override
    public void accept(I tuple) {
        Iterable<O> outputs = function.apply(tuple);
        if (outputs != null) {
        	for (O output : outputs) {
        		if (output != null)
                    submit(output);
        	}
        }
    }

    @Override
    public void close() throws Exception {
        closeFunction(function);
    }
}
