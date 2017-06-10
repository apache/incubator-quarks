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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.apache.edgent.analytics.math3.json.JsonAnalytics;
import org.apache.edgent.analytics.math3.json.JsonUnivariateAggregate;
import org.apache.edgent.analytics.math3.stat.Regression;
import org.apache.edgent.analytics.math3.stat.Statistic;
import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StatisticsTest  extends DirectTopologyTestBase {
	@Test
	public void testMin() throws Exception {
	    Topology topology = newTopology("testMin");
	    
	    TStream<JsonObject> aggregate = aggregate(topology, Statistic.MIN);
	    
        Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 11);
        Condition<List<JsonObject>> contents = topology.getTester().streamContents(aggregate);
        complete(topology, count);
        assertTrue(count.valid());
        
        List<JsonObject> tuples = contents.getResult();
        assertEquals(11, tuples.size());
        
        assertOutputStructure(tuples, Statistic.MIN);
        
        // "A1", "B7", "C4", "A4", "B3", "C99", "A102", "B43", "B13.0", "A0", "C700"
        assertResult(tuples, Statistic.MIN, 0, "A", 1.0);
        assertResult(tuples, Statistic.MIN, 1, "B", 7.0);
        assertResult(tuples, Statistic.MIN, 2, "C", 4.0);
        
        assertResult(tuples, Statistic.MIN, 3, "A", 1.0);
        assertResult(tuples, Statistic.MIN, 4, "B", 3.0);
        assertResult(tuples, Statistic.MIN, 5, "C", 4.0);
        
        assertResult(tuples, Statistic.MIN, 6, "A", 4.0);
        assertResult(tuples, Statistic.MIN, 7, "B", 3.0);
        assertResult(tuples, Statistic.MIN, 8, "B", 13.0);
        
        assertResult(tuples, Statistic.MIN, 9, "A", 0.0);
        assertResult(tuples, Statistic.MIN, 10, "C", 99.0);
	}
	
    @Test
    public void testMaxMean() throws Exception {
        Topology topology = newTopology("testMaxMean");

        TStream<JsonObject> aggregate = aggregate(topology, Statistic.MAX, Statistic.MEAN);

        Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 11);
        Condition<List<JsonObject>> contents = topology.getTester().streamContents(aggregate);
        complete(topology, count);
        assertTrue(count.valid());

        List<JsonObject> tuples = contents.getResult();
        assertEquals(11, tuples.size());

        assertOutputStructure(tuples, Statistic.MAX, Statistic.MEAN);

        // "A1", "B7", "C4", "A4", "B3", "C99", "A102", "B43", "B13.0", "A0",
        // "C700"
        assertResult(tuples, Statistic.MAX, 0, "A", 1.0);
        assertResult(tuples, Statistic.MAX, 1, "B", 7.0);
        assertResult(tuples, Statistic.MAX, 2, "C", 4.0);

        assertResult(tuples, Statistic.MAX, 3, "A", 4.0);
        assertResult(tuples, Statistic.MAX, 4, "B", 7.0);
        assertResult(tuples, Statistic.MAX, 5, "C", 99.0);

        assertResult(tuples, Statistic.MAX, 6, "A", 102.0);
        assertResult(tuples, Statistic.MAX, 7, "B", 43.0);
        assertResult(tuples, Statistic.MAX, 8, "B", 43.0);

        assertResult(tuples, Statistic.MAX, 9, "A", 102.0);
        assertResult(tuples, Statistic.MAX, 10, "C", 700.0);
        
        assertResult(tuples, Statistic.MEAN, 0, "A", 1.0);
        assertResult(tuples, Statistic.MEAN, 1, "B", 7.0);
        assertResult(tuples, Statistic.MEAN, 2, "C", 4.0);

        assertResult(tuples, Statistic.MEAN, 3, "A", 2.5);
        assertResult(tuples, Statistic.MEAN, 4, "B", 5.0);
        assertResult(tuples, Statistic.MEAN, 5, "C", 51.5);

        assertResult(tuples, Statistic.MEAN, 6, "A", 53.0);
        assertResult(tuples, Statistic.MEAN, 7, "B", 23.0);
        assertResult(tuples, Statistic.MEAN, 8, "B", 28.0);

        assertResult(tuples, Statistic.MEAN, 9, "A", 51.0);
        assertResult(tuples, Statistic.MEAN, 10, "C", 399.5);
    }
    
    @Test
    public void testSlope() throws Exception {
        Topology topology = newTopology("testSlope");
        
        TStream<JsonObject> aggregate = aggregate(topology, Regression.SLOPE);
        
        Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 11);
        Condition<List<JsonObject>> contents = topology.getTester().streamContents(aggregate);
        complete(topology, count);
        assertTrue(count.valid());
        
        List<JsonObject> tuples = contents.getResult();
        assertEquals(11, tuples.size());
                
        // "A1", "B7", "C4", "A4", "B3", "C99", "A102", "B43", "B13.0", "A0", "C700"
        assertResult(tuples, Regression.SLOPE, 0, "A", null);
        assertResult(tuples, Regression.SLOPE, 1, "B", null);
        assertResult(tuples, Regression.SLOPE, 2, "C", null);
        
        assertResult(tuples, Regression.SLOPE, 3, "A", 3.0);
        assertResult(tuples, Regression.SLOPE, 4, "B", -4.0);
        assertResult(tuples, Regression.SLOPE, 5, "C", 95.0);
        
        assertResult(tuples, Regression.SLOPE, 6, "A", 98.0);
        assertResult(tuples, Regression.SLOPE, 7, "B", 40.0);
        assertResult(tuples, Regression.SLOPE, 8, "B", -30.0);
        
        assertResult(tuples, Regression.SLOPE, 9, "A", -102.0);
        assertResult(tuples, Regression.SLOPE, 10, "C", 601.0);
    }
    
    @Test
    public void testMvMaxMean() throws Exception {
        Topology topology = newTopology("testMvMaxMean");

        TStream<JsonObject> aggregate = mvAggregate(topology, Statistic.MAX, Statistic.MEAN);

        Condition<Long> count = topology.getTester().atLeastTupleCount(aggregate, 11);
        Condition<List<JsonObject>> contents = topology.getTester().streamContents(aggregate);
        complete(topology, count);
        assertTrue(count.valid());

        List<JsonObject> tuples = contents.getResult();
        assertEquals(11, tuples.size());

        assertMvOutputStructure(tuples, Statistic.MAX, Statistic.MEAN);

        // "A1", "B7", "C4", "A4", "B3", "C99", "A102", "B43", "B13.0", "A0",
        // "C700"
        assertMvResult(tuples, Statistic.MAX, 0, "A", 1.0);
        assertMvResult(tuples, Statistic.MAX, 1, "B", 7.0);
        assertMvResult(tuples, Statistic.MAX, 2, "C", 4.0);

        assertMvResult(tuples, Statistic.MAX, 3, "A", 4.0);
        assertMvResult(tuples, Statistic.MAX, 4, "B", 7.0);
        assertMvResult(tuples, Statistic.MAX, 5, "C", 99.0);

        assertMvResult(tuples, Statistic.MAX, 6, "A", 102.0);
        assertMvResult(tuples, Statistic.MAX, 7, "B", 43.0);
        assertMvResult(tuples, Statistic.MAX, 8, "B", 43.0);

        assertMvResult(tuples, Statistic.MAX, 9, "A", 102.0);
        assertMvResult(tuples, Statistic.MAX, 10, "C", 700.0);
        
        assertMvResult(tuples, Statistic.MEAN, 0, "A", 1.0);
        assertMvResult(tuples, Statistic.MEAN, 1, "B", 7.0);
        assertMvResult(tuples, Statistic.MEAN, 2, "C", 4.0);

        assertMvResult(tuples, Statistic.MEAN, 3, "A", 2.5);
        assertMvResult(tuples, Statistic.MEAN, 4, "B", 5.0);
        assertMvResult(tuples, Statistic.MEAN, 5, "C", 51.5);

        assertMvResult(tuples, Statistic.MEAN, 6, "A", 53.0);
        assertMvResult(tuples, Statistic.MEAN, 7, "B", 23.0);
        assertMvResult(tuples, Statistic.MEAN, 8, "B", 28.0);

        assertMvResult(tuples, Statistic.MEAN, 9, "A", 51.0);
        assertMvResult(tuples, Statistic.MEAN, 10, "C", 399.5);
    }
	
	private static void assertResult(List<JsonObject> tuples, JsonUnivariateAggregate stat, int index, String key, Double value) {
	    JsonObject tuple = tuples.get(index);
	    assertEquals(key, tuple.get("id").getAsString());
	    
	    JsonObject agg = tuple.getAsJsonObject("value");
        if (value != null) {
            double result = agg.get(stat.name()).getAsDouble();
            assertEquals("index:" + index, value, result, 0.01);
        } else {
            assertFalse(agg.has(stat.name()));
        }
	}
  
  private static void assertMvResult(List<JsonObject> tuples, JsonUnivariateAggregate stat, int index, String key, Double value) {
      JsonObject tuple = tuples.get(index);
      assertEquals(key, tuple.get("id").getAsString());
      
      if (value != null) {
        Double result = JsonAnalytics.getMvAggregate(tuple, "aggResults", "value", stat).getAsDouble();
        assertEquals("index:" + index + " value "+stat, value, result, 0.01);
  
        Double result2 = JsonAnalytics.getMvAggregate(tuple, "aggResults", "value2", stat).getAsDouble();
        assertEquals("index:" + index + " value2 "+stat, value+1000, result2, 0.01);
      }
      else {
        assertFalse("index:" + index + " value "+stat, JsonAnalytics.hasMvAggregate(tuple, "aggResults", "value", stat));
        assertFalse("index:" + index + " value2 "+stat, JsonAnalytics.hasMvAggregate(tuple, "aggResults", "value2", stat));
      }
  }
	
	public static void assertOutputStructure(List<JsonObject> tuples, JsonUnivariateAggregate ... stats) {
	    for (JsonObject j : tuples) {
	        assertTrue(j.has("id")); // Value of the key
	        assertTrue(j.has("value")); // Value of the key
	        
	        JsonObject v = j.getAsJsonObject("value");
	        for (JsonUnivariateAggregate stat : stats) {
	            assertTrue(v.has(stat.name()));
	        }
	    }
	}
  
  public static void assertMvOutputStructure(List<JsonObject> tuples, JsonUnivariateAggregate ... stats) {
      for (JsonObject j : tuples) {
          assertTrue(j.has("id")); // Value of the key
          assertTrue(j.has("aggResults")); // Value of the key
          
          for (JsonUnivariateAggregate stat : stats) {
            assertTrue("value "+stat, JsonAnalytics.hasMvAggregate(j, "aggResults", "value", stat));
          }
          for (JsonUnivariateAggregate stat : stats) {
            assertTrue("value2 "+stat, JsonAnalytics.hasMvAggregate(j, "aggResults", "value2", stat));
          }
      }
  }
	
	public static TStream<JsonObject> aggregate(Topology topology, JsonUnivariateAggregate ... stats) {
	       TStream<JsonObject> sourceData = sourceData(topology);
	        
	        TWindow<JsonObject, JsonElement> window = sourceData.last(2, j -> j.get("id"));
	        
	        return JsonAnalytics.aggregate(window, "id", "value",
	                j -> j.get("value").getAsDouble(), stats);
	}
	
	public static TStream<JsonObject> sourceData(Topology topology)
	{
	       TStream<String> seed = topology.strings("A1", "B7", "C4", "A4", "B3", "C99", "A102", "B43", "B13", "A0", "C700");
	        
	        return seed.map(s -> {
	            JsonObject j = new JsonObject();
	            j.addProperty("id", s.substring(0, 1));
	            j.addProperty("value", Integer.valueOf(s.substring(1)));
	            return j;
	        });
	}
  
  public static TStream<JsonObject> mvAggregate(Topology topology, JsonUnivariateAggregate ... stats) {
         TStream<JsonObject> sourceData = sourceMvData(topology);
          
          TWindow<JsonObject, JsonElement> window = sourceData.last(2, j -> j.get("id"));
          
          List<Pair<String, JsonUnivariateAggregate[]>> aggSpecs = new ArrayList<>();
          aggSpecs.add(JsonAnalytics.mkAggregationSpec("value", stats));
          aggSpecs.add(JsonAnalytics.mkAggregationSpec("value2", stats));
          
          return JsonAnalytics.mvAggregate(window, "id", "aggResults", aggSpecs);
  }
  
  /*
   * same JsonObject as sourceData() but with an additional 
   * "value2" variable whose value is the the "value" variable's value + 1000 
   */
  public static TStream<JsonObject> sourceMvData(Topology topology)
  {
    return sourceData(topology)
        .map(jo -> {
          jo.addProperty("value2", jo.get("value").getAsLong() + 1000);
          return jo;
        });
  }
  
}
