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
 * Function that supplies a value.
 * For example, functions that returns the current time:
 * <UL>
 * <LI>
 * As a lambda expression {@code () -> System.currentTimeMillis()}
 * </LI>
 * <LI>
 * As a method reference {@code () -> System::currentTimeMillis}
 * </LI>
 * </UL>
 *
 * @param <T> Type of function return.
 */
public interface Supplier<T> extends Serializable {
    /**
     * Supply a value, each call to this function may return
     * a different value.
     * @return Value supplied by this function.
     */
    T get();
}
