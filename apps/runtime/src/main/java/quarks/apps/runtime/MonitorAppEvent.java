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
package quarks.apps.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import quarks.execution.Job;
import quarks.execution.services.job.JobRegistryService;

/**
 * Helpers for parsing generating and parsing a JSON representation of job
 * monitoring events. 
 */
class MonitorAppEvent {

    /**
     * Creates a JsonObject wrapping a {@code JobRegistryService} event type
     * and Job info.
     * 
     * @param evType the event type
     * @param job the job
     * @return the wrapped data
     */
    static JsonObject toJsonObject(JobRegistryService.EventType evType, Job job) {
        JsonObject value = new JsonObject();
        value.addProperty("time", (Number)System.currentTimeMillis());
        value.addProperty("event", evType.toString());
        JsonObject obj = new JsonObject();
        obj.addProperty("id", job.getId());
        obj.addProperty("name", job.getName());
        obj.addProperty("state", job.getCurrentState().toString());
        obj.addProperty("nextState", job.getNextState().toString());
        obj.addProperty("health", job.getHealth().toString());
        obj.addProperty("lastError", job.getLastError());
        value.add("job", obj);
        return value;
    }

    /**
     * Gets the {@code job} JsonObject from the given JSON value.
     * 
     * @param value a JSON object
     * @return the job JsonObject
     */
    static JsonObject getJob(JsonObject value) {
        JsonObject job = value.getAsJsonObject("job");
        if (job == null) {
            throw new IllegalArgumentException("Could not find the job object in: " + value);
        }
        return job;
    }

    /**
     * Gets the {@code name} string property from the given job JSON object.
     *  
     * @param job the job JSON object
     * @return the job name
     * @throws IllegalArgumentException if it could not find the property
     */
    static String getJobName(JsonObject job) {
        return getProperty(job, "name");
    }

    /**
     * Gets the {@code health} string property from the given job JSON object.
     * 
     * @param job the job JSON object
     * @return the job health
     * @throws IllegalArgumentException if it could not find the property
     */
    static String getJobHealth(JsonObject job) {
        return getProperty(job, "health");
    }

    /**
     * Gets a string property with the specified name from the given JSON 
     * object.
     * 
     * @param value a JSON object
     * @param name the property name
     * @return the property value
     * 
     * @throws IllegalArgumentException if could not find a property with the 
     *      given name
     */
    static String getProperty(JsonObject value, String name) {
        JsonElement e = value.get(name);
        if (e != null && e.isJsonPrimitive()) {
            try {
                return e.getAsString();
            } catch (Exception ex) {
            }
        }
        throw new IllegalArgumentException("Could not find the " + name + " property in: " + value);
    }
}
