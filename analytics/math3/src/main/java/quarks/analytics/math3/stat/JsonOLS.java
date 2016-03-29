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

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import quarks.analytics.math3.json.JsonUnivariateAggregator;

class JsonOLS implements JsonUnivariateAggregator {
    
    private final Regression type;
    private final OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
    private double[] values;
    private int yOffset;
    
    JsonOLS(Regression type) {
        this.type = type;
    }

    @Override
    public void clear(JsonElement partition, int n) {
        values = new double[n*2];
        yOffset = 0;
    }

    @Override
    public void increment(double v) {  
        values[yOffset] = v;
        yOffset+=2;
    }
    
    void setSampleData() {
        // Fill  in the x values
        for (int x = 0; x < values.length/2; x++)
            values[(x*2)+1] = x;
        ols.newSampleData(values, values.length/2, 1);
    }
    @Override
    public void result(JsonElement partition, JsonObject result) {
        // If there are no values or only a single
        // value then we cannot calculate tne slope.
        if (values.length <= 2)
            return;
            
        setSampleData();
        double[] regressionParams = ols.estimateRegressionParameters();
        if (regressionParams.length >= 2) {
            // [0] is the constant (zero'th order)
            // [1] is the first order , which we use as the slope.
            final double slope = regressionParams[1];
            if (Double.isFinite(slope))
                result.addProperty(type.name(), slope);
        }
        values = null;
    }
}
