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
package org.apache.edgent.samples.apps.fleetManagement;

import org.apache.edgent.samples.apps.AbstractIotpApplication;
import org.apache.edgent.topology.Topology;

/**
 * A Global Positional System and On-Board Diagnostics application to perform
 * analytics defined in {@link GpsAnalyticsApplication} and
 * {@link ObdAnalyticsApplication}.
 * <p>
 * The Edgent console URL is written to the console and to file consoleUrl.txt.
 * <p>
 * The Watson IotF URL is written to the console and to file iotfUrl.txt
 * 
 * <p>
 * Argument: specify pathname to application properties file. If running in
 * Eclipse, you can specify GpsObdAnalyticsApplication.properties.
 */
public class FleetManagementAnalyticsClientApplication extends AbstractIotpApplication {

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            throw new Exception("missing pathname to application properties file");

        FleetManagementAnalyticsClientApplication application = new FleetManagementAnalyticsClientApplication(args[0]);

        application.run();
    }

    /**
     * Create an application instance.
     * 
     * @param propsPath
     *            pathname to an application configuration file
     * @throws Exception
     */
    FleetManagementAnalyticsClientApplication(String propsPath) throws Exception {
        super(propsPath);
    }

    @Override
    protected void buildTopology(Topology t) {

        // Add the GPS analytics to the topology
        new GpsAnalyticsApplication(t, this).addAnalytics();

        // TODO Add the OBD analytics to the topology
        // new ObdAnalyticsApplication(t, this).addAnalytics();
    }
}