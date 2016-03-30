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
package quarks.runtime.appservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import quarks.execution.DirectSubmitter;
import quarks.execution.Job;
import quarks.execution.services.ControlService;
import quarks.function.BiConsumer;
import quarks.topology.Topology;
import quarks.topology.TopologyProvider;
import quarks.topology.mbeans.ApplicationServiceMXBean;
import quarks.topology.services.ApplicationService;

/**
 * Application service for a {@code TopologyProvider}.
 * <BR>
 * Applications {@link #registerTopology(String, BiConsumer) registered}
 * can be submitted through the control {@link ApplicationServiceMXBean}
 * registered with the {@link ControlService} for the topology provider.
 * <BR>
 * If a control service is not available then no control MBean is registered
 * and the application service is effectively inactive.
 *
 */
public class AppService implements ApplicationService {
    
	/**
	 * Create an register an application service using the default alias {@link ApplicationService#ALIAS}.
	 * @param provider Provider to create topology instances for registered applications.
	 * @param submitter Submitter for registered applications.
	 * @return Application service instance.
	 */
    public static ApplicationService createAndRegister(TopologyProvider provider, DirectSubmitter<Topology, Job> submitter) {
        
        AppService service = new AppService(provider, submitter, ALIAS);
        
        submitter.getServices().addService(ApplicationService.class, service);
        
        return service;
        
    }
    
    private final Map<String,BiConsumer<Topology, JsonObject>> applications =
            Collections.synchronizedMap(new HashMap<>());
    
    private final TopologyProvider provider;
    private final DirectSubmitter<Topology, Job> submitter;
    
    /**
     * Create an {@code ApplicationService} instance.
     * @param provider Provider to create topology instances for registered applications.
     * @param submitter Submitter for registered applications.
     * @param alias Alias used to register the control MBean.
     */
    public AppService(TopologyProvider provider,
            DirectSubmitter<Topology, Job> submitter, String alias) {
        this.provider = provider;
        this.submitter = submitter;
        
        ControlService cs = submitter.getServices().getService(ControlService.class);
        if (cs != null)
            cs.registerControl(ApplicationServiceMXBean.TYPE,
                    ALIAS+System.currentTimeMillis(), alias,
                    ApplicationServiceMXBean.class,
                    new AppServiceControl(this));
    }

    @Override
    public void registerTopology(String applicationName, BiConsumer<Topology, JsonObject> builder) {
        applications.put(applicationName, builder);
    }
    
    @Override
    public Set<String> getApplicationNames() {
        synchronized (applications) {
            return new HashSet<>(applications.keySet());
        }
    }   

    BiConsumer<Topology, JsonObject> getBuilder(String applicationName) {
        return applications.get(applicationName);
    }
    
    TopologyProvider getProvider() {
        return provider;
    }
    
    DirectSubmitter<Topology, Job> getSubmitter() {
        return submitter;
    }
}
