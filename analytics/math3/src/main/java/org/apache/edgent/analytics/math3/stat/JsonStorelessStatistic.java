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
import org.apache.edgent.analytics.math3.json.JsonUnivariateAggregator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.edgent.analytics.math3.utils.Java7Helper;

/**
 * JSON univariate aggregator implementation wrapping a {@code StorelessUnivariateStatistic}.
 */
public class JsonStorelessStatistic implements JsonUnivariateAggregator {
        
    private final Statistic stat;
    private final StorelessUnivariateStatistic statImpl;
    
    public JsonStorelessStatistic(Statistic stat, StorelessUnivariateStatistic statImpl) {
        this.stat = stat;
        this.statImpl = statImpl;
    }

    @Override
    public void clear(JsonElement partition, int n) {
        statImpl.clear();
    }

    @Override
    public void increment(double v) {
        statImpl.increment(v);
    }

    @Override
    public void result(JsonElement partition, JsonObject result) {        
        double rv = statImpl.getResult();
        
        if (Java7Helper.doubleIsFinite(rv))
            result.addProperty(stat.name(), rv);
    }

}
