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