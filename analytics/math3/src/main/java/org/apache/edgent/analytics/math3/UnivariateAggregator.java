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
package org.apache.edgent.analytics.math3;

/**
 * Univariate aggregator for tuples.
 * This is the runtime implementation interface
 * of {@link UnivariateAggregate} defined aggregate.
 */
public interface UnivariateAggregator {
  
    UnivariateAggregate getAggregate();
    
    /**
     * Clear the aggregator to prepare for a new aggregation.
     * @param n Number of tuples to be aggregated.
     */
    void clear(int n);
    
    /**
     * Add a value to the aggregation. 
     * @param value Value to be added.
     */
    void increment(double value);
    
    /**
     * Get the aggregation result.  It may be a NaN (empty collection, etc) or Infinite value.
     * @return the result
     */
    double getResult();
}
