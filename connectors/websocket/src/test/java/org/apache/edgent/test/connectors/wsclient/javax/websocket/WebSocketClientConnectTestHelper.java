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