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
package org.apache.edgent.javax.websocket;

import java.util.Properties;
import java.util.ServiceLoader;

import javax.websocket.WebSocketContainer;

/**
 * A {@link WebSocketContainer} provider for dealing with javax.websocket
 * SSL issues.
 * <ul>
 * <li>JSR356 lacks API for Client-side SSL configuration</li>
 * <li>Jetty's {@code javax.websocket.ContainerProvider} ignores the
 *     {@code javax.net.ssl.keyStore} system property.
 *     It correctly handles {@code javax.net.ssl.trustStore}</li>
 * </ul>
 * see https://github.com/eclipse/jetty.project/issues/155
 * <p>
 * The net is that Jetty's {@code javax.websocket.ContainerProvider}
 * works fine for the "ws" protocol and for "wss" as long as
 * one doesn't need to <b>programatically</b> specify a trustStore path
 * and one doesn't to specify a keyStore for SSL client authentication support.
 * <p>
 * A {@code EdgentSslContainerProvider} implementation is responsible for
 * working around those limitations.
 */
public abstract class EdgentSslContainerProvider {
    
    /**
     * Create a WebSocketContainer setup for SSL.
     * <p>
     * The Java {@link ServiceLoader} is used to locate an implementation
     * of {@code org.apache.edgent.javax.websocket.Edgent.SslContainerProvider}.
     * @param config  SSL configuration info as described by
     * {@code org.apache.edgent.connectors.wsclient.javax.websocket.Jsr356WebSocketClient}.
     * @return the WebSocketContainer
     * @throws RuntimeException upon failure
     */
    public static WebSocketContainer getSslWebSocketContainer(Properties config) throws RuntimeException {
        for (EdgentSslContainerProvider provider : ServiceLoader.load(EdgentSslContainerProvider.class)) {
            WebSocketContainer container = provider.getSslContainer(config);
            if (container != null)
                return container;
        }

        throw new RuntimeException("Unable to find an implementation of EdgentSslContainerProvider");
    }

    /**
     * Create a WebSocketContainer setup for SSL.
     * To be implemented by a javax.websocket client provider.
     * @param config  SSL configuration info. 
     * @return the WebSocketContainer
     * @throws RuntimeException if it fails
     */
    protected abstract WebSocketContainer getSslContainer(Properties config);
}
