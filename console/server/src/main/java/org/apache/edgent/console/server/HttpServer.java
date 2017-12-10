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

import java.io.File;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * The "Edgent Console".
 * <p>
 * The Console's HTTP server starts with a random available port unless
 * a port is specified via the {@code edgent.console.port} system property. 
 */
public class HttpServer {

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
        private static final Server JETTYSERVER = new Server(Integer.getInteger("edgent.console.port", 0));
        private static final WebAppContext WEBAPP = new WebAppContext();
        private static final HttpServer INSTANCE = new HttpServer();
        private static boolean INITIALIZED = false;
    }

    /**
     * Gets the jetty server associated with this class
     * @return the org.eclipse.jetty.server.Server
     */
    private static Server getJettyServer() {
        return HttpServerHolder.JETTYSERVER;
    }
    /**
     * Initialization of the context path for the web application "/console" occurs in this method
     * and the handler for the web application is set.  This only occurs once.
     * @return HttpServer: the singleton instance of this class
     * @throws Exception on failure
     */
    public static HttpServer getInstance() throws Exception {
        if (!HttpServerHolder.INITIALIZED) {
            HttpServerHolder.WEBAPP.setContextPath("/console");
            ServletContextHandler contextJobs = new ServletContextHandler(ServletContextHandler.SESSIONS);
            contextJobs.setContextPath("/jobs");
            ServletContextHandler contextMetrics = new ServletContextHandler(ServletContextHandler.SESSIONS);
            contextMetrics.setContextPath("/metrics");
            ServerUtil sUtil = new ServerUtil();
            File servletsJarFile = sUtil.getWarFilePath();
            if (servletsJarFile.exists()){
            	HttpServerHolder.WEBAPP.setWar(servletsJarFile.getAbsolutePath());
            } else {
                throw new RuntimeException("Unable to find WAR archive in " + servletsJarFile.getAbsolutePath());
            }

            HttpServerHolder.WEBAPP.addAliasCheck(new AllowSymLinkAliasChecker());
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.setHandlers(new Handler[] { contextJobs, contextMetrics, HttpServerHolder.WEBAPP });
            HttpServerHolder.JETTYSERVER.setHandler(contexts);
            HttpServerHolder.INITIALIZED = true;
        }
        return HttpServerHolder.INSTANCE;
    }

    /**
     * 
     * @return the ServerConnector object for the jetty server
     */
    private static ServerConnector getServerConnector() {
        return (ServerConnector) HttpServerHolder.JETTYSERVER.getConnectors()[0];
    }

    /**
     * 
     * @return a String containing the context path to the console web application
     * @throws Exception on failure
     */
    public String getConsoleContextPath() throws Exception {
        return HttpServerHolder.WEBAPP.getContextPath();
    }

    /**
     * Starts the jetty web server
     * @throws Exception on failure
     */
    public void startServer() throws Exception {
        getJettyServer().start();
    }

    /**
     * Stops the jetty web server
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private static void stopServer() throws Exception {
        getJettyServer().stop();
    }

    /**
     * Checks to see if the jetty web server is started
     * @return a boolean: true if the server is started, false if not
     */
    public boolean isServerStarted() {
        if (getJettyServer().isStarted() || getJettyServer().isStarting() || getJettyServer().isRunning()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Checks to see if the server is in a "stopping" or "stopped" state
     * @return a boolean: true if the server is stopping or stopped, false otherwise
     */
    public boolean isServerStopped() {
        if (getJettyServer().isStopping() || getJettyServer().isStopped()) {
            return true;
        }
        else {
            return false;
        }
    }
    /**
     * Returns the port number the console is running on.  Each time the console is started a different port number may be returned.
     * @return an int: the port number the jetty server is listening on
     */
    public int getConsolePortNumber() {
        return getServerConnector().getLocalPort();
    }
    
    /**
     * Returns the url for the web application at the "console" context path.  Localhost is always assumed
     * @return the url for the web application at the "console" context path.
     * @throws Exception on failure
     */
    public String getConsoleUrl() throws Exception {
        return new String("http://localhost" + ":" + getConsolePortNumber() + getConsoleContextPath());
    }
}
