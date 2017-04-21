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

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.apache.edgent.analytics.math3.Aggregations;
import org.apache.edgent.analytics.math3.UnivariateAggregate;
import org.apache.edgent.analytics.math3.UnivariateAggregator;

/**
 * Statistic implementations.
 * 
 * Univariate statistic aggregate calculations against a value
 * extracted from a tuple.
 * 
 * Unfortunately the implicitly-Json Statistic wasn't named JsonStatistic.
 * 
 * @see Aggregations
 */
public enum Statistic2 implements UnivariateAggregate {
    
    /**
     * Calculate the number of elements in the collection.
     * The value is represented as a {@code double}
     * with the key {@code COUNT} in the aggregate result.
     */
    COUNT(new Count()),
    /**
     * Calculate the arithmetic mean.
     * The mean value is represented as a {@code double}
     * with the key {@code MEAN} in the aggregate result.
     */
    MEAN(new Mean()),
    /**
     * Calculate the minimum.
     * The minimum value is represented as a {@code double}
     * with the key {@code MIN} in the aggregate result.
     */
    MIN(new Min()),
    /**
     * Calculate the maximum.
     * The maximum value is represented as a {@code double}
     * with the key {@code MAXIMUM} in the aggregate result.
     */
    MAX(new Max()),
    /**
     * Calculate the sum.
     * The sum is represented as a {@code double}
     * with the key {@code SUM} in the aggregate result.
     */
    SUM(new Sum()),
    /**
     * Calculate the standard deviation.
     */
    STDDEV(new StandardDeviation());

    private final StorelessUnivariateStatistic statImpl;

    private Statistic2(StorelessUnivariateStatistic statImpl) {
        this.statImpl = statImpl;
        statImpl.clear();
    }

    /**
     * Return a new instance of this statistic implementation.
     * N.B. must call clear(n) before using the result.
     * @return A new instance of this statistic implementation.
     */
    @Override
    public UnivariateAggregator get() {
        return new StorelessStatistic(this, statImpl.copy());
    }
}
