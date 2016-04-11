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
package quarks.test.runtime.jobregistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import quarks.execution.Job;
import quarks.execution.services.JobRegistryService;
import quarks.execution.services.JobRegistryService.EventType;
import quarks.function.BiConsumer;
import quarks.runtime.jobregistry.JobRegistry;

public class JobRegistryTest {

    @Test
    public void testGetJob() {
        JobRegistryService service = new JobRegistry();
        Job job1 = new JobImpl("Job_1");
        Job job2 = new JobImpl("Job_2");
        service.addJob(job1);
        service.addJob(job2);
        
        assertSame(job1, service.getJob("Job_1"));
        assertSame(job2, service.getJob("Job_2"));
    }

    @Test
    public void testAddJob() {
        JobRegistryService service = new JobRegistry();
        
        service.addJob(new JobImpl("Job_1"));
        service.addListener((et, j) ->
            {
                assertEquals(JobRegistryService.EventType.ADD, et);
                assertEquals("Job_1", j.getId());
            });
    }
    
    @Test
    public void testAddJob2() {
        JobRegistryService service = new JobRegistry();
        
        service.addListener((et, j) -> {
                assertEquals(JobRegistryService.EventType.ADD, et);
                assertEquals("Job_1", j.getId());
            });
        service.addJob(new JobImpl("Job_1"));
    }

    @Test
    public void testRemoveJob() {
        JobRegistryService service = new JobRegistry();
        service.addJob(new JobImpl("Job_1"));
        service.addJob(new JobImpl("Job_2"));
        
        service.addListener(new JobHandler(JobRegistryService.EventType.REMOVE));
        service.removeJob("Job_1");
        service.removeJob("Job_2");
    }

    @Test
    public void testUpdateJob() {
        JobRegistryService service = new JobRegistry();
        service.addJob(new JobImpl("Job_1"));
        service.addJob(new JobImpl("Job_2"));
        
        service.addListener(new JobHandler(JobRegistryService.EventType.UPDATE));
        service.updateJob(service.getJob("Job_1"));
        service.updateJob(service.getJob("Job_2"));
    }

    private static class JobHandler implements BiConsumer<JobRegistryService.EventType, Job> {
        private static final long serialVersionUID = 1L;
        private int eventCount = 0;
        private final JobRegistryService.EventType eventType;
        
        JobHandler(JobRegistryService.EventType et) {
            this.eventType = et;
        }

        @Override
        public void accept(EventType et, Job j) {
            eventCount++;
            switch (eventCount) {
            case 1:
            case 2:
                assertEquals(JobRegistryService.EventType.ADD, et);
                break;
            case 3:
                assertEquals(eventType, et);
                assertEquals("Job_1", j.getId());
                break;
            case 4:
                assertEquals(eventType, et);
                assertEquals("Job_2", j.getId());
                break;
            }
        }
    }

    private static class JobImpl implements Job {

        private String id;
        
        JobImpl(String id) {
            this.id = id;
        }

        @Override
        public State getCurrentState() {
            throw new UnsupportedOperationException();
        }

        @Override
        public State getNextState() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stateChange(Action action) throws IllegalArgumentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Health getHealth() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLastError() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void complete() throws ExecutionException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void complete(long timeout, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            throw new UnsupportedOperationException();
        }
        
    }
}
