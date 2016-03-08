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

package quarks.test.apps.iot;

import static quarks.function.Functions.alwaysTrue;
import static quarks.function.Functions.discard;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;

import quarks.connectors.iot.IotDevice;
import quarks.function.Function;
import quarks.function.UnaryOperator;
import quarks.topology.TSink;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.plumbing.PlumbingStreams;

/**
 * A test IotDevice that echos back every event as a command with command
 * identifier equal to the {@code cmdId} value in the event payload. If {@code cmdId}
 * is not set then {@code ec_eventId} is used.
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
            return "ec_" + eventId;
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
            return echoCmds.filter(alwaysTrue());

        TStream<JsonObject> cmd0 = echoCmds
                .filter(j -> j.getAsJsonPrimitive(CMD_ID).getAsString().equals(commands[0]));

        if (commands.length == 1)
            return cmd0;

        Set<TStream<JsonObject>> cmds = new HashSet<>();
        for (int i = 1; i < commands.length; i++) {
            final int idx = i;
            cmds.add(echoCmds.filter(j -> j.getAsJsonPrimitive(CMD_ID).getAsString().equals(commands[idx])));
        }

        return cmd0.union(cmds);
    }
}