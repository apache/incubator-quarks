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
package org.apache.edgent.connectors.wsclient.javax.websocket.runtime;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;

public class WebSocketClientReceiver<T> implements Consumer<Consumer<T>>, AutoCloseable {
    private static final long serialVersionUID = 1L;
    protected final WebSocketClientConnector connector;
    private final Function<String,T> toTuple;
    protected Consumer<T> eventHandler;
    
    public WebSocketClientReceiver(WebSocketClientConnector connector, Function<String,T> toTuple) {
        this.connector = connector;
        this.toTuple = toTuple;
    }

    @Override
    public void accept(Consumer<T> eventHandler) {
        this.eventHandler = eventHandler;
        connector.setReceiver(this);
        try {
            connector.client();  // induce connecting.
        } catch (Exception e) {
            connector.getLogger().error("{} receiver setup failed", connector.id(), e);
        }
    }
    
    void onBinaryMessage(byte[] message) {
        connector.getLogger().debug("{} ignoring received binary message (expecting text)", connector.id());
    }
    
    void onTextMessage(String message) {
        eventHandler.accept(toTuple.apply(message));
    }

    @Override
    public void close() throws Exception {
        connector.close();
    }

}
