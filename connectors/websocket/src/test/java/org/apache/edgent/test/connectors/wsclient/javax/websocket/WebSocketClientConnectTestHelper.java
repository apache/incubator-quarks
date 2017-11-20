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
package org.apache.edgent.test.connectors.wsclient.javax.websocket;

import java.net.URI;
import java.util.Properties;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.util.component.LifeCycle;

@ClientEndpoint 
public class WebSocketClientConnectTestHelper {
  
  @OnError
  public void onError(Session client, Throwable t) {
    System.err.println("Unable to connect to WebSocket server: "+t.getMessage());
  }

  public static void connectToServer(Properties config) throws Exception {
    // Verify we can create a real websocket connection to the server.
    //
    // We do the following instead of a simple socket connect
    // because in at least one location, the websocket connect/upgrade
    // fails with: expecting 101 got 403 (Forbidden).
    // There's something about that location that's not
    // allowing a websocket to be created to the (public) server.
    // Everything works fine from other locations.
    //
    String wsUri = config.getProperty("ws.uri");
    URI uri = new URI(wsUri);
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    try {
      Session session = container.connectToServer(WebSocketClientConnectTestHelper.class,  uri);
      session.close();
    }
    finally {
      if (container instanceof LifeCycle) {
        ((LifeCycle)container).stop();
      }
    }
  }

}