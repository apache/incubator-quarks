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
package org.apache.edgent.function;

import java.io.Serializable;

/**
 * A wrapped function. 
 *
 * @param <F> Type of function being wrapped.
 */
public abstract class WrappedFunction<F> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final F f;
    
    protected WrappedFunction(F f) {
        this.f = f;
    }
    
    /**
     * Function that was wrapped.
     * @return Function that was wrapped.
     */
    public final F f() { return f; }
    
    /**
     * Unwrap to find the outermost function that is an instance of {@code clazz}.
     * @param <C> Type of class.
     * @param clazz Implementation class to search for.
     * @return outermost function that is an instance of {@code clazz},
     * {@code null} if {@code clazz} is not implemented by {@code this}
     * or any function it wraps.
     * 
     * @see WrappedFunction#unwrap(Class, Object)
     */
    public <C> C unwrap(Class<C> clazz) {
        return unwrap(clazz, this);
    }
    
    /**
     * Unwrap a function object to find the outermost function that implements {@code clazz}.
     * If a function object is not an instance of {@code clazz} but is an instance of
     * {@code WrappedFunction} then the test is repeated on the value of {@link #f()}.
     *  
     * @param <C> Type of class.
     * @param clazz Implementation class to search for.
     * @param wf Function to unwrap
     * @return outermost function that implements {@code clazz}, {@code null} if
     * if {@code clazz} is not implemented by {@code wf} or any function it wraps.
     */
    public static <C> C unwrap(Class<C> clazz, Object wf) {
        
        for (;;) {
            if (clazz.isInstance(wf))
                return clazz.cast(wf);
            if (wf instanceof WrappedFunction)
                wf = ((WrappedFunction<?>) wf).f();
            else
                return null;              
        }
    }
}
