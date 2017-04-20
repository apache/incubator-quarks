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
import org.apache.edgent.analytics.math3.UnivariateAggregate;
import org.apache.edgent.analytics.math3.UnivariateAggregator;

/**
 * Univariate aggregator implementation wrapping a {@code StorelessUnivariateStatistic}.
 */
public class StorelessStatistic implements UnivariateAggregator {
        
    private final Statistic2 stat;
    private final StorelessUnivariateStatistic statImpl;
    
    public StorelessStatistic(Statistic2 stat, StorelessUnivariateStatistic statImpl) {
        this.stat = stat;
        this.statImpl = statImpl;
    }

    @Override
    public UnivariateAggregate getAggregate() {
      return stat;
    }

    @Override
    public void clear(int n) {
        statImpl.clear();
    }

    @Override
    public void increment(double v) {
        statImpl.increment(v);
    }
    
    @Override
    public double getResult() {
      return statImpl.getResult();
    }

}
