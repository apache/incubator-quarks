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
package org.apache.edgent.test.connectors.common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

public class ConnectorTestBase extends DirectTopologyTestBase {
    
    public static List<String> createMsgs(MsgGenerator mgen, String topic, String msg1, String msg2) {
        List<String> msgs = new ArrayList<>();
        msgs.add(mgen.create(topic, msg1));
        msgs.add(mgen.create(topic, msg2));
        return msgs;
    }

    public static String simpleTS() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    }
    
    public static class MsgId {
        private int seq;
        private String uniq;
        private String prefix;
        MsgId(String prefix) {
            this.prefix = prefix;
        }
        String next() {
            if (uniq==null) {
                uniq = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            }
            return String.format("[%s.%d %s]", uniq, seq++, prefix);
        }
        String pattern() {
            return String.format(".*\\[%s\\.\\d+ %s\\].*", uniq, prefix);
        }
    }

    public static class MsgGenerator {
        private MsgId id;
        public MsgGenerator(String testName) {
            id = new MsgId(testName);
        }
        public String create(String topic, String baseContent) {
            return String.format("%s [for-topic=%s] %s", id.next(), topic, baseContent);
        }
        public String pattern() {
            return id.pattern();
        }
    }

    public static class Msg implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String topic;
        private final String msg;

        public Msg(String msg, String topic) {
            this.msg = msg;
            this.topic = topic;
        }
        public String getTopic()   { return topic; }
        public String getMessage() { return msg; }
        public String toString()   { return "[topic="+topic+", msg="+msg+"]"; }
    }
    
    public void completeAndValidate(String msg, Topology t,
            TStream<String> s, MsgGenerator mgen, int secTimeout, String... expected)
            throws Exception {
        completeAndValidate(true/*ordered*/, msg, t, s, mgen, secTimeout, expected);
    }
    
    public void completeAndValidate(boolean ordered, String msg, Topology t,
            TStream<String> s, MsgGenerator mgen, int secTimeout, String... expected)
            throws Exception {
        
        s = s.filter(tuple -> tuple.matches(mgen.pattern()));
        s.sink(tuple -> System.out.println(
                String.format("[%s][%s] rcvd: %s", t.getName(), simpleTS(), tuple)));

        super.completeAndValidate(ordered, msg, t, s, secTimeout, expected);
    }

}
