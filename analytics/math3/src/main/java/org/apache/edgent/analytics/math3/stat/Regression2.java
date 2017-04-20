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
package org.apache.edgent.analytics.math3.stat;

import org.apache.edgent.analytics.math3.UnivariateAggregate;
import org.apache.edgent.analytics.math3.UnivariateAggregator;

/**
 * Univariate regression aggregates.
 *
 * Unfortunately the implicitly-Json Regression wasn't named JsonRegresson.
 */
public enum Regression2 implements UnivariateAggregate {
    
    /**
     * Calculate the slope of a single variable.
     * The slope is calculated using the first
     * order of a ordinary least squares
     * linear regression.
     * The list of values for the single
     * single variable are processed as Y-values
     * that are evenly spaced on the X-axis.
     * <BR>
     * This is useful as a simple determination
     * if the variable is increasing or decreasing.
     * <BR>
     * The slope value is represented as a {@code double}
     * with the key {@code SLOPE} in the aggregate result.
     * <BR>
     * If the collection to be aggregated contains less than
     * two values then no regression is performed 
     * (an associated UnivariateAggregator.getResult() returns NaN).
     */
    SLOPE() {
        @Override
        public UnivariateAggregator get() {
            return new OLS(this);
        }
    }
}
