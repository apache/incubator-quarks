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
package quarks.analytics.math3.stat;

import quarks.analytics.math3.json.JsonUnivariateAggregate;
import quarks.analytics.math3.json.JsonUnivariateAggregator;

/**
 * Univariate regression aggregates.
 *
 */
public enum Regression implements JsonUnivariateAggregate {
    
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
     * If the window to be aggregated contains less than
     * two values then no regression is performed.
     */
    SLOPE() {
        @Override
        public JsonUnivariateAggregator get() {
            return new JsonOLS(this);
        }
    }
}
