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
package org.apache.edgent.analytics.math3.json;

import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.apache.edgent.analytics.math3.Aggregations;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.ToDoubleFunction;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Apache Common Math analytics for streams with JSON tuples.
 *
 * @see Aggregations
 */
public class JsonAnalytics {
    
    /**
     * Aggregate against a single {@code Numeric} variable contained in an JSON object.
     * 
     * The returned stream contains a tuple for each execution performed against a window partition.
     * The tuple is a {@code JsonObject} containing:
     * <UL>
     * <LI> Partition key of type {@code K} as a property with key {@code resultPartitionProperty}. </LI>
     * <LI> Aggregation results as a {@code JsonObject} as a property with key {@code valueProperty}.
     * This results object contains the results of all aggregations defined by {@code aggregates} against
     * {@code double} property with key {@code valueProperty}.
     * <BR>
     * Each {@link JsonUnivariateAggregate} declares how it represents its aggregation in this result
     * object.
     * </LI>
     * </UL>
     * <P>
     * For example if the window contains these three tuples (pseudo JSON) for
     * partition 3:
     * <BR>
     * <code>{id=3,reading=2.0}, {id=3,reading=2.6}, {id=3,reading=1.8}</code>
     * <BR>
     * the resulting aggregation for the stream returned by:
     * <BR>
     * {@code aggregate(window, "id", "reading", Statistic.MIN, Statistic.MAX)}
     * <BR>
     * would contain this tuple with the maximum and minimum values in the {@code reading}
     * JSON object:
     * <BR>
     * <code>{id=3, reading={MIN=1.8, MAX=1.8}}</code>
     * </P>
     * @param <K> Partition type
     * 
     * @param window Window to aggregate over.
     * @param resultPartitionProperty Property to store the partition key in tuples on the returned stream.
     * @param valueProperty JSON property containing the value to aggregate.
     * @param aggregates Which aggregations to be performed.
     * @return Stream that will contain aggregations.
     */
    public static <K extends JsonElement> TStream<JsonObject> aggregate(
            TWindow<JsonObject, K> window,
            String resultPartitionProperty,
            String valueProperty,
            JsonUnivariateAggregate... aggregates) {
        return aggregate(window, resultPartitionProperty, valueProperty, j -> j.get(valueProperty).getAsDouble(), aggregates);

    }
    
    /**
     * Aggregate against a single {@code Numeric} variable contained in an JSON object.
     * 
     * The returned stream contains a tuple for each execution performed against a window partition.
     * The tuple is a {@code JsonObject} containing:
     * <UL>
     * <LI> Partition key of type {@code K} as a property with key {@code resultPartitionProperty}. </LI>
     * <LI> Aggregation results as a {@code JsonObject} as a property with key {@code resultProperty}.
     * This results object contains the results of all aggregations defined by {@code aggregates} against
     * value returned by {@code valueGetter}.
     * <BR>
     * Each {@link JsonUnivariateAggregate} declares how it represents its aggregation in this result
     * object.
     * </LI>
     * </UL>
     * 
     * @param <K> Partition type
     * @param window Window to aggregate over.
     * @param resultPartitionProperty Property to store the partition key in tuples on the returned stream.
     * @param resultProperty Property to store the aggregations in tuples on the returned stream.
     * @param valueGetter How to obtain the single variable from input tuples.
     * @param aggregates Which aggregations to be performed.
     * @return Stream that will contain aggregations.
     */
    public static <K extends JsonElement> TStream<JsonObject> aggregate(
            TWindow<JsonObject, K> window,
            String resultPartitionProperty,
            String resultProperty,
            ToDoubleFunction<JsonObject> valueGetter,
            JsonUnivariateAggregate... aggregates) {

        return window.aggregate(aggregateList(
                resultPartitionProperty,
                resultProperty,
                valueGetter,
                aggregates
                ));
    }
    
    /**
     * Create a Function that aggregates against a single {@code Numeric}
     * variable contained in an JSON object.
     * 
     * Calling {@code apply(List<JsonObject>)} on the returned {@code BiFunction}
     * returns a {@link JsonObject} containing:
     * <UL>
     * <LI> Partition key of type {@code K} as a property with key {@code resultPartitionProperty}. </LI>
     * <LI> Aggregation results as a {@code JsonObject} as a property with key {@code valueProperty}.
     * This results object contains the results of all aggregations defined by {@code aggregates}
     * against the value returned by {@code valueGetter}.
     * <BR>
     * Each {@link JsonUnivariateAggregate} declares how it represents its aggregation in this result
     * object.
     * </LI>
     * </UL>
     * <P>
     * For example if the list contains these three tuples (pseudo JSON) for
     * partition 3:
     * <BR>
     * <code>{id=3,reading=2.0}, {id=3,reading=2.6}, {id=3,reading=1.8}</code>
     * <BR>
     * the resulting aggregation for the JsonObject returned by:
     * <BR>
     * {@code aggregateList("id", "reading", Statistic.MIN, Statistic.MAX).apply(list, 3)}
     * <BR>
     * would be this tuple with the maximum and minimum values in the {@code reading}
     * JSON object:
     * <BR>
     * <code>{id=3, reading={MIN=1.8, MAX=1.8}}</code>
     * </P>
     * @param <K> Partition type
     * 
     * @param resultPartitionProperty Property to store the partition key in tuples on the returned stream.
     * @param resultProperty Property to store the aggregations in the returned JsonObject.
     * @param valueGetter How to obtain the single variable from input tuples.
     * @param aggregates Which aggregations to be performed.
     * @return Function that performs the aggregations.
     */
    public static <K extends JsonElement> 
    BiFunction<List<JsonObject>, K, JsonObject> aggregateList(
            String resultPartitionProperty,
            String resultProperty,
            ToDoubleFunction<JsonObject> valueGetter,
            JsonUnivariateAggregate... aggregates) {

        BiFunction<List<JsonObject>, K, JsonObject> function = (tuples, partition) -> {
            
            final JsonUnivariateAggregator[] aggregators = new JsonUnivariateAggregator[aggregates.length];
            for (int i = 0; i < aggregates.length; i++) {
                aggregators[i] = aggregates[i].get();
            }     
            
            final JsonObject result = new JsonObject();
            result.add(resultPartitionProperty, partition);
            JsonObject aggregateResults = new JsonObject();
            result.add(resultProperty, aggregateResults);

            final int n = tuples.size();
            aggregateResults.addProperty(JsonUnivariateAggregate.N, n);
            
            if (n != 0) {

                for (JsonUnivariateAggregator agg : aggregators) {
                    agg.clear(partition, n);
                }
                for (JsonObject tuple : tuples) {
                    double v = valueGetter.applyAsDouble(tuple);
                    for (JsonUnivariateAggregator agg : aggregators) {
                        agg.increment(v);
                    }
                }
                for (JsonUnivariateAggregator agg : aggregators) {
                    agg.result(partition, aggregateResults);
                }
            }

            return result;
        };
        
        return function;
    }
    
    /**
     * Aggregate against multiple {@code Numeric} variables contained in an JSON object.
     * <P>
     * This is a multi-variable analog of {@link #aggregate(TWindow, String, String, JsonUnivariateAggregate...) aggregate()}
     * </P>
     * <P>
     * See {@link #mvAggregateList(String, String, List) mvAggregateList()} for
     * a description of the aggregation processing and result stream.
     * </P>
     * <P>
     * Sample use:
     * <pre>{@code
     * // Ingest the data.  The JsonObject tuples have properties:
     * //   "id" - the partitionKey
     * //   "tx" - a numeric data variable
     * //   "rx" - a numeric data variable
     * TStream<JsonObject> ingestData = ...
     * 
     * // Define the tuple variables and their aggregations to compute
     * List<Pair<String, JsonUnivariateAggregate[]>> aggSpecs = new ArrayList<>();
     * aggSpecs.add(mkAggregationSpec("tx", Statistics.MIN, Statistics.MAX));
     * aggSpecs.add(mkAggregationSpec("rx", Statistics.MEAN));
     * 
     * // Create the window over which to aggregate
     * TWindow<JsonObject, JsonElement> window = 
     *    ingestData.last(5, TimeUnit.SECONDS, jo -> jo.get("id"));
     * 
     * // Create a stream with the aggregations. The result tuples have properties:
     * //   "id" - the partitionKey
     * //   "aggResults" - the aggregation results
     * TStream<JsonObject> aggResults = 
     *    mvAggregate(window, "id", "aggResults", aggSpecs);
     *    
     * // Create a stream of JsonObject tuples with just the average "rx"
     * TStream<JsonObject> avgRx = aggResults.map(
     *    jo -> {
     *      JsonObject result = new JsonObject();
     *      result.add("id", jo.get("id"))
     *      result.add("avgRx", getMvAggregate(jo, "aggResults", "Rx", Statistic.MEAN);
     *      return result;
     *    });
     * }</pre>
     * 
     * @param window the window to compute aggregations over
     * @param resultPartitionKeyProperty name of the partition key property in the result
     * @param resultProperty name of the aggregation results property in the result
     * @param aggregateSpecs see {@link #mkAggregationSpec(String, JsonUnivariateAggregate...) mkAggregationSpec()}
     * @return TStream<JsonObject> with aggregation results
     * 
     * @see #mvAggregateList(String, String, List) mvAggregateList()
     * @see #mkAggregationSpec(String, JsonUnivariateAggregate...) mkAggregationSpec()
     * @see #getMvAggregate(JsonObject, String, String, JsonUnivariateAggregate) getMvAggregate()
     */
    public static <K extends JsonElement> TStream<JsonObject> mvAggregate(
        TWindow<JsonObject, K> window,
        String resultPartitionKeyProperty,
        String resultProperty,
        List<Pair<String, JsonUnivariateAggregate[]>> aggregateSpecs) {

      return window.aggregate(mvAggregateList(
              resultPartitionKeyProperty,
              resultProperty,
              aggregateSpecs
              ));
    }

    /**
     * Create an aggregation specification.
     * <P>
     * The aggregation specification specifies a variable name and
     * the aggregates to compute on it.
     * </P>
     * <P>
     * The specification can be use with {@link #mvAggregateList(String, String, List) mkAggregateList()}
     * 
     * @param variableName the name of a {@code Numeric} data variable in a JSON object 
     * @param aggregates the aggregates to compute for the variable
     * @return the aggregation specification
     */
    public static Pair<String, JsonUnivariateAggregate[]> 
    mkAggregationSpec(String variableName, JsonUnivariateAggregate... aggregates) {
      return new Pair<String, JsonUnivariateAggregate[]>(variableName, aggregates);
    }
    
    /**
     * Create a Function that aggregates multiple {@code Numeric}
     * variables contained in an JSON object.
     * <P>
     * This is a multi-variable analog of {@link JsonAnalytics#aggregateList(String, String, org.apache.edgent.function.ToDoubleFunction, JsonUnivariateAggregate...) aggregateList()}
     * <P>
     * The overall multi-variable aggregation result is a JSON object
     * with properties:
     * <ul>
     * <li>{@code resultPartionKeyProperty} whose value is the tuple's partition key
     * <li>{@code resultProperty} whose value is a JSON object containing
     *     a property for each variable aggregation.  The property names
     *     correspond to the variable names from the {@code aggregateSpecs}
     *     and the values are the aggregation results for the variable.
     *     The aggregation results for a variable are a JSON object 
     *     having a property for each aggregation name and its value.</li>
     * </ul>
     * <P>
     * For example if the list contains these three tuples (pseudo JSON) for
     * partition 3:
     * <BR>
     * <code>{id=3,tx=2.0,rx=1.0,...}, {id=3,tx=2.6,rx=2.0,...}, {id=3,tx=1.8,rx=3.0,...}</code>
     * <BR>
     * the resulting aggregation JsonObject returned is:
     * <BR>
     * <code>{id=3, aggData={tx={MIN=1.8, MAX=2.6}, rx={MEAN=2.0}}}</code>
     * <BR>
     * for the invocation:
     * <BR>
     * <code>mvAggregateList("id", "aggData", aggSpecs).apply(list, 3))</code>
     * <BR>
     * where {@code aggSpecs} is:
     * <BR>
     * {@code
     * aggSpecs.add(mkAggregationSpec("tx", Statistics.MIN, Statistics.MAX));
     * aggSpecs.add(mkAggregationSpec("rx", Statistics.MEAN));
     * }
     * </P>
     * <P>
     * {@link #getMvAggregate(JsonObject, String, String, JsonUnivariateAggregate) getMvAggregate()}
     * can be used to extract individual aggregate values from the result.
     * </P>
     * 
     * @param <K> Partition Key as a JsonElement
     * 
     * @param resultPartitionKeyProperty name of the partition key property in the result
     * @param resultProperty name of the aggregation results property in the result
     * @param aggregateSpecs see {@link #mkAggregationSpec(String, JsonUnivariateAggregate...) mkAggregationSpec()}
     * @return Function that performs the aggregations.
     * 
     * @see #mkAggregationSpec(String, JsonUnivariateAggregate...) mkAggregationSpec()
     * @see #getMvAggregate(JsonObject, String, String, JsonUnivariateAggregate) getMvAggregate()
     */
    public static <K extends JsonElement> 
    BiFunction<List<JsonObject>, K, JsonObject> mvAggregateList(
        String resultPartitionKeyProperty, String resultProperty,
        List<Pair<String, JsonUnivariateAggregate[]>> aggregateSpecs) {
      
      BiFunction<List<JsonObject>, K, JsonObject> function = 
        (joList, partition) -> {
          JsonObject joResult = new JsonObject();
          joResult.add(resultPartitionKeyProperty, partition);
          
          JsonObject aggregateResults = new JsonObject();
          joResult.add(resultProperty, aggregateResults);
          
          for (Pair<String, JsonUnivariateAggregate[]> p : aggregateSpecs) {
            String variableName = p.getFirst();
            JsonUnivariateAggregate[] aggregates = p.getSecond();
            
            // Compute the aggregates for the variable
            JsonObject jo2 = JsonAnalytics.aggregateList(resultPartitionKeyProperty,
                resultProperty, jo -> jo.get(variableName).getAsDouble(),
                aggregates).apply(joList,  partition);
            
            // Add the variable's aggregates result to the result
            aggregateResults.add(variableName, jo2.get(resultProperty).getAsJsonObject());
          }
        
          return joResult;
        };
        
        return function;
    }
    
    /**
     * Get the value of an aggregate computed by a multi-variable aggregation.
     * <P>
     * This convenience method can be used to extract information from a JSON object
     * created by {@link #mvAggregateList(String, String, List) mvAggregationList()}
     * or {@link #mvAggregate(TWindow, String, String, List) mvAggregate()}
     * </P>
     * <P>
     * Sample use:
     * <pre>{@code
     * ...
     * TStream<JsonObject> aggData = mvAggregate(window, "id", "aggResults", aggSpecs);
     * 
     * // Create a stream of JsonObject tuples with just the average "tx"
     * TStream<JsonObject> avgTx = aggResults.map(
     *    jo -> {
     *      JsonObject result = new JsonObject();
     *      result.add(partitionKeyName, jo.get(partitionKeyName))
     *      result.add("avgTx", getMvAggregate(jo, "aggResults", "tx", Statistic.MEAN);
     *      return result;
     *    });
     * }</pre>
     * 
     * @param jo a JSON object created by {@code mvAggregationList}
     * @param resultProperty the corresponding value passed to {@code mvAggragateList}
     * @param variableName the data variable of interest in the multivariable aggregates
     * @param aggregate the variable's aggregate of interest
     * @return the variable's aggregate's value as a JsonElement
     * @throws RuntimeException if the aggregate isn't present in the result
     * 
     * @see #hasMvAggregate(JsonObject, String, String, JsonUnivariateAggregate) hasAggregate()
     * @see #mvAggregate(TWindow, String, String, List) mvAggregate()
     * @see #mvAggregateList(String, String, List) mvAggregateList()
     */
    public static JsonElement getMvAggregate(JsonObject jo, String resultProperty, String variableName, JsonUnivariateAggregate aggregate) {
      return jo.get(resultProperty).getAsJsonObject()
              .get(variableName).getAsJsonObject()
              .get(aggregate.name());
    }

    /**
     * Check if an aggregation result from a multi-variable aggregation
     * is present.
     * 
     * @param jo a JSON object created by {@code mvAggregationList}
     * @param resultProperty the corresponding value passed to {@code mvAggragateList}
     * @param variableName the data variable of interest in the multivariable aggregates
     * @param aggregate the variable's aggregate of interest
     * @return true if the specified aggregate is present in the jo, false otherwise.
     * 
     * @see #getMvAggregate(JsonObject, String, String, JsonUnivariateAggregate) getMvAggregate()
     */
    public static boolean hasMvAggregate(JsonObject jo, String resultProperty, String variableName, JsonUnivariateAggregate aggregate) {
      JsonElement je = jo.get(resultProperty);
      if (je != null && je.isJsonObject()) {
        JsonObject jo2 = je.getAsJsonObject();
        je = jo2.get(variableName);
        if (je != null && je.isJsonObject()) {
          jo2 = je.getAsJsonObject();
          je = jo2.get(aggregate.name());
          if (je != null)
            return true;
        }
      }
      return false;
    }

}
