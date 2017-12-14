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
package org.apache.edgent.javax.websocket.impl;

import java.util.Properties;

import javax.websocket.WebSocketContainer;

import org.apache.edgent.javax.websocket.EdgentSslContainerProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;

public class EdgentSslContainerProviderImpl extends EdgentSslContainerProvider {
    
    public EdgentSslContainerProviderImpl() { }

    @Override
    public WebSocketContainer getSslContainer(Properties config) {
        
        // With jetty, can't directly use ContainerProvider.getWebSocketContainer()
        // as it's "too late" to inject SslContextFactory into the mix.
        
        String trustStore = config.getProperty("ws.trustStore", 
                                System.getProperty("javax.net.ssl.trustStore"));
        String trustStorePassword = config.getProperty("ws.trustStorePassword",
                                System.getProperty("javax.net.ssl.trustStorePassword"));
        String keyStore = config.getProperty("ws.keyStore", 
                                System.getProperty("javax.net.ssl.keyStore"));
        String keyStorePassword = config.getProperty("ws.keyStorePassword", 
                                System.getProperty("javax.net.ssl.keyStorePassword"));
        String keyPassword = config.getProperty("ws.keyPassword", keyStorePassword);
        String certAlias = config.getProperty("ws.keyCertificateAlias", "default");
        
        // create ClientContainer as usual
        ClientContainer container = new ClientContainer();
        
        //  tweak before starting it
        SslContextFactory scf = container.getClient().getSslContextFactory();
        if (trustStore != null) {
            // System.out.println("setting " + trustStore);
            scf.setTrustStorePath(trustStore);
            scf.setTrustStorePassword(trustStorePassword);
        }
        if (keyStore != null) {
            // System.out.println("setting " + keyStore);
            scf.setKeyStorePath(keyStore);
            scf.setKeyStorePassword(keyStorePassword);
            scf.setKeyManagerPassword(keyPassword);
            scf.setCertAlias(certAlias);
        }
        
        // start as usual
        try {
            container.start();
            return container;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to start Client Container", e);
        }
    }

}
