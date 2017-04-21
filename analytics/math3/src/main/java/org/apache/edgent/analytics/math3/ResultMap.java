package org.apache.edgent.analytics.math3;

import java.util.HashMap;

import org.apache.edgent.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Aggregation results for a single aggregated variable.
 */
public class ResultMap extends HashMap<UnivariateAggregate,Double> {
  private static final long serialVersionUID = 1L;

  /**
   * <p>Returns a {@link Function} whose {@code apply(ResultMap)} converts the value
   * to a {@code JsonObject}.
   * 
   * <p>The JsonObject property names are the ResultMap's keys and the property
   * values are the key's associated Double map value.
   * 
   * <p>An example resulting JsonObject would be 
   * <pre>{ "MEAN":3.75, "MIN":2.0 }</pre>.
   * 
   * @return the JsonObject
   */
  public static Function<ResultMap,JsonObject> toJsonObject() {
    Gson gson = new Gson();
    return (ResultMap resultMap) -> gson.toJsonTree(resultMap).getAsJsonObject();
  }
  
  /**
   * Create a new ResultMap.
   */
  public ResultMap() {
  }
  
}