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
package org.apache.edgent.topology.json;

import java.nio.charset.StandardCharsets;

import org.apache.edgent.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utilities for use of JSON and Json Objects in a streaming topology.
 */
public class JsonFunctions {

    private static final JsonElement ZERO_ELEMENT = new JsonParser().parse("0");
    private static final Function<JsonObject,JsonElement> ZERO = jo -> ZERO_ELEMENT;

    /**
     * Get the JSON for a JsonObject.
     * 
     * <p>Returns a Function whose {@code apply(JsonObject jo)} returns the JSON
     * for the {@code jo}.
     * 
     * @return the Function
     */
    public static Function<JsonObject,String> asString() {
        return jo -> jo.toString();
    }

    /**
     * Create a new JsonObject from JSON.
     * 
     * <p>Returns a Function whose {@code apply(String json)} creates a JsonObject
     * from the {@code json}.
     * 
     * @return the Function
     */
    public static Function<String,JsonObject> fromString() {
        JsonParser jp = new JsonParser();
        return json -> jp.parse(json).getAsJsonObject();
    }

    /**
     * Get the UTF-8 bytes representation of the JSON for a JsonObject.
     * 
     * <p>Returns a Function whose {@code apply(JsonObject jo)} returns
     * the UTF-8 bytes for the JSON of {@code jo}.
     * 
     * @return the Function
     */
    public static Function<JsonObject,byte[]> asBytes() {
        return jo -> jo.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Create a new JsonObject from the UTF8 bytes representation of JSON.
     * 
     * <p>Returns a Function whose {@code apply(byte[] bytes)} returns
     * a JsonObject from the {@code bytes}.
     * 
     * @return the Function
     */
    public static Function<byte[],JsonObject> fromBytes() {
        JsonParser jp = new JsonParser();
        return jsonbytes -> jp.parse(new String(jsonbytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  
    /**
     * Returns a constant function that returns a zero (0) JsonElement.
     * 
     * <p>Useful for an unpartitioned {@code TWindow<JsonObject,JsonElement>}. 
     * 
     * @return Constant function that returns a zero (0) JsonElement.
     */
    public static Function<JsonObject,JsonElement> unpartitioned() {
      return ZERO;
    }

    /**
     * Create a JsonObject with a {@code Number} property.
     * 
     * <p>Returns a Function whose {@code apply(T v)} returns a JsonObject having 
     * a single property named {@code propName} with the value of {@code v}.
     * 
     * @param propName property name
     * @return the Function
     */
    public static <T extends Number> Function<T,JsonObject> valueOfNumber(String propName) {
      return v -> {
        JsonObject jo = new JsonObject();
        jo.addProperty(propName, v);
        return jo;
      };
    }
    
    /**
     * Create a JsonObject with a {@code Boolean} property.
     * 
     * <p>Returns a Function whose {@code apply(Boolean v)} creates a new JsonObject having 
     * a single property named {@code propName} with the value of {@code v}.
     *  
     * @param propName property name
     * @return the Function
     */
    public static Function<Boolean,JsonObject> valueOfBoolean(String propName) {
      return v -> {
        JsonObject jo = new JsonObject();
        jo.addProperty(propName, v);
        return jo;
      };
    }
    
    /**
     * Create a JsonObject with a {@code String} property.
     * 
     * <p>Returns a Function whose {@code apply(String v)} creates a new JsonObject having 
     * a single property named {@code propName} with the value of {@code v}.
     * 
     * @param propName property name
     * @return the Function
     */
    public static Function<String,JsonObject> valueOfString(String propName) {
      return v -> {
        JsonObject jo = new JsonObject();
        jo.addProperty(propName, v);
        return jo;
      };
    }
    
    /**
     * Create a JsonObject with a {@code Character} property.
     * 
     * <p>Returns a Function whose {@code apply(Character v)} creates a new JsonObject having 
     * a single property named {@code propName} with the value of {@code v}.
     * 
     * @param propName property name
     * @return the Function
     */
    public static Function<Character,JsonObject> valueOfCharacter(String propName) {
      return v -> {
        JsonObject jo = new JsonObject();
        jo.addProperty(propName, v);
        return jo;
      };
    }

}
