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

import org.apache.commons.math3.stat.descriptive.AbstractStorelessUnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;

/**
 * The number of items in the collection being aggregated.
 * 
 * Need this semi-hack to be able to capture the number
 * of items in an aggregation in a ResultMap.
 */
class Count extends AbstractStorelessUnivariateStatistic {
  int n;

  @Override
  public long getN() {
    return n;
  }

  @Override
  public void clear() {
    n = 0;
  }

  @Override
  public StorelessUnivariateStatistic copy() {
    return new Count();
  }

  @Override
  public double getResult() {
    return n;
  }

  @Override
  public void increment(double arg0) {
    n++;
  }

}
