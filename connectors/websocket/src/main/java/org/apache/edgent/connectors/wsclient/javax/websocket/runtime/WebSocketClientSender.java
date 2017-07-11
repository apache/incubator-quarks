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

public class WebSocketClientSender<T> implements Consumer<T>, AutoCloseable {
    private static final long serialVersionUID = 1L;
    protected final WebSocketClientConnector connector;
    protected final Function<T,String> toPayload;
    
    public WebSocketClientSender(WebSocketClientConnector connector, Function<T,String> toPayload) {
        this.connector = connector;
        this.toPayload = toPayload;
    }

    @Override
    public void accept(T value) {
        connector.sendText(toPayload.apply(value));
    }

    @Override
    public void close() throws Exception {
        connector.close();
    }

}
