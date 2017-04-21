package org.apache.edgent.analytics.math3;

import java.util.HashMap;

import org.apache.edgent.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Aggregation results for multiple aggregated variables.
 * 
 * <p>The name of the aggregated variable is typically the key for the variable's {@link ResultMap}.
 */
public class MvResultMap extends HashMap<String,ResultMap> {
  private static final long serialVersionUID = 1L;

  /**
   * Returns a {@link Function} whose {@code apply(MvResultMap)} converts the value
   * to a {@code JsonObject}.
   * 
   * <p>The JsonObject property names are the MvResultMap's keys and the property
   * values are the key's associated ResultMap value as a JsonObject.
   * 
   * <p>An example resulting JsonObject would be 
   * <pre>{ "temperature":{"MEAN":123.75, "MAX":180.5}, "pressure":{"MAX":13.0} }</pre>.
   * 
   * @return the JsonObject
   * 
   * @see ResultMap#toJsonObject()
   */
  public static Function<MvResultMap,JsonObject> toJsonObject() {
    Gson gson = new Gson();
    return (MvResultMap mvResultMap) -> gson.toJsonTree(mvResultMap).getAsJsonObject();
  }
  
  /**
   * Create a new MvResultMap.
   */
  public MvResultMap() {
  }
  
}