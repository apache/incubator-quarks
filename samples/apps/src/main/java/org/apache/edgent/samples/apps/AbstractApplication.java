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
package org.apache.edgent.samples.apps;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.edgent.console.server.HttpServer;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.samples.apps.mqtt.AbstractMqttApplication;
import org.apache.edgent.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Application base class.
 * <p>
 * Application instances need to:
 * <ul>
 * <li>define an implementation for {@link #buildTopology(Topology)}</li>
 * <li>call {@link #run()} to build and submit the topology for execution.</li>
 * </ul>
 * <p>
 * The class provides some common processing needs:
 * <ul>
 * <li>Support for an external configuration file</li>
 * <li>Provides a {@link TopologyProviderFactory}</li>
 * <li>Provides a {@link ApplicationUtilities}</li>
 * </ul>
 * @see AbstractMqttApplication
 */
public abstract class AbstractApplication {
    
    protected final String propsPath;
    protected final Properties props;
    private final ApplicationUtilities applicationUtilities;
    private static final Logger logger = LoggerFactory.getLogger(AbstractApplication.class);

    protected Topology t;
    
    public AbstractApplication(String propsPath) throws Exception {
        this.propsPath = propsPath;
        props = new Properties();
        props.load(new FileReader(new File(propsPath)));
        applicationUtilities = new ApplicationUtilities(props);
    }
    
    /**
     * Construct and run the application's topology.
     * @throws Exception on failure
     */
    protected void run() throws Exception {
// TODO need to setup logging to squelch stderr output from the runtime/connectors, 
// including paho output

        TopologyProviderFactory tpFactory = new TopologyProviderFactory(props);
        
        DirectProvider tp = tpFactory.newProvider();
        
        // Create a topology for the application
        t = tp.newTopology(config().getProperty("application.name"));
        
        preBuildTopology(t);
        
        buildTopology(t);
        
        // Run the topology
        HttpServer httpServer = tp.getServices().getService(HttpServer.class);
        if (httpServer != null) {
            System.out.println("Edgent Console URL for the job: "
                                + httpServer.getConsoleUrl());
        }
        tp.submit(t);
    }
    
    /**
     * Get the application's raw configuration information.
     * @return the configuration
     */
    public Properties config() {
        return props;
    }
    
    /**
     * Get the application's 
     * @return the helper
     */
    public ApplicationUtilities utils() {
        return applicationUtilities;
    }

    /**
     * A hook for a subclass to do things prior to the invocation
     * of {@link #buildTopology(Topology)}.
     * <p>
     * The default implementation is a no-op.
     * @param t the application's topology
     */
    protected void preBuildTopology(Topology t) {
        return;
    }
    
    /**
     * Build the application's topology.
     * @param t Topology to add to
     */
    abstract protected void buildTopology(Topology t);
    
    public void handleRuntimeError(String msg, Exception e) {
        logger.error("A runtime error occurred", e);
    }

}
