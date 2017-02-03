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
package org.apache.edgent.samples.scenarios.iotp;


import java.io.File;

import org.apache.edgent.connectors.iotp.IotpDevice;
import org.apache.edgent.providers.iot.IotProvider;
import org.apache.edgent.samples.connectors.iotp.IotpSensors;

/**
 * Sample IotProvider scenario using IBM Watson IoT Platform.
 * <BR>
 * IotProvider with three registered applications that
 * are not started but can be started by a a remote
 * application sending device commands through
 * IBM Watson IoT Platform.
 * <P>
 * This is equivalent to the {@link IotpSensors} application
 * but executing as three separate applications using
 * {@link IotProvider} rather than the lower level
 * {@link org.apache.edgent.providers.direct.DirectProvider}.
 * 
 * </P>
 * 
 * @see org.apache.edgent.topology.mbeans.ApplicationServiceMXBean
 * @see <a href="{@docRoot}/org/apache/edgent/topology/mbeans/package-summary.html">org.apache.edgent.topology.mbeans</a>
 */
public class IotpFullScenario {
    
    /**
     * Run the IotpFullScenario application.
     * 
     * Takes a single argument that is the path to the
     * device configuration file containing the connection
     * authentication information.
     * 
     * @param args Must contain the path to the device configuration file.
     * @throws Exception on failure
     * @see IotpDevice#IotpDevice(org.apache.edgent.topology.Topology, File)
     */
    public static void main(String[] args) throws Exception {
        String deviceCfg = args[0];
        
        // Create an IotProvider that will use
        // an IotpDevice as the connectivity to
        // the IBM Watson IoT Platform message hub.
        IotProvider provider = new IotProvider(
                topology -> new IotpDevice(topology, new File(deviceCfg)));
              
        // Register three applications
        registerHeartbeat(provider);       
        registerSensors(provider);
        registerDisplay(provider);
        
        // Start this provider
        // the three applications will not start
        provider.start();
    }
    
    public static void registerHeartbeat(IotProvider provider) {
        provider.registerTopology("Heartbeat",
                (iotDevice,config) -> IotpSensors.heartBeat(iotDevice, true));
    }
    
    public static void registerSensors(IotProvider provider) {
        provider.registerTopology("Sensors",
                (iotDevice,config) -> IotpSensors.simulatedSensors(iotDevice, true));
    }
    public static void registerDisplay(IotProvider provider) {
        provider.registerTopology("Display",
                (iotDevice,config) -> IotpSensors.displayMessages(iotDevice, true));
    }

}