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
package org.apache.edgent.oplet;

import java.util.List;

import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.function.Consumer;

/**
 * Context information for the {@code Oplet}'s invocation context.
 * <P>
 * At execution time an oplet uses its invocation context to retrieve 
 * provided {@link #getService(Class) services}, 
 * {@link #getOutputs() output ports} for tuple submission
 * and {@link #getJobContext() job} information. 
 *
 * @param <I> tuple type of input streams
 * @param <O> tuple type of output streams
 */
public interface OpletContext<I, O> extends RuntimeServices {

	/**
	 * Get the unique identifier (within the running job)
	 * for this oplet.
	 * @return unique identifier for this oplet
	 */
	String getId();

    /**
     * {@inheritDoc}
     * <P>
     * Get a service for this oplet invocation.
     * 
     * An invocation of an oplet may get access to services,
     * which provide specific functionality, such as metrics.
     * </P>
     * 
     */
	@Override
    <T> T getService(Class<T> serviceClass);
    
    /**
     * Get the number of connected inputs to this oplet.
     * @return number of connected inputs to this oplet.
     */
    int getInputCount();
    
    /**
     * Get the number of connected outputs to this oplet.
     * @return number of connected outputs to this oplet.
     */
    int getOutputCount();

    /**
     * Get the mechanism to submit tuples on an output port.
     * 
     * @return list of consumers
     */
    List<? extends Consumer<O>> getOutputs();

    /**
     * Get the oplet's output port context information.
     * @return list of {@link OutputPortContext}, one for each output port.
     */
    List<OutputPortContext> getOutputContext();

    /**
     * Get the job hosting this oplet. 
     * @return {@link JobContext} hosting this oplet invocation.
     */
    JobContext getJobContext();
    
    /**
     * Creates a unique name within the context of the current runtime.
     * <p>
     * The default implementation adds a suffix composed of the package 
     * name of this interface, the current job and oplet identifiers, 
     * all separated by periods ({@code '.'}).  Developers should use this 
     * method to avoid name clashes when they store or register the name in 
     * an external container or registry.
     *
     * @param name name (possibly non-unique)
     * @return unique name within the context of the current runtime.
     */
    String uniquify(String name);
}
