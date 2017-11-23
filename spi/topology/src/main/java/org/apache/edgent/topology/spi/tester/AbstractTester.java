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
package org.apache.edgent.topology.spi.tester;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.Job;
import org.apache.edgent.execution.Job.State;
import org.apache.edgent.execution.Submitter;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.apache.edgent.topology.tester.Tester;

import com.google.gson.JsonObject;

public abstract class AbstractTester implements Tester { 
    
    private Job job;
    
    private static long getTimeoutValue(long timeout, TimeUnit units) {
        // try to protect the tests from timing out prematurely
        // in the face of overloaded/slow build/test servers.
        //
        // One problem with the following "generally bump the timeout value"
        // scheme is that there are some tests that expect / test-for something
        // to NOT happen - within some timeout tolerance.
        // Ideally we don't want such tests to always take this extra-long timeout.
        // Not sure what to do about that.  The first step is to just try this and
        // see if it generally addresses the slow-test-server problem.
        // Then we can review test execution times to identify those that fall
        // into this case and then contemplate what to do about them.
        
        if (Boolean.getBoolean("edgent.build.ci")) {
            // could do something like base the decision of the current value of timeout and/or units
            return timeout * 2;  // minimize the multiplier because of the aforementioned await-tmo test cases
        }
        return timeout;
    }

    @Override
    public boolean complete(Submitter<Topology, ? extends Job> submitter, JsonObject config, Condition<?> endCondition,
            long timeout, TimeUnit unit) throws Exception {

        long tmoMsec = Math.max(unit.toMillis(timeout), 1000);
        tmoMsec = getTimeoutValue(tmoMsec, TimeUnit.MILLISECONDS);
        long maxTime = System.currentTimeMillis() + tmoMsec;

        Future<?> future = submitter.submit(topology(), config);
        // wait at most tmoMsec for the submit to create the job
        job = (Job) future.get(tmoMsec, TimeUnit.MILLISECONDS);

        // wait for the first of: endCondition, jobComplete, tmo
        while (!endCondition.valid()
                && getJob().getCurrentState() != State.CLOSED
                && System.currentTimeMillis() < maxTime) {
            Thread.sleep(100);
        }
        
        if (!endCondition.valid() && getJob().getCurrentState() != State.CLOSED) {
            System.err.println("complete(): timed out after " + tmoMsec + "msec");
        }
        
        if (getJob().getCurrentState() != State.CLOSED)
            getJob().stateChange(Job.Action.CLOSE);
        else
            System.out.println("complete(): Job already closed");

        return endCondition.valid();
    }
    
    @Override
    public Job getJob() {
        return job;
    }
    
    @Override
    public Condition<Boolean> and(final Condition<?>... conditions) {
        return new Condition<Boolean>() {

            @Override
            public boolean valid() {
                for (Condition<?> condition : conditions)
                    if (!condition.valid())
                        return false;
                return true;
            }

            @Override
            public Boolean getResult() {
                return valid();
            }
        };
    }
}
