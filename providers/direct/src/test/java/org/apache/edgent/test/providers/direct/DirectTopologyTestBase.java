package org.apache.edgent.test.providers.direct;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Submitter;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.test.topology.TopologyAbstractTest;
import org.apache.edgent.topology.Topology;

public class DirectTopologyTestBase extends TopologyAbstractTest {

  @Override
  public DirectProvider createTopologyProvider() {
      return new DirectProvider();
  }

  @Override
  public Submitter<Topology, Job> createSubmitter() {
      return (DirectProvider) getTopologyProvider();
  }

}
