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
package org.apache.edgent.providers.direct;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.edgent.execution.Configs;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.graph.Graph;
import org.apache.edgent.runtime.etiao.Executable;
import org.apache.edgent.runtime.etiao.graph.DirectGraph;
import org.apache.edgent.topology.spi.graph.GraphTopology;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * {@code DirectTopology} is a {@link GraphTopology} that
 * is executed in threads in the current virtual machine.
 * <P> 
 * The org.apache.edgent.org.apache.edgent.topology is backed by a {@code DirectGraph} and its
 * execution is controlled and monitored by a {@code EtiaoJob}.
 * </P>
 */
public class DirectTopology extends GraphTopology<DirectTester> {

    private final DirectGraph eg;
    private final Executable executable;
    private Job job; // created at submit time

    /**
     * Creates a {@code DirectTopology} instance.
     * 
     * @param name org.apache.edgent.org.apache.edgent.topology name
     * @param container container which provides runtime services
     */
    DirectTopology(String name, ServiceContainer container) {
        super(name);

        this.eg = new DirectGraph(name, container);
        this.executable = eg.executable();
    }

    @Override
    public Graph graph() {
        return eg;
    }

    Executable getExecutable() {
        return executable;
    }

    @Override
    public Supplier<RuntimeServices> getRuntimeServiceSupplier() {
        return () -> getExecutable();
    }

    @Override
    protected DirectTester newTester() {
        return new DirectTester(this);
    }

    private Callable<Job> getCallable() {
        return new Callable<Job>() {

            @Override
            public Job call() throws Exception {
                execute();
                return job;
            }
        };
    }

    Future<Job> executeCallable(JsonObject config) {
        JsonElement value = null;
        if (config != null) 
            value = config.get(Configs.JOB_NAME);

        String jobName = null;
        if (value != null && !(value instanceof JsonNull))
            jobName = value.getAsString();

        this.job = getExecutable().createJob(graph(), getName(), jobName);
        return getExecutable().getScheduler().submit(getCallable());
    }

    private void execute() {
        job.stateChange(Job.Action.INITIALIZE);
        job.stateChange(Job.Action.START);
    }
}
