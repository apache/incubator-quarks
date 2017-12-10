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
package org.apache.edgent.providers.development;

import java.util.Hashtable;
import java.util.concurrent.Future;

import org.apache.edgent.console.server.HttpServer;
import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.metrics.Metrics;
import org.apache.edgent.metrics.MetricsSetup;
import org.apache.edgent.metrics.oplets.CounterOp;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.runtime.jmxcontrol.JMXControlService;
import org.apache.edgent.streamscope.StreamScopeRegistry;
import org.apache.edgent.streamscope.StreamScopeSetup;
import org.apache.edgent.streamscope.mbeans.StreamScopeRegistryMXBean;
import org.apache.edgent.topology.Topology;

import com.codahale.metrics.MetricRegistry;
import com.google.gson.JsonObject;

/**
 * Provider intended for development.
 * This provider executes topologies using {@code DirectProvider}
 * and extends it by:
 * <UL>
 * <LI>
 * starting an embedded web-server providing the Edgent development console
 * that shows live graphs for running applications.  
 * The console {@link HttpServer} is registered as a service with
 * the provider and is accessible via {@link #getServices()}.
 * The console's HTTP server starts with a random available port unless
 * a port is specified via the {@code edgent.console.port} system property.
 * </LI>
 * <LI>
 * Creating a metrics registry with metrics registered
 * in the platform MBean server.
 * </LI>
 * <LI>
 * Add a {@link ControlService} that registers control management
 * beans in the platform MBean server.
 * </LI>
 * <LI>
 * Add tuple count metrics on all the streams before submitting a topology.
 * The implementation calls {@link Metrics#counter(Topology)} to insert 
 * {@link CounterOp} oplets into each stream.
 * </LI>
 * <LI>
 * Instrument the topology adding {@link org.apache.edgent.streamscope.oplets.StreamScope StreamScope}
 * oplets on all the streams before submitting a topology.  
 * See {@link StreamScopeSetup#addStreamScopes(Topology) StreamScopeSetup.addStreamscopes}.
 * </LI>
 * <LI>
 * Add a {@link StreamScopeRegistry} runtime service and a
 * {@link StreamScopeRegistryMXBean} management bean to the {@code ControlService}.
 * See {@link StreamScopeSetup#register(org.apache.edgent.execution.services.ServiceContainer) StreamScopeSetup.register}.
 * </LI>
 * </UL>
 * @see StreamScopeRegistry
 */
public class DevelopmentProvider extends DirectProvider {
    
    /**
     * JMX domains that this provider uses to register MBeans.
     * Set to {@value}.
     */
    public static final String JMX_DOMAIN = "org.apache.edgent.providers.development";
    
    public DevelopmentProvider() throws Exception {
        
        MetricsSetup.withRegistry(getServices(), new MetricRegistry()).
                startJMXReporter(JMX_DOMAIN);
        
        getServices().addService(ControlService.class,
                new JMXControlService(JMX_DOMAIN, new Hashtable<>()));
        
        StreamScopeSetup.register(getServices());

        HttpServer server = HttpServer.getInstance();
        getServices().addService(HttpServer.class, server);   
        server.startServer();
    }

    @Override
    public Future<Job> submit(Topology topology, JsonObject config) {
        Metrics.counter(topology);
        StreamScopeSetup.addStreamScopes(topology);
        return super.submit(topology, config);
    }

}
