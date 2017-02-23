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

package org.apache.edgent.connectors.iotp.runtime;

import org.apache.edgent.function.Consumer;

import com.google.gson.JsonObject;

/**
 * Consumer that publishes stream tuples as IoTf GW device events.
 *
 */
public class IotpGWEventsFixed implements Consumer<JsonObject> {
    private static final long serialVersionUID = 1L;
    private final IotpGWConnector connector;
    private final String eventId;
    private final int qos;

    public IotpGWEventsFixed(IotpGWConnector connector, String eventId, int qos) {
        this.connector = connector;
        this.eventId = eventId;
        this.qos = qos;
    }

    @Override
    public void accept(JsonObject event) {
        connector.publishGWEvent(eventId, event, qos);
    }
}
