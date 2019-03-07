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
package org.apache.edgent.topology;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Submitter;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Before;
import org.junit.Ignore;

import com.google.gson.JsonObject;

@Ignore("Abstract class proiding generic org.apache.edgent.org.apache.edgent.topology testing.")
public abstract class TopologyAbstractTest implements TopologyTestSetup {

    private TopologyProvider topologyProvider;
    private Submitter<Topology, Job> submitter;

    @Before
    public void setup() {
        topologyProvider = createTopologyProvider();
        assertNotNull(topologyProvider);

        submitter = createSubmitter();
        assertNotNull(submitter);
    }

    protected Submitter<Topology, ?> getSubmitter() {
        return submitter;
    }

    @Override
    public TopologyProvider getTopologyProvider() {
        return topologyProvider;
    }

    protected Topology newTopology() {
        return getTopologyProvider().newTopology();
    }

    protected Topology newTopology(String name) {
        return getTopologyProvider().newTopology(name);
    }

    protected boolean complete(Topology topology, Condition<?> endCondition, long timeout, TimeUnit units) throws Exception {
        return topology.getTester().complete(getSubmitter(), new JsonObject(), endCondition, timeout, units);
    }

    protected boolean complete(Topology topology, Condition<?> endCondition) throws Exception {
        return complete(topology, endCondition, 10, TimeUnit.SECONDS);
    }
    
    public void completeAndValidate(String msg, Topology t,
            TStream<String> s, int secTimeout, String... expected)
            throws Exception {
        completeAndValidate(true/*ordered*/, msg, t, s, secTimeout, expected);
    }
    
    public void completeAndValidate(boolean ordered, String msg, Topology t,
            TStream<String> s, int secTimeout, String... expected)
            throws Exception {

        // if expected.length==0 we must run until the job completes or tmo
        Condition<Long> tc = t.getTester().tupleCount(s, 
                expected.length == 0 ? Long.MAX_VALUE : expected.length);
        Condition<List<String>> contents = 
                ordered ? t.getTester().streamContents(s, expected)
                        : t.getTester().contentsUnordered(s, expected);

        complete(t, tc, secTimeout, TimeUnit.SECONDS);

        assertTrue(msg + " contents:" + contents.getResult(), contents.valid());
        if (expected.length != 0)
            assertTrue("valid:" + tc.getResult(), tc.valid());
    }

    protected void assertStream(Topology t, TStream<?> s) {
        assertSame(t, s.topology());
    }

}
