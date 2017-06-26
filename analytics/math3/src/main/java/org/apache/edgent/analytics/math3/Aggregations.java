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

import java.util.Collection;

import org.apache.edgent.analytics.math3.json.JsonAnalytics;
import org.apache.edgent.analytics.math3.stat.Regression2;
import org.apache.edgent.analytics.math3.stat.Statistic2;
import org.apache.edgent.analytics.math3.utils.Java7Helper;
import org.apache.edgent.function.ToDoubleFunction;
import org.apache.edgent.topology.TWindow;

/**
 * Apache Common Math analytics for Collections.
 * 
 * <p>These operations are typically used when processing a collection of tuples
 * in a non-partitioned or partitioned {@link TWindow}.
 * 
 * <p>Simple sum aggregation functions are also provided (helpful given the avoidance
 * of depending on Java8 Streams).
 * 
 * <p>Example: compute a batched MEAN aggregation for a simple unpartitioned window of
 * numeric {@code TStream<Double>} where the desired result is a {@code TStream<Double>} of
 * the MEAN values:
 * <pre>{@code
 *  TStream<Double> pressureReadings = ...
 *TWindow<Double,Integer> window = pressureReadings.last(5, Functions.unpartitioned());
 *TStream<Double> meanPressureReadings = window.batch(
 *      (list, partition) -> Aggregations.aggregate(list, Statistic2.MEAN));
 * }</pre>
 * 
 * <p>Example: compute the MEAN and SLOPE, capturing the results in a {@link ResultMap}:
 * <pre>{@code
 *  TStream<Double> pressureReadings = ...
 *TWindow<Double,Integer> window = pressureReadings.last(5, Functions.unpartitioned());
 *TStream<ResultMap> meanPressureReadings = window.batch(
 *      (list, partition) -> Aggregations.aggregateN(list, Statistic2.MEAN, Regression2.SLOPE));
 * }</pre>
 * 
 * <p>Example: compute multiple aggregations on multiple variables in a tuple T,
 * capturing the results in a {@link MvResultMap}:
 * <pre><code>
 *class SensorReading { 
 *  double getTemp() {...};
 *  double getPressure() {...};
 *  ... };
 *TStream&lt;SensorReading&gt; readings = ...
 *TWindow&lt;SensorReading,Integer&gt; window = sensorReadings.last(5, Functions.unpartitioned());
 *TStream&lt;MvResultMap&gt; aggregations = window.batch(
 *      (list, partition) -&gt; {
 *        ResultMap pressureResults = Aggregations.aggregateN(list, t -&gt; t.getPressure(), Statistic2.MEAN, Regression2.SLOPE));
 *        ResultMap tempResults = Aggregations.aggregateN(list, t -&gt; t.getTemp(), Statistic2.MAX));
 *        MvResultMap results = Aggregations.newMvResults();
 *        results.put("pressure", pressureResults);
 *        results.put("temp", tempResults);
 *        return results;
 *      });
 * </code></pre>
 *
 * <p>Example: convert a {@code TStream<ResultMap>} or {@code TStream<MvResultMap>} to a JsonObject:
 * 
 * <pre>{@code
 *   TStream<ResultMap> resultMap = ...
 * TStream<JsonObject> joResultMap = resultMap.map(ResultMap.toJsonObject());
 * }</pre>
 * 
 * 
 * <p>Background: 
 * {@link JsonAnalytics} predates this class.  Use of JsonAnalytics for computing
 * Apache Commons Math aggregations requires:
 * <ul>
 * <li>the input stream tuple type must be a JsonObject and the user must identify
 *     the property that contains the value to be aggregated.</li>
 * <li>batched window aggregations are not supported (they could be added).</li>
 * <li>the aggregation result tuple is a JsonObject that holds a user-specified
 *     property for a window partition key and a user-specified property
 *     for a JsonObject holding the UnivariateAggregation results by name (e.g., "MEAN").
 *     Accessing a particular aggregation results in the JsonObject
 *     is somewhat cumbersome.</li>
 * </ul>
 * 
 * <p>As a result, using JsonAnalytics for simple cases can be a bit unintuitive and cumbersome.  
 * 
 * <p>For example, to JsonAnalytics for a simple case of a continuous aggregation
 * of {@code TStream<Double>} =&gt; {@code TStream<Double>} of MEAN values:
 * 
 * <pre>{@code
 *  TStream<Double> pressureReadings = ...
 * 
 * // transform TStream<Double> to TStream<JsonObject>
 * TStream<JsonObject> joPressureReadings = pressureReadings.map(JsonFunctions.valueOfDouble("pressure"));
 * 
 * // aggregate
 * TWindow<JsonObject,JsonElement> window = joPressureReadings.last(5, JsonFunctions.unpartitioned());
 * TStream<JsonObject> results = JsonAnalytics.aggregate(window, "partition", "pressure", Statistic.MEAN);
 *  
 * // transform to TStream<Double> mean results
 * TStream<Double> meanPressureReadings = results.map(jo -> jo.get("pressure").getAsObject().get("MEAN").getAsDouble());
 * }</pre>
 * 
 * <p>Using Aggregations it's simply:
 * 
 * <pre>{@code
 *TWindow<Double,Integer> window = pressureReadings.last(5, Functions.unpartitioned());
 *TStream<Double> meanPressureReadings = window.aggregate(
 *      (list, partition) -> Aggregations.aggregate(list, Statistic2.MEAN));
 * }</pre>
 * 
 * 
 * @see Statistic2
 * @see Regression2
 * @see JsonAnalytics
 */
public class Aggregations {
  
  /** 
   * Perform a sum of numbers treated as double values, accumulating in a double.
   * An empty list yields a 0.0 result.
   * @param list numbers to sum
   * @return the sum
   */
  public static Double sum(Collection<? extends Number> list) {
    double sum = 0.0;
    for (Number v : list) 
      sum += v.doubleValue();
    return sum;
  }
  
  /** 
   * Perform a sum of numbers treated as long values, accumulating in a long.
   * More efficient than sum() for non-floating point values.
   * An empty list yields a 0 result.
   * @param list numbers to sum
   * @return the sum
   */
  public static long sumInts(Collection<? extends Number> list) {
    long sum = 0;
    for (Number v : list) 
      sum += v.longValue();
    return sum;
  }

  /**
   * Perform the specified {@link UnivariateAggregate} on a Collection of {@link Number}.
   * 
   * <p>A null result is returned if the collection is empty.
   * An aggregation result may be null under other conditions,
   * e.g., a Regression2.SLOPE where the minimum number of samples has not been met.
   * 
   * @param c the Collection to aggregate
   * @param aggregate the aggregation to perform
   * @return the aggregation result, may be null.
   */
  public static Double aggregate(Collection<? extends Number> c, UnivariateAggregate aggregate) {
    return aggregateN(c, aggregate).get(aggregate);
  }
  
  /**
   * Perform the specified {@link UnivariateAggregate} on a Collection of {@link Number}.
   *  
   * <p>If the collection is empty an empty ResultMap is returned.
   * The ResultMap does not contain an entry for an aggregation with a null,
   * e.g., a Regression2.SLOPE where the minimum number of samples has not been met.
   * 
   * @param c the Collection to aggregate
   * @param aggregates the aggregations to perform
   * @return a {@link ResultMap} containing the variable's aggregation results
   */
  public static ResultMap aggregateN(Collection<? extends Number> c, UnivariateAggregate... aggregates) {
    return aggregateN(c, num -> num.doubleValue(), aggregates);
  }
  
  /**
   * Perform the specified {@link UnivariateAggregate} a Collection of {@code T}
   * using the specified {@link ToDoubleFunction getter} to extract the
   * variable to aggregate.
   * 
   * <p>A null result is returned if the collection is empty.
   * An aggregation result may be null under other conditions,
   * e.g., a Regression2.SLOPE where the minimum number of samples has not been met.
   * 
   * @param c the Collection to aggregate
   * @param getter function that returns the variable to aggregate from a {@code T}
   * @param aggregate the aggregation to perform
   * @return the aggregation result, may be null.
   */
  public static <T> Double aggregate(Collection<T> c, ToDoubleFunction<T> getter, UnivariateAggregate aggregate) {
    return aggregateN(c, getter, aggregate).get(aggregate);
  }

  /**
   * Perform the specified {@link UnivariateAggregate} a Collection of {@code T}
   * using the specified {@link ToDoubleFunction getter} to extract the
   * variable to aggregate.
   * 
   * <p>If the collection is empty an empty ResultMap is returned.
   * The ResultMap does not contain an entry for an aggregation with a null,
   * e.g., a Regression2.SLOPE where the minimum number of samples has not been met.
   * 
   * @param c the Collection to aggregate
   * @param getter function that returns the variable to aggregate from a {@code T}
   * @param aggregates the aggregations to perform
   * @return a {@link ResultMap} containing the variable's aggregation results
   */
  public static <T> ResultMap aggregateN(Collection<T> c, ToDoubleFunction<T> getter, UnivariateAggregate... aggregates) {

    final int n = c.size();
    final ResultMap result = new ResultMap();
    
    if (n != 0) {
      // get new UnivariateAggregate instances for this aggregation
      final UnivariateAggregator[] aggregators = new UnivariateAggregator[aggregates.length];
      for (int i = 0; i < aggregates.length; i++) {
          aggregators[i] = aggregates[i].get();
      }
      for (UnivariateAggregator agg : aggregators) {
          agg.clear(n);
      }
      for (T value : c) {
          Double d = getter.applyAsDouble(value);
          for (UnivariateAggregator agg : aggregators) {
              agg.increment(d);
          }
      }
      for (UnivariateAggregator agg : aggregators) {
          // do as JsonAnalytics did and omit Nan/Inf results from the map.
          double rv = agg.getResult();
          
          if (Java7Helper.doubleIsFinite(rv))
              result.put(agg.getAggregate(), rv);
      }
    }

    return result;
  }

}
