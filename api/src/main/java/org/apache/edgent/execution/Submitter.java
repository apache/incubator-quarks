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
package org.apache.edgent.execution;

import java.util.concurrent.Future;

import com.google.gson.JsonObject;


/**
 * An interface for submission of an executable.
 * <p>
 * The class implementing this interface is responsible
 * for the semantics of this operation.  e.g., an direct org.apache.edgent.org.apache.edgent.topology
 * provider would run the org.apache.edgent.org.apache.edgent.topology as threads in the current jvm.
 * 
 * @param <E> the executable type
 * @param <J> the submitted executable's future
 */
public interface Submitter<E, J extends Job> {
    
    /**
     * Submit an executable.
     * No configuration options are specified,
     * this is equivalent to {@code submit(executable, new JsonObject())}.
     * 
     * @param executable executable to submit
     * @return a future for the submitted executable
     */
    Future<J> submit(E executable);
    
    /**
     * Submit an executable.
     * 
     * @param executable executable to submit
     * @param config context {@linkplain org.apache.edgent.execution.Configs information} for the submission
     * @return a future for the submitted executable
     */
    Future<J> submit(E executable, JsonObject config);
}
