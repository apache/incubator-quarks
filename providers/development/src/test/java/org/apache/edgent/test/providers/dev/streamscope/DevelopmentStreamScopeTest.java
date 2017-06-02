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
package org.apache.edgent.test.providers.dev.streamscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.streamscope.StreamScope;
import org.apache.edgent.streamscope.StreamScopeRegistry;
import org.apache.edgent.streamscope.StreamScope.Sample;
import org.apache.edgent.streamscope.mbeans.StreamScopeMXBean;
import org.apache.edgent.streamscope.mbeans.StreamScopeRegistryMXBean;
import org.apache.edgent.test.providers.dev.DevelopmentTestSetup;
import org.apache.edgent.test.streamscope.StreamScopeTest;
import org.apache.edgent.topology.Topology;
import org.junit.Test;

import com.google.gson.Gson;

public class DevelopmentStreamScopeTest extends StreamScopeTest implements DevelopmentTestSetup {
  
  @Test
  public void testServiceRegistered() throws Exception {
    Topology t1 = newTopology();
    StreamScopeRegistry rgy1 = t1.getRuntimeServiceSupplier().get()
        .getService(StreamScopeRegistry.class);
    assertNotNull(rgy1);
    
    Topology t2 = newTopology();
    StreamScopeRegistry rgy2 = t2.getRuntimeServiceSupplier().get()
        .getService(StreamScopeRegistry.class);
    assertNotNull(rgy2);
    
    assertSame(rgy1, rgy2);
  }
  
  @Test
  public void testRegistryControlRegistered() throws Exception {
    Topology t1 = newTopology();
    ControlService cs1 = t1.getRuntimeServiceSupplier().get()
        .getService(ControlService.class);
    StreamScopeRegistryMXBean rgy1 = cs1.getControl(StreamScopeRegistryMXBean.TYPE,
        StreamScopeRegistryMXBean.TYPE, StreamScopeRegistryMXBean.class);
    assertNotNull(rgy1);
    
    Topology t2 = newTopology();
    ControlService cs2 = t2.getRuntimeServiceSupplier().get()
        .getService(ControlService.class);
    StreamScopeRegistryMXBean rgy2 = cs2.getControl(StreamScopeRegistryMXBean.TYPE,
        StreamScopeRegistryMXBean.TYPE, StreamScopeRegistryMXBean.class);
    assertNotNull(rgy2);
    
    // The rgy1, rgy1 mbean instances may or may not be the same object
    // depending on the ControlService implementation.  For JMXControlService,
    // each getControl() yields a different MXBeanProxy instance but they are
    // for the underlying bean (same objectname).
    //assertSame(rgy1, rgy2);
  }
  
  @Test
  public void testStreamScopeBeans() throws Exception {
    testStreamScopeBeans("JOB_1000");
  }
  
  private void testStreamScopeBeans(String jobId) throws Exception {
    // Development provider should have controls registered.
    
    // Get the Rgy and RgyBean
    Topology t1 = newTopology();
    StreamScopeRegistry rgy = t1.getRuntimeServiceSupplier().get()
        .getService(StreamScopeRegistry.class);
    assertNotNull(rgy);
    ControlService cs = t1.getRuntimeServiceSupplier().get()
                          .getService(ControlService.class);
    StreamScopeRegistryMXBean rgyBean = 
        cs.getControl(StreamScopeRegistryMXBean.TYPE,
            StreamScopeRegistryMXBean.TYPE, StreamScopeRegistryMXBean.class);
    assertNotNull(rgyBean);
    
    // Add a StreamScope and verify it can be located via the controls
    StreamScope<Integer> ss1 = new StreamScope<Integer>();
    String streamId = StreamScopeRegistry.mkStreamId(jobId, "OP_1", 2);
    rgy.register(StreamScopeRegistry.nameForStreamId(streamId), ss1);
    
    StreamScopeMXBean ss1Bean = rgyBean.lookup(jobId, "OP_1", 2);
    assertNotNull(ss1Bean);
    
    ss1.setEnabled(true);
    ss1.accept(100);
    ss1.accept(101);
    ss1.accept(102);
    // access via the bean
    assertEquals(3, ss1Bean.getSampleCount());
    String json = ss1Bean.getSamples();
    assertNotNull(json);
    
    Gson gson = new Gson();
    Sample<?>[] sa = gson.fromJson(json, Sample[].class);
    for (int i = 0; i < 3; i++) {
      Sample<?> s = sa[i];
      Object t = s.tuple(); // fyi, w/o type info fromJson() yields a Double for the numeric
      assertEquals(t, i+100.0);
    }
  }
  
  @Test
  public void testStreamScopeBeans2() throws Exception {
    // verify successive providers and rgyBean control hackery works
    testStreamScopeBeans("JOB_1001");
  }

  // Ideally would test that beans are available via JMX and/or servlet.StreamScopeUtil stuff works
}
