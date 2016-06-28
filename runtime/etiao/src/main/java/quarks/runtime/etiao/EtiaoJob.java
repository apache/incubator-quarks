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
package quarks.runtime.etiao;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import quarks.execution.services.ControlService;
import quarks.execution.services.JobRegistryService;
import quarks.execution.services.ServiceContainer;
import quarks.graph.spi.execution.AbstractGraphJob;
import quarks.oplet.JobContext;
import quarks.runtime.etiao.graph.DirectGraph;
import quarks.runtime.etiao.mbeans.EtiaoJobBean;

/**
 * Etiao runtime implementation of the {@link quarks.execution.Job} interface.
 */
public class EtiaoJob extends AbstractGraphJob implements JobContext {
    /** Prefix used by job unique identifiers. */
    public static final String ID_PREFIX = "JOB_";
    
    private final DirectGraph graph;
    private final String id;
    private final String topologyName;
    private final String name;
    private final ServiceContainer containerServices;
    private final JobRegistryService jobs;

    private static final AtomicInteger jobID = new AtomicInteger(0);

    /**
     * Creates a new {@code EtiaoJob} instance which controls the lifecycle 
     * of the specified graph.
     * 
     * @param graph graph representation of the topology
     * @param topologyName name of the topology
     * @param jobName name of the Job
     * @param container service container
     */
    EtiaoJob(DirectGraph graph, String topologyName, String jobName, ServiceContainer container) {
        this.graph = graph;
        this.id = ID_PREFIX + String.valueOf(jobID.getAndIncrement());
        this.topologyName = topologyName;
        this.name = jobName;
        this.containerServices = container;

        ControlService cs = container.getService(ControlService.class);
        if (cs != null) {
            EtiaoJobBean.registerControl(cs, this);
        }            
        this.jobs = container.getService(JobRegistryService.class);
        if (jobs != null)
            jobs.addJob(this);
    }

    /**
     * {@inheritDoc}
     * <P>
     * If a job name is not specified, this implementation 
     * creates a job name with the following format: {@code topologyName_jobId}.
     * </P>
     */
    @Override
    public String getName() {
        return name != null ? name : topologyName + "_" + this.id;
    }

    @Override
    public String getId() {
        return id;
    }
    
    ServiceContainer getContainerServices() {
        return containerServices;
    }

    @Override
    public void stateChange(Action action) {
        switch (action) {
        case INITIALIZE:
            setNext(State.INITIALIZED, action);
            executable().initialize();
            break;
        case START:
            setNext(State.RUNNING, action);
            executable().start();
            break;
        case PAUSE:
        case RESUME:
            throw new UnsupportedOperationException(action.name());
        case CLOSE:
            // idempotent
            State s = setNext(State.CLOSED, action);
            if (s != State.CLOSED)
                executable().close();
            else
                completeTransition();
            break;
        default:
            // TODO log Unsupported action
            // log.severe("Unsupported action: " + action.name());
            throw new IllegalArgumentException(action.name());
        }
    }

    Executable executable() {
        return graph.executable();
    }
    
    /* State transitions map.  Each entry associates a state with a set of 
     * reachable states the system can transition to. */
    static final HashMap<State, EnumSet<State>> stateMap;
    static {
        stateMap = new HashMap<State, EnumSet<State>>();

        stateMap.put(State.CONSTRUCTED, EnumSet.of(State.INITIALIZED, State.CLOSED));
        stateMap.put(State.INITIALIZED, EnumSet.of(State.RUNNING, State.CLOSED));
        stateMap.put(State.RUNNING,     EnumSet.of(State.PAUSED, State.CLOSED));
        stateMap.put(State.PAUSED,      EnumSet.of(State.RUNNING, State.CLOSED));
        stateMap.put(State.CLOSED,      EnumSet.of(State.CLOSED));
    }

    private synchronized State setNext(State desiredState, Action cause) {
        if (!isReachable(desiredState))
            throw new IllegalArgumentException(cause.name());
        else {
            setNextState(desiredState);
            updateRegistry();
            return getCurrentState();
        }
    }

    private boolean isReachable(State desiredState) {
        return !inTransition() && stateMap.get(getCurrentState()).contains(desiredState);
    }
    
    protected synchronized void completeTransition() {
        super.completeTransition();
        updateRegistry();
    }

    void onActionComplete() {
        completeTransition();
    }

    @Override
    public void complete() throws ExecutionException, InterruptedException {
        if (getCurrentState() != State.CLOSED && getNextState() != State.CLOSED)
            awaitComplete(Long.MAX_VALUE); // TODO remove timeout
    }

    @Override
    public void complete(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        if (getCurrentState() != State.CLOSED && getNextState() != State.CLOSED &&
                !awaitComplete(unit.toMillis(timeout))) {
            throw new TimeoutException();
        }
    }

    /**
     * Complete job closing.  This method can be invoked after a job close 
     * has been initiated.  
     */
    public void completeClosing(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        if (getCurrentState() != State.CLOSED &&
                !awaitComplete(unit.toMillis(timeout))) {
            throw new TimeoutException();
        }
    }

    private boolean awaitComplete(long millis) throws ExecutionException, InterruptedException {
        try {
            return executable().complete(millis);
        }
        catch (InterruptedException e) {
            throw e;
        }
        catch (Throwable t) {
            throw Executable.executionException(t);
        }
    }

    public DirectGraph graph() {
        return graph;
    }

    private void updateRegistry() {
        if (jobs != null)
            jobs.updateJob(this);
    }
    
    void updateHealth(Throwable t) {
        if (t != null) {
            setHealth(Health.UNHEALTHY);
            setLastError(t.getMessage());
        }
        updateRegistry();
    }
}
