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
package org.apache.edgent.test.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.SortedMap;

import org.apache.edgent.execution.Job;
import org.apache.edgent.metrics.Metrics;
import org.apache.edgent.topology.TopologyAbstractTest;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Ignore;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * This will only work with an direct setup.
 */
@Ignore
public abstract class MetricsBaseTest extends TopologyAbstractTest {

    protected MetricRegistry metricRegistry;
    
    @Test
    public void counter() throws Exception {
        counter(new String[] {"a", "b", "c"});
    }

    @Test
    public void counterZeroTuples() throws Exception {
        counter(new String[0]);
    }

    @Test
    public void rateMeter() throws Exception {
        rateMeter(new String[] {"a", "b", "c"});
    }

    @Test
    public void rateMeterZeroTuples() throws Exception {
        rateMeter(new String[0]);
    }

    private final void counter(String[] data) throws Exception {
        Topology t = newTopology();
        TStream<String> s = t.strings(data);
        s = Metrics.counter(s);

        waitUntilComplete(t, s, data);

        if (metricRegistry != null) {
            SortedMap<String, Counter> counters = metricRegistry.getCounters();
            assertEquals(1, counters.size());
            Collection<Counter> values = counters.values();
            for (Counter v : values) {
                assertEquals(data.length, v.getCount());
            }
        }
    }

    private final void rateMeter(String[] data) throws Exception {
        Topology t = newTopology();
        TStream<String> s = t.strings(data);
        s = Metrics.rateMeter(s);

        waitUntilComplete(t, s, data);

        if (metricRegistry != null) {
            SortedMap<String, Meter> meters = metricRegistry.getMeters();
            assertEquals(1, meters.size());
            Collection<Meter> values = meters.values();
            for (Meter v : values) {
                assertEquals(data.length, v.getCount());
            }
        }
    }

    protected Job job;
    protected void waitUntilComplete(Topology t, TStream<String> s, String[] data) throws Exception {
        Condition<Long> tc = t.getTester().tupleCount(s, data.length);
        complete(t, tc);
        
        // Save the job.
        job = t.getTester().getJob();
        assertNotNull(job);
        
    }
}
