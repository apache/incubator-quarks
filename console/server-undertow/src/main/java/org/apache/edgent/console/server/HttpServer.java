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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.edgent.console.servlets.ConsoleJobServlet;
import org.apache.edgent.console.servlets.ConsoleMetricsServlet;
import org.apache.edgent.console.servlets.ConsoleServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

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
        private static String context = "/console";
        private static int port = Integer.getInteger("edgent.console.port", 0);
        // use port 0 if system prop not set, so we know the server will always start
        private static Undertow SERVER;

        private static final HttpServer INSTANCE = new HttpServer();
        private static boolean INITIALIZED = false;
    }

    /**
     * Gets the undertow server associated with this class
     * @return the io.undertow.Undertow
     */
    private static Undertow getUndertowServer() {
        return HttpServerHolder.SERVER;
    }

    /**
     * Initialization of the context path for the web application "/console" occurs in this method
     * and the handler for the web application is set.  This only occurs once.
     * @return HttpServer: the singleton instance of this class
     */
    public static HttpServer getInstance() throws Exception {
        if (!HttpServerHolder.INITIALIZED) {
            logger.info("initializing");

            DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(HttpServer.class.getClassLoader())
                .setContextPath("/console")
                .setDeploymentName("console.war")
                .addServlets(
                    Servlets.servlet(ConsoleServlet.class.getSimpleName(), ConsoleServlet.class)
                        .addMapping("/console/*"),
                    Servlets.servlet(ConsoleJobServlet.class.getSimpleName(), ConsoleJobServlet.class)
                        .addMapping("/console/jobs/*"),
                    Servlets.servlet(ConsoleMetricsServlet.class.getSimpleName(), ConsoleMetricsServlet.class)
                        .addMapping("/console/metrics/*")
                );

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
            manager.deploy();

            HttpHandler servletHandler = manager.start();
            PathHandler path = Handlers.path(Handlers.redirect(HttpServerHolder.context))
                .addPrefixPath(HttpServerHolder.context, servletHandler);

            HttpServerHolder.SERVER = Undertow.builder()
                .addHttpListener(HttpServerHolder.port, "localhost")
                .setHandler(path)
                .build();

            HttpServerHolder.INITIALIZED = true;
        }
        return HttpServerHolder.INSTANCE;
    }

    /**
     * 
     * @return a String containing the context path to the console web application
     */
    public String getConsoleContextPath() {
        return HttpServerHolder.context;
    }

    /**
     * Starts the tomcat web server
     */
    public void startServer() {
        if(!isServerStarted()) {
            getUndertowServer().start();
        }
    }

    /**
     * Stops the tomcat web server
     * @throws Exception on failure
     */
    @SuppressWarnings("unused")
    private static void stopServer() {
        getUndertowServer().stop();
    }

    /**
     * Checks to see if the tomcat web server is started
     * @return a boolean: true if the server is started, false if not
     */
    public boolean isServerStarted() {
        try {
            HttpServerHolder.SERVER.getListenerInfo();
            return true;
        } catch(IllegalStateException e) {
            if ("UT000138: Server not started".equals(e.getMessage())) {
                return false;
            } else {
                throw e;
            }
        }
    }

    /**
     * Checks to see if the server is in a "stopping" or "stopped" state
     * @return a boolean: true if the server is stopping or stopped, false otherwise
     */
    public boolean isServerStopped() {
        return !isServerStarted();
    }

    /**
     * Returns the port number the console is running on.  Each time the console is started a different port number may be returned.
     * @return an int: the port number the jetty server is listening on
     */
    public int getConsolePortNumber() {
        List<Undertow.ListenerInfo> listenerInfos = HttpServerHolder.SERVER.getListenerInfo();
        if(!listenerInfos.isEmpty()) {
            Undertow.ListenerInfo listenerInfo = listenerInfos.iterator().next();
            if(listenerInfo.getAddress() instanceof InetSocketAddress) {
                InetSocketAddress address = (InetSocketAddress) listenerInfo.getAddress();
                return address.getPort();
            }
        }
        return HttpServerHolder.port;
    }
    
    /**
     * Returns the url for the web application at the "console" context path.  Localhost is always assumed
     * @return the url for the web application at the "console" context path.
     */
    public String getConsoleUrl() {
        return "http://localhost" + ":" + getConsolePortNumber() + getConsoleContextPath();
    }

}
