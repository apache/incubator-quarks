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
package org.apache.edgent.analytics.math3.json;

import org.apache.edgent.function.Supplier;

/**
 * Univariate aggregate for a JSON tuple.
 * This is the declaration of the aggregate that
 * application use when declaring a org.apache.edgent.org.apache.edgent.topology.
 * <P>
 * Implementations are typically enums such
 * as {@link org.apache.edgent.analytics.math3.stat.Statistic Statistic}.
 * </P>
 * <P>
 * Each call to {@code get()} must return a new
 * {@link JsonUnivariateAggregator aggregator}
 * that implements the required aggregate.
 * </P>
 * 
 * @see JsonAnalytics
 */
public interface JsonUnivariateAggregate extends Supplier<JsonUnivariateAggregator>{
    
    /**
     * JSON key used for representation of the number
     * of tuples that were aggregated. Value is {@value}.
     */
    public static final String N = "N";
    
    /**
     * Name of the aggregate.
     * The returned value is used as the JSON key containing
     * the result of the aggregation.
     * @return Name of the aggregate.
     */
    public String name();
}
