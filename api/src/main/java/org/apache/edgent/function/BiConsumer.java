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
 * Consumer function that takes two arguments.
 * @param <T> Type of the first function argument
 * @param <U> Type of the second function argument
 */
public interface BiConsumer<T,U> extends Serializable {
    
    /**
     * Consume the two arguments.
     * @param t First function argument
     * @param u Second function argument
     */
    void accept(T t, U u);
}
