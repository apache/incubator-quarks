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

package org.apache.edgent.console.server;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.edgent.console.servlets.ConsoleJobServlet;
import org.apache.edgent.console.servlets.ConsoleMetricsServlet;
import org.apache.edgent.console.servlets.ConsoleServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The "Edgent Console".
 * <p>
 * The Console's HTTP server starts with a random available port unless
 * a port is specified via the {@code edgent.console.port} system property. 
 */
public class HttpServer {

  private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

	/**
	 * The only constructor.  A private no-argument constructor.  Called only once from the static HttpServerHolder class.
	 */
    private HttpServer() {
    }
    
    /** 
	 * The static class that creates the singleton HttpServer object.
	 */
    private static class HttpServerHolder {
        // use port 0 if system prop not set, so we know the server will always start
        private static final Tomcat TOMCATSERVER;
        static {
            TOMCATSERVER = new Tomcat();
            TOMCATSERVER.setPort(Integer.getInteger("edgent.console.port", 0));
            // Trigger the creation of the default connector
            TOMCATSERVER.getConnector();
        }

        private static Context CONSOLE_CONTEXT;
        private static final HttpServer INSTANCE = new HttpServer();
        private static boolean INITIALIZED = false;
    }

    /**
     * Gets the tomcat server associated with this class
     * @return the org.apache.catalina.startup.Tomcat
     */
    private static Tomcat getTomcatServer() {
        return HttpServerHolder.TOMCATSERVER;
    }

    /**
     * Initialization of the context path for the web application "/console" occurs in this method
     * and the handler for the web application is set.  This only occurs once.
     * @return HttpServer: the singleton instance of this class
     */
    public static HttpServer getInstance() {
        if (!HttpServerHolder.INITIALIZED) {
            logger.info("initializing");

            Tomcat tomcat = HttpServerHolder.TOMCATSERVER;

            // Initialize the console context
            Context context = tomcat.addContext("/console", new File(".").getAbsolutePath());
            HttpServerHolder.CONSOLE_CONTEXT = context;

            // Initialize the console servlet.
            Class consoleServletClass = ConsoleServlet.class;
            String consoleServletName = consoleServletClass.getSimpleName();
            Tomcat.addServlet(context, consoleServletName, consoleServletClass.getName());
            context.addServletMappingDecoded("/console/*", consoleServletName);

            // Initialize the jobs servlet.
            Class jobsServletClass = ConsoleJobServlet.class;
            String jobsServletName = jobsServletClass.getSimpleName();
            Tomcat.addServlet(context, jobsServletName, jobsServletClass.getName());
            context.addServletMappingDecoded("/console/jobs/*", jobsServletName);

            // Initialize the metrics servlet.
            Class metricsServletClass = ConsoleMetricsServlet.class;
            String metricsServletName = metricsServletClass.getSimpleName();
            Tomcat.addServlet(context, metricsServletName, metricsServletClass.getName());
            context.addServletMappingDecoded("/console/metrics/*", metricsServletName);

            HttpServerHolder.INITIALIZED = true;
        }
        return HttpServerHolder.INSTANCE;
    }

    /**
     * 
     * @return a String containing the context path to the console web application
     */
    public String getConsoleContextPath() {
        return HttpServerHolder.CONSOLE_CONTEXT.getPath();
    }

    /**
     * Starts the tomcat web server
     * @throws Exception on failure
     */
    public void startServer() throws Exception {
        getTomcatServer().start();
    }

    /**
     * Stops the tomcat web server
     * @throws Exception on failure
     */
    @SuppressWarnings("unused")
    private static void stopServer() throws Exception {
        getTomcatServer().stop();
    }

    /**
     * Checks to see if the tomcat web server is started
     * @return a boolean: true if the server is started, false if not
     */
    public boolean isServerStarted() {
        return getTomcatServer().getConnector().getState().isAvailable();
    }

    /**
     * Checks to see if the server is in a "stopping" or "stopped" state
     * @return a boolean: true if the server is stopping or stopped, false otherwise
     */
    public boolean isServerStopped() {
        return !getTomcatServer().getConnector().getState().isAvailable();
    }

    /**
     * Returns the port number the console is running on.  Each time the console is started a different port number may be returned.
     * @return an int: the port number the jetty server is listening on
     */
    public int getConsolePortNumber() {
        return getTomcatServer().getConnector().getLocalPort();
    }
    
    /**
     * Returns the url for the web application at the "console" context path.  Localhost is always assumed
     * @return the url for the web application at the "console" context path.
     */
    public String getConsoleUrl() {
        return "http://localhost" + ":" + getConsolePortNumber() + getConsoleContextPath();
    }

}
