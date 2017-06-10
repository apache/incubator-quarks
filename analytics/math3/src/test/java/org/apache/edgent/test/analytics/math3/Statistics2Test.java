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
package org.apache.edgent.test.analytics.math3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.apache.edgent.analytics.math3.Aggregations;
import org.apache.edgent.analytics.math3.MvResultMap;
import org.apache.edgent.analytics.math3.ResultMap;
import org.apache.edgent.analytics.math3.UnivariateAggregate;
import org.apache.edgent.analytics.math3.stat.Regression2;
import org.apache.edgent.analytics.math3.stat.Statistic2;
import org.apache.edgent.function.Functions;
import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

import com.google.gson.JsonObject;

/** test Aggregations, Statistics2 and Regression2 */
public class Statistics2Test  extends DirectTopologyTestBase {
  
  // Expected results for **continuous last-2** aggregations of (1, 4, 102, 0) for the various stats.
  protected static Map<UnivariateAggregate,Double[]> STAT_RESULTS = new HashMap<>();
  static {
    STAT_RESULTS.put(
        Statistic2.COUNT,
        new Double[] {
            1.0,  // 1
            2.0,  // 1,4  
            2.0,  // 4,102
            2.0   // 102,0
        });
    STAT_RESULTS.put(
        Statistic2.MIN,
        new Double[] {
            1.0,  // 1
            1.0,  // 1,4  
            4.0,  // 4,102
            0.0   // 102,0
        });
    STAT_RESULTS.put(
        Statistic2.MAX,
        new Double[] {
            1.0,   // 1
            4.0,   // 1,4  
            102.0, // 4,102
            102.0  // 102,0
        });
    STAT_RESULTS.put(
        Statistic2.SUM,
        new Double[] {
            1.0,   // 1
            5.0,   // 1,4  
            106.0, // 4,102
            102.0  // 102,0
        });
    STAT_RESULTS.put(
        Statistic2.MEAN,
        new Double[] {
            1.0,   // 1
            2.5,   // 1,4  
            53.0,  // 4,102
            51.0   // 102,0
        });
    STAT_RESULTS.put(
        Statistic2.STDDEV,
        new Double[] {
            0.0,   // 1
            2.12,  // 1,4  
            69.29, // 4,102
            72.12  // 102,0
        });
    STAT_RESULTS.put(
        Regression2.SLOPE,
        new Double[] {
            null,  // 1
            3.0,   // 1,4  
            98.0,  // 4,102
            -102.0 // 102,0
        });
  }
  
  /* test Aggregations.sum() */
  @Test
  public void testSum() throws Exception {
    Double act = Aggregations.sum(Arrays.asList(1, 2, 3.8));  // check with mixed Number
    Double exp = 1 + 2 + 3.8;
    assertEquals(exp, act, 0.01);

    act = Aggregations.sum(Arrays.asList());
    exp = 0.0;
    assertEquals(exp, act, 0.01);
  }
  
  /* test Aggregations.sumInts() */
  @Test
  public void testSumInts() throws Exception {
    long act = Aggregations.sumInts(Arrays.asList(1, 2, 3));
    long exp = 1 + 2 + 3;
    assertEquals(exp, act);
    
    act = Aggregations.sumInts(Arrays.asList());
    exp = 0;
    assertEquals(exp, act);
  }
  
  /* util to test Aggregations.aggregate(List<Number>, stat) */
  protected void aggregate(UnivariateAggregate stat, Double[] expResults) throws Exception {
    
    // for "continuous last-2" style inputs of (1, 4, 102, 0)
	  
	  int i = 0;
	  Double result;
	  
    result = Aggregations.aggregate(Arrays.asList(1), stat);
    assertResult(i, stat, expResults[i++], result);
    
    result = Aggregations.aggregate(Arrays.asList(1,4), stat);
    assertResult(i, stat, expResults[i++], result);
    
    result = Aggregations.aggregate(Arrays.asList(4,102), stat);
    assertResult(i, stat, expResults[i++], result);
    
    result = Aggregations.aggregate(Arrays.asList(102,0), stat);
    assertResult(i, stat, expResults[i++], result);
  }
  
  /* util to test Aggregations.aggregate(List<T>, getter, stat) */
  protected void aggregateT(UnivariateAggregate stat, Double[] expResults) throws Exception {
    
    // for "continuous last-2" style inputs of (1, 4, 102, 0)
    
    int i = 0;
    Double result;
    List<Object> values;
    
    values = Arrays.asList(1);
    result = Aggregations.aggregate(values, o -> Double.valueOf(o.toString()), stat);
    assertResult(i, stat, expResults[i++], result);
    
    values = Arrays.asList(1,4);
    result = Aggregations.aggregate(values, o -> Double.valueOf(o.toString()), stat);
    assertResult(i, stat, expResults[i++], result);
    
    values = Arrays.asList(4,102);
    result = Aggregations.aggregate(values, o -> Double.valueOf(o.toString()), stat);
    assertResult(i, stat, expResults[i++], result);
    
    values = Arrays.asList(102,0);
    result = Aggregations.aggregate(values, o -> Double.valueOf(o.toString()), stat);
    assertResult(i, stat, expResults[i++], result);
  }

  /* util to test Aggregations.aggregateN(List<Number>, stats) */
  protected void aggregateN(UnivariateAggregate[] stats) throws Exception {
    
    // for "continuous last-2" style inputs of (1, 4, 102, 0)
    
    int i = 0;
    ResultMap result;
    
    result = Aggregations.aggregateN(Arrays.asList(1), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    result = Aggregations.aggregateN(Arrays.asList(1,4), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    result = Aggregations.aggregateN(Arrays.asList(4,102), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    result = Aggregations.aggregateN(Arrays.asList(102,0), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
  }
  
  /* util to test Aggregations.aggregateN(List<T>, getter, stats) */
  protected void aggregateNT(UnivariateAggregate[] stats) throws Exception {
    
    // for "continuous last-2" style inputs of (1, 4, 102, 0)
    
    int i = 0;
    ResultMap result;
    List<Object> values;
    
    values = Arrays.asList(1);
    result = Aggregations.aggregateN(values, o -> Double.valueOf(o.toString()), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    values = Arrays.asList(1,4);
    result = Aggregations.aggregateN(values, o -> Double.valueOf(o.toString()), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    values = Arrays.asList(4,102);
    result = Aggregations.aggregateN(values, o -> Double.valueOf(o.toString()), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
    
    values = Arrays.asList(102,0);
    result = Aggregations.aggregateN(values, o -> Double.valueOf(o.toString()), stats);
    assertResult(i++, stats, STAT_RESULTS, result);
  }
	
  protected static void assertResult(int index, UnivariateAggregate stat, Double exp, Double act) {
	  if (exp == null && act == null)
	    return;  // match
	  assertFalse("index="+index+" "+stat.name()+" exp="+exp+" act="+act, exp == null || act == null);
    assertEquals("index="+index+" "+stat.name(), exp, act, 0.01);
	}
  
  protected static void assertResult(int i, UnivariateAggregate[] stats, Map<UnivariateAggregate,Double[]> expResults, ResultMap actResults) {
    for (UnivariateAggregate stat : stats) {
      Double act = actResults.get(stat);
      Double exp = expResults.get(stat)[i]; 
      
      assertResult(i, stat, exp, act);
    }
  }
	
  /* util to test Aggregations.aggregate(List<Number>, stat) */
  protected void testAggregate(UnivariateAggregate stat) throws Exception {
    aggregate(stat, STAT_RESULTS.get(stat));
  }
  
  @Test
  public void testCOUNT() throws Exception {
    testAggregate(Statistic2.COUNT);
  }
  
  @Test
  public void testMIN() throws Exception {
    testAggregate(Statistic2.MIN);
  }
  
  @Test
  public void testMAX() throws Exception {
    testAggregate(Statistic2.MAX);
  }
  
  @Test
  public void testSUM() throws Exception {
    testAggregate(Statistic2.SUM);
  }
  
  @Test
  public void testMEAN() throws Exception {
    testAggregate(Statistic2.MEAN);
  }
  
  @Test
  public void testSTDDEV() throws Exception {
    testAggregate(Statistic2.STDDEV);
  }
  
  @Test
  public void testSLOPE() throws Exception {
    testAggregate(Regression2.SLOPE);
  }
  
  @Test
  public void testEmptyAggregate() throws Exception {
    Double result;
    
    result = Aggregations.aggregate(Collections.emptyList(), Statistic2.COUNT);
    assertResult(0, Statistic2.COUNT, null, result);

    result = Aggregations.aggregate(Collections.emptyList(), Statistic2.MIN);
    assertResult(0, Statistic2.MIN, null, result);

    // ...
    
    ResultMap resultMap =  Aggregations.aggregateN(Collections.emptyList(), Statistic2.MIN);
    assertTrue(resultMap.isEmpty());
    
    resultMap =  Aggregations.aggregateN(Arrays.asList(1));
    assertTrue(resultMap.isEmpty());
    
    resultMap =  Aggregations.aggregateN(Arrays.asList(1), new UnivariateAggregate[0]);
    assertTrue(resultMap.isEmpty());
  }
  
  @Test
  public void testAggregateT() throws Exception {
    aggregateT(Statistic2.MIN, STAT_RESULTS.get(Statistic2.MIN));
  }
  
  /* test Aggregations.aggregateN(List<Number>, stats) */
  @Test
  public void testAggregateN() throws Exception {
    aggregateN(STAT_RESULTS.keySet().toArray(new UnivariateAggregate[0]));
  }
  
  /* test Aggregations.aggregateN(List<T>, getter, stats) */
  @Test
  public void testAggregateNT() throws Exception {
    aggregateNT(STAT_RESULTS.keySet().toArray(new UnivariateAggregate[0]));
  }
  
  /* test Aggregations.newResultToJson() */
  @Test
  public void testNewResultsToJson() throws Exception {
    ResultMap result = new ResultMap();
    result.put(Statistic2.MIN, 2.5);
    result.put(Statistic2.MAX, 4.5);
    
    JsonObject jo = ResultMap.toJsonObject().apply(result);
    
    assertTrue(jo.get(Statistic2.MIN.name()) != null);
    assertEquals(jo.get(Statistic2.MIN.name()).getAsDouble(), 2.5, 0.01);
    assertTrue(jo.get(Statistic2.MAX.name()) != null);
    assertEquals(jo.get(Statistic2.MAX.name()).getAsDouble(), 4.5, 0.01);
  }
  
  /* test Aggregations.newMvResultToJson() */
  @Test
  public void testNewMvResultsToJson() throws Exception {
    ResultMap var1result = new ResultMap();
    var1result.put(Statistic2.MIN, 2.5);
    var1result.put(Statistic2.MAX, 4.5);
    
    ResultMap var2result = new ResultMap();
    var2result.put(Statistic2.SUM, 27.1);

    MvResultMap result = new MvResultMap();
    result.put("var1", var1result);
    result.put("var2", var2result);

    JsonObject jo = MvResultMap.toJsonObject().apply(result);

    assertTrue(jo.get("var1") != null);
    JsonObject joVar1 = jo.get("var1").getAsJsonObject();
    assertTrue(joVar1.get(Statistic2.MIN.name()) != null);
    assertEquals(joVar1.get(Statistic2.MIN.name()).getAsDouble(), 2.5, 0.01);
    assertTrue(joVar1.get(Statistic2.MAX.name()) != null);
    assertEquals(joVar1.get(Statistic2.MAX.name()).getAsDouble(), 4.5, 0.01);
    
    assertTrue(jo.get("var2") != null);
    JsonObject joVar2 = jo.get("var2").getAsJsonObject();
    assertTrue(joVar2.get(Statistic2.SUM.name()) != null);
    assertEquals(joVar2.get(Statistic2.SUM.name()).getAsDouble(), 27.1, 0.01);
  }

  /* test Aggregations.aggregateN(list, stats) in a Stream/Window context */
  @Test
  public void testAggregateNStream() throws Exception {
    // Aggregations.aggregate are ignorant of Stream/Window but
    // to be safe verify a full Stream/Window based use case works
    
    UnivariateAggregate[] stats = STAT_RESULTS.keySet().toArray(new UnivariateAggregate[0]);
    
    Topology topology = newTopology("testAggregateNStream");
    
    // (1, 4, 102, 0)
    TStream<Integer> sourceData = sourceData(topology);
    
    TWindow<Integer, Integer> window = sourceData.last(2, Functions.unpartitioned());
    
    TStream<ResultMap> aggregate = window.aggregate( (list,partition) -> {
        return Aggregations.aggregateN(list, stats);
    });
    
    Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 4);
    Condition<List<ResultMap>> contents = topology.getTester().streamContents(aggregate);
    complete(topology, count);
    assertTrue(count.valid());
      
    List<ResultMap> tuples = contents.getResult();
    assertEquals(4, tuples.size());
    
    for (int i = 0; i < tuples.size(); i++) {
      assertResult(i, stats, STAT_RESULTS, tuples.get(i));
    }
  }

  /* test Aggregations.aggregateN(list, stats) in a multivariable Stream/Window context */
  @Test
  public void testMvAggregateNStream() throws Exception {
    // Aggregations.aggregate are ignorant of Stream/Window but
    // to be safe verify a full Stream/Window based use case works (TStream<MvResultMap>)
    
    UnivariateAggregate[] stats = { Statistic2.MIN, Statistic2.MAX };
    
    Map<UnivariateAggregate,Double[]> var2_STAT_RESULTS = new HashMap<>();
    var2_STAT_RESULTS.put(Statistic2.MIN, new Double[] {1001.0, 1001.0, 1004.0, 1000.0});
    var2_STAT_RESULTS.put(Statistic2.MAX, new Double[] {1001.0, 1004.0, 1102.0, 1102.0});
    
    
    Topology topology = newTopology("testAggregateNStream");
    
    // (1, 4, 102, 0)
    TStream<SensorReadings> sourceData = sourceData(topology)
        .map(i -> new SensorReadings(i, i+1000));
    
    TWindow<SensorReadings, Integer> window = sourceData.last(2, Functions.unpartitioned());
    
    TStream<MvResultMap> aggregate = window.aggregate( (list,partition) -> {
        ResultMap var1result = Aggregations.aggregateN(list, sr -> sr.var1, stats);
        ResultMap var2result = Aggregations.aggregateN(list, sr -> sr.var2, stats);
        MvResultMap result = new MvResultMap();
        result.put("var1", var1result);
        result.put("var2", var2result);
        return result;
    });
    
    Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 4);
    Condition<List<MvResultMap>> contents = topology.getTester().streamContents(aggregate);
    complete(topology, count);
    assertTrue(count.valid());
      
    List<MvResultMap> tuples = contents.getResult();
    assertEquals(4, tuples.size());
    
    for (int i = 0; i < tuples.size(); i++) {
      MvResultMap results = tuples.get(i);
      assertResult(i, stats, STAT_RESULTS, results.get("var1"));
      assertResult(i, stats, var2_STAT_RESULTS, results.get("var2"));
    }
  }
  
  protected static class SensorReadings {
    int var1;
    int var2;
    SensorReadings(int var1, int var2) {
      this.var1 = var1;
      this.var2 = var2;
    }
  }
  
  protected static TStream<Integer> sourceData(Topology topology)
  {
      return topology.of(1, 4, 102, 0);
  }
  
}
