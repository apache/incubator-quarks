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
