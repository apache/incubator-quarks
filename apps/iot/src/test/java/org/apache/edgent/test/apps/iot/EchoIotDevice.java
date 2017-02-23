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

package org.apache.edgent.test.apps.iot;

import static org.apache.edgent.function.Functions.discard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;

import com.google.gson.JsonObject;

/**
 * A test IotDevice that echos back every event as a command with command
 * identifier equal to the {@code cmdId} value in the event payload. If {@code cmdId}
 * is not set then the event identifier is used.
 *
 */
public class EchoIotDevice implements IotDevice {
    
    public static final String EVENT_CMD_ID = "cmdId";

    private final Topology topology;
    private TStream<JsonObject> echoCmds;

    public EchoIotDevice(Topology topology) {
        this.topology = topology;
    }

    @Override
    public Topology topology() {
        return topology;
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, Function<JsonObject, String> eventId,
            UnaryOperator<JsonObject> payload, Function<JsonObject, Integer> qos) {
        
        stream = stream.map(e -> {
            JsonObject c = new JsonObject();
            JsonObject evPayload = payload.apply(e);
            c.addProperty(CMD_ID, getCommandIdFromEvent(eventId.apply(e), evPayload));
            c.add(CMD_PAYLOAD, evPayload);
            c.addProperty(CMD_FORMAT, "json");
            c.addProperty(CMD_TS, System.currentTimeMillis());
            return c;
        });
        
        return handleEvents(stream);
    }
    
    private static String getCommandIdFromEvent(String eventId, JsonObject evPayload) {
        if (evPayload.has(EVENT_CMD_ID))
            return evPayload.getAsJsonPrimitive(EVENT_CMD_ID).getAsString();
        else
            return eventId;
    }

    @Override
    public TSink<JsonObject> events(TStream<JsonObject> stream, String eventId, int qos) {
        
        stream = stream.map(e -> {
            JsonObject c = new JsonObject();
            c.addProperty(CMD_ID, getCommandIdFromEvent(eventId, e));
            c.add(CMD_PAYLOAD, e);
            c.addProperty(CMD_FORMAT, "json");
            c.addProperty(CMD_TS, System.currentTimeMillis());
            return c;
        });
        
        return handleEvents(stream);
    }
    
    private TSink<JsonObject> handleEvents(TStream<JsonObject> stream) {
        
        if (echoCmds == null)
            echoCmds = PlumbingStreams.isolate(stream, true);
        else
            echoCmds = PlumbingStreams.isolate(stream.union(echoCmds), true);
        
        return stream.sink(discard());
    }

    @Override
    public TStream<JsonObject> commands(String... commands) {
        if (commands.length == 0)
            return echoCmds;

        Set<String> cmds = new HashSet<>(Arrays.asList(commands));
        return echoCmds.filter(cmd -> cmds.contains(cmd.getAsJsonPrimitive(CMD_ID).getAsString()));
    }

    @Override
    public String getDeviceType() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getDeviceId() {
      // TODO Auto-generated method stub
      return null;
    }
}

