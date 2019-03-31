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
package org.apache.edgent.execution.mbeans;

import java.util.concurrent.TimeUnit;

/**
 * Control mbean interface for an entity having an a time period control.
 * <P>
 * This mbean lacks a {@code TYPE} declaration because it's
 * a generic control interface applicable to a variety of
 * object types (e.g., a stream or window).
 * The type of the associated object is to be used when
 * registering instances of this mbean with the
 * {@link org.apache.edgent.execution.services.ControlService ControlService}.
 * </P>
 * 
 * See {@code org.apache.edgent.org.apache.edgent.org.apache.edgent.topology.Topology.poll(org.apache.edgent.function.Supplier, long, TimeUnit)}
 */
public interface PeriodMXBean {
    
    /**
     * Get the period.
     * @return period
     */
    public long getPeriod();
    
    /**
     * Get the time unit for {@link #getPeriod()}.
     * @return time unit
     */
    public TimeUnit getUnit();
    
    /**
     * Set the period.
     * @param period the period
     */
    public void setPeriod(long period);
    
    /**
     * Set the period and unit
     * @param period the period
     * @param unit the {@link TimeUnit} of {@code period}
     */
    public void setPeriod(long period, TimeUnit unit);
}
