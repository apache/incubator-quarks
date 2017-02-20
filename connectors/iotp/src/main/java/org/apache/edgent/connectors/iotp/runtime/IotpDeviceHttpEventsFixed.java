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
 * Consumer that publishes stream tuples as IoTf device events.
 *
 */
public class IotpDeviceHttpEventsFixed implements Consumer<JsonObject> {
    private static final long serialVersionUID = 1L;
    private final IotpConnector connector;
    private final String eventId;

    public IotpDeviceHttpEventsFixed(IotpConnector connector, String eventId) {
        this.connector = connector;
        this.eventId = eventId;
    }

    @Override
    public void accept(JsonObject event) {
        connector.publishHttpEvent(eventId, event);
    }
}
