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
package org.apache.edgent.oplet.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.mbeans.PeriodMXBean;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.OutputPortContext;

public abstract class PeriodicSource<T> extends Source<T> implements Runnable, PeriodMXBean {
  
    // see comment in TStream.TYPE
    private static final String TSTREAM_TYPE = /*TStream.TYPE*/"stream";

    private long period;
    private TimeUnit unit;
    private ScheduledFuture<?> future;

    protected PeriodicSource(long period, TimeUnit unit) {
        this.period = period;
        this.unit = unit;
    }

    @Override
    public void initialize(OpletContext<Void, T> context) {
        super.initialize(context);
    }

    @Override
    public synchronized void start() {
        ControlService cs = getOpletContext().getService(ControlService.class);
        // TODO BUG HERE: the control alias needs to be unique across the
        // entire provider instance (multiple topologies) because the ControlService
        // is provider-wide, not org.apache.edgent.org.apache.edgent.topology specific.
        // Scope it with just the jobId.  What's going to unregister this control?
        if (cs != null)
            cs.registerControl(TSTREAM_TYPE, getOpletContext().uniquify(getClass().getSimpleName()), 
                    getAlias(), PeriodMXBean.class, this);
        schedule(false);
    }
    
    private String getAlias() {
        OutputPortContext oc = getOpletContext().getOutputContext().get(0);
        return oc.getAlias();
    }

    private synchronized void schedule(boolean delay) {
        future = getOpletContext().getService(ScheduledExecutorService.class).scheduleAtFixedRate(
                getRunnable(), delay ? getPeriod() : 0, getPeriod(), getUnit());
    }

    protected Runnable getRunnable() {
        return this;
    }

    protected abstract void fetchTuples() throws Exception;

    @Override
    public void run() {
        try {
            fetchTuples();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized long getPeriod() {
        return period;
    }

    @Override
    public synchronized TimeUnit getUnit() {
        return unit;
    }

    @Override
    public synchronized void setPeriod(long period) {
        setPeriod(period, getUnit());
    }

    @Override
    public synchronized void setPeriod(long period, TimeUnit unit) {
        if (period <= 0)
            throw new IllegalArgumentException();
        if (this.period != period || this.unit != unit) {
            future.cancel(false);
            this.period = period;
            this.unit = unit;
            schedule(true);
        }  
    }
}
