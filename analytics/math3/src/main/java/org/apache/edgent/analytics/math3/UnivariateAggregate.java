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

import org.apache.edgent.function.Supplier;

/**
 * Univariate aggregate for a tuple.
 * This is the declaration of the aggregate that
 * application use when declaring a topology.
 * <P>
 * Implementations are typically enums such
 * as {@link org.apache.edgent.analytics.math3.stat.Statistic2 Statistic2}.
 * </P>
 * <P>
 * Each call to {@code get()} must return a new
 * {@link UnivariateAggregator aggregator}
 * that implements the required aggregate.
 * </P>
 * 
 * @see Aggregations
 */
public interface UnivariateAggregate extends Supplier<UnivariateAggregator>{
    
    /**
     * Name of the aggregate.
     * The returned value is used as the JSON key containing
     * the result of the aggregation.
     * @return Name of the aggregate.
     */
    public String name();
}
