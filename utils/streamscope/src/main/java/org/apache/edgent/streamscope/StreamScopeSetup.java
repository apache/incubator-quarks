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
package org.apache.edgent.streamscope;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.function.Functions;
import org.apache.edgent.streamscope.mbeans.StreamScopeRegistryMXBean;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.TopologyProvider;

/**
 * Utility helpers for StreamScope setup and management.
 * <p>
 * This class is not thread safe.
 */
public class StreamScopeSetup {
    private final StreamScopeRegistry rgy;
    private StreamScopeRegistryBean rgyBean;
    @SuppressWarnings("unused")
    private String rgyBeanControlId;
    
    // Hackery is needed to work around all of the issues cropping
    // up with per-provider StreamScopeRegistry and complications
    // caused by JMXControlService and SteamsScopeRegistryMXBean registrations.
    //
    // There are probably a couple of possible long term solutions.
    // So for now minimize changing anything already created - i.e.,
    // keep lazy StreamScopeBean creation via use of StreamScopeRegistryBean.
    //
    // Avoid rgy / rgyBean mismatches or rgyBean "already registered"
    // conditions by mirroring the static-ness of using the platform JMX instance
    // with a singleton StreamScopeSetup instance... which will result in
    // singleton rgy and rgyBean instances.
    //
    // More background on issues:
    // Nothing is unregistering the registry control bean from the JMX...
    // and attempting to register by the next new provider instance throws.
    // Note, there's no cs.unregister(type,alias) (Really? ugh) so we'd
    // have to remember the rgyBean control controlId for use with cs.unregister(controlId).
    // 
    // There's a general issue of missing hooks to make unregistering
    // controls for jobs or non-job/oplet related entities possible.
    // ServiceContainer.addCleaner() provides a hook for job oplet shutdown.
    // But there are no hooks available to be able to unregister JobMXBean,
    // AppService bean, or StreamScopeRegistry etc beans.
    //
    // While there's a storage leak for Job beans regardless of
    // which ControlService is used, since job beans
    // register with a jobId that's unique across successive provider instances
    // in a JVM (the Etiao id generator is static), an "already registered"
    // condition is avoided.  StreamScopeRegistry bean uses the same control
    // alias so it encounters this problem.
    // AppService bean uses the same alias ("edgent") but it's only being
    // used with the JsonControlService so it doesn't encounter "already
    // registered" on successive provider instances in a JVM (each of which
    // allocates a new control service instance).


    // the singleton StreamScopeSetup workaround.
    private static StreamScopeSetup setup = new StreamScopeSetup(new StreamScopeRegistry());
    
    /**
     * Perform the registrations needed to use the streamscope package.
     * <P>
     * Typically called during {@link TopologyProvider} construction.
     * </P>
     * <UL>
     * <LI>register the StreamScopeRegistry service</LI>
     * <LI>register a cleaner to remove job oplet StreamScope registrations</LI>
     * <LI>register a StreamScopeRegistryMXBean with the registered ControlService</LI>
     * </UL>
     * @param services ServiceContainer to register with.
     */
    public static void register(ServiceContainer services) {
      setup.registerPvt(services);
    }

    /**
     * Create a new {@link StreamScopeSetup} for setup.
     *
     * @param registry the registry to use
     */
    private StreamScopeSetup(StreamScopeRegistry registry) {
      this.rgy = registry;
    }
    
    /**
     * Perform the necessary registrations. 
     * <P>
     * <UL>
     * <LI>register the StreamScopeRegistry service</LI>
     * <LI>register a cleaner to remove job oplet StreamScope registrations</LI>
     * <LI>register a StreamScopeRegistryMXBean with the registered ControlService</LI>
     * </UL>
     * </P>
     * @param services ServiceContainer to register with.
     */
    private void registerPvt(ServiceContainer services) {
      services.addService(StreamScopeRegistry.class, rgy);
      services.addCleaner(
          (jobId, opletId) -> {
            rgy.unregister(jobId, opletId);
            if (rgyBean != null)
              rgyBean.unregister(jobId, opletId);
          });
      registerRegistryBean(services);
    }
    
    /**
     * Register a {@link StreamScopeRegistryMXBean} with the registered
     * {@link ControlService} (for use by the Edgent Console).
     */
    private void registerRegistryBean(ServiceContainer services) {
      ControlService cs = services.getService(ControlService.class);
      if (cs == null || rgy == null)
        throw new IllegalStateException();
      {
        // more workaround...
        //
        // If a rgyBean control is already registered, then don't reregister
        // (this will/should be the JMXControlService case) so as to avoid
        // an "already registered" exception from the ControlService.
        // The rgyBean is gotta be for the matching rgy with this 
        // singleton StreamScopeSetup instance scheme so everything is OK.
        
        StreamScopeRegistryMXBean mbean = cs.getControl(StreamScopeRegistryMXBean.TYPE, StreamScopeRegistryMXBean.TYPE, StreamScopeRegistryMXBean.class);
        if (mbean != null) {
          return;
        }
      }
      if (rgyBean == null)
        rgyBean = new StreamScopeRegistryBean(rgy, cs);
      rgyBeanControlId = cs.registerControl(StreamScopeRegistryMXBean.TYPE, 
          StreamScopeRegistryMXBean.TYPE+"_0", StreamScopeRegistryMXBean.TYPE,
          StreamScopeRegistryMXBean.class, rgyBean);
    }
    
    // until there's a hook available to be able to unregister things
//    
//    /**
//     * Unregister the StreamScopeRegistry service and the StreamScopeRegistryMXBean
//     * control.
//     * @param services
//     */
//    public void unregister(ServiceContainer services) {
//      services.removeService(StreamScopeRegistry.class);
//      unregisterRegistryBean(services);
//    }
//    
//    private void unregisterRegistryBean(ServiceContainer services) {
//      if (rgyBean != null) {
//        ControlService cs = services.getService(ControlService.class);
//        if (cs != null && rgyBeanControlId != null) {
//          cs.unregister(rgyBeanControlId);
//        }
//      }
//    }

    /**
     * Add StreamScope instances to the org.apache.edgent.org.apache.edgent.topology
     * <P>
     * Instrument the org.apache.edgent.org.apache.edgent.topology by adding StreamScope peekers to the
     * org.apache.edgent.org.apache.edgent.topology's streams.  At org.apache.edgent.org.apache.edgent.topology submission time, StreamScopes
     * register themselves with the org.apache.edgent.org.apache.edgent.topology's
     * {@link StreamScopeRegistry} runtime service.
     * </P>
     * 
     * @param t the Topology.  The operation is a no-op if the org.apache.edgent.org.apache.edgent.topology
     *     does not have a StreamScopeRegistry service.
     * @see #register(ServiceContainer) register
     * @see org.apache.edgent.graph.Graph#peekAll(org.apache.edgent.function.Supplier, org.apache.edgent.function.Predicate) Graph.peekAll()
     */
    public static void addStreamScopes(Topology t) {
      StreamScopeRegistry rgy = (StreamScopeRegistry) 
          t.getRuntimeServiceSupplier().get()
            .getService(StreamScopeRegistry.class);
      if (rgy == null)
        return;
      
      // N.B. at runtime, the Console will need to be able to lookup
      // StreamScopeMXBean be streamId (see StreamScopeRegistryMXBean).
      // Nominally, that streamId should be the jobId/opletId/oport of
      // the stream that the StreamScope was created for - what I'll
      // call the "origin stream". i.e., the Console shouldn't be 
      // looking up a streamId for the StreamScopeOplet's opletId.
      //
      // Registration is left to the StreamScope oplet initialization processing
      // since the jobId isn't known until that time.
      // 
      // As noted above we really want the StreamScope's streamId registration
      // to be for the "origin stream".  Today the API (Graph,Vertex,Oplet)
      // doesn't provide that information so we can't capture that as part
      // of the StreamScope oplet's info.  The net is that for the time being
      // a StreamScope will end up having to register itself with its opletId,
      // not the origin oplet's id and that has implications for the Console. 
      //
      // We could create a peekAllFn that takes a BiFunction that receives
      // the Vertex+oport the StreamScope is being created for but that
      // Vertex/Oplet's opletId still isn't accessible today.
      // (The Etaio provider maintains the opletId in its Instance object fwiw).
      //
      // TODO straighten this all out

      t.graph().peekAll( 
          () -> { return new org.apache.edgent.streamscope.oplets.StreamScope<>(new StreamScope<>()); },
          Functions.alwaysTrue());
    }

}
