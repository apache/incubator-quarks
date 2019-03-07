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
package org.apache.edgent.topology;

import static org.junit.Assert.assertEquals;

import org.apache.edgent.function.Function;
import org.apache.edgent.topology.json.JsonFunctions;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonFunctionsTest {
    
    private JsonObject newTestObject() {
        // Just a mix of things so we have reasonable confidence
        // the JsonFunctions are working.
        JsonObject jo = new JsonObject();
        jo.addProperty("boolean", true);
        jo.addProperty("character", 'c');
        jo.addProperty("short", (short)7);
        jo.addProperty("int", 23);
        jo.addProperty("long", 99L);
        jo.addProperty("float", 3.0f);
        jo.addProperty("double", 7.128d);
        jo.addProperty("string", "a string value");
        JsonArray ja = new JsonArray();
        ja.add(new JsonPrimitive(123));
        ja.add(new JsonPrimitive(456));
        jo.add("array", ja);
        JsonObject jo2 = new JsonObject();
        jo2.addProperty("int", 789);
        jo.add("object", jo2);
        return jo;
    }
    
    @Test
    public void testStrings() {
        JsonObject jo1 = newTestObject();
        Function<JsonObject,String> asString = JsonFunctions.asString();
        Function<String,JsonObject> fromString = JsonFunctions.fromString();
        
        String s1 = asString.apply(jo1);
        JsonObject jo2 = fromString.apply(s1);
        
        Assert.assertEquals(jo2, jo1);
    }
    
    @Test
    public void testBytes() {
        JsonObject jo1 = newTestObject();
        Function<JsonObject,byte[]> asBytes = JsonFunctions.asBytes();
        Function<byte[],JsonObject> fromBytes = JsonFunctions.fromBytes();
        
        byte[] b1 = asBytes.apply(jo1);
        JsonObject jo2 = fromBytes.apply(b1);
        
        Assert.assertEquals(jo2, jo1);
    }
    
    @Test
    public void testUnpartitioned() {
        Function<JsonObject,JsonElement> unpartitionedFn = JsonFunctions.unpartitioned();
        Assert.assertEquals(0, unpartitionedFn.apply(new JsonObject()).getAsInt());
    }
    
    @Test
    public void testValueOfNumber() {
      JsonObject joShort = JsonFunctions.valueOfNumber("propName").apply(Short.MAX_VALUE);
      Assert.assertEquals(Short.MAX_VALUE, joShort.get("propName").getAsShort());
      
      JsonObject joInt = JsonFunctions.valueOfNumber("propName").apply(Integer.MAX_VALUE);
      Assert.assertEquals(Integer.MAX_VALUE, joInt.get("propName").getAsInt());
      
      JsonObject joLong = JsonFunctions.valueOfNumber("propName").apply(Long.MAX_VALUE);
      Assert.assertEquals(Long.MAX_VALUE, joLong.get("propName").getAsLong());
      
      JsonObject joFloat = JsonFunctions.valueOfNumber("propName").apply(Float.MAX_VALUE);
      Assert.assertEquals(Float.MAX_VALUE, joFloat.get("propName").getAsFloat(), 0.0f);
      
      JsonObject joDouble = JsonFunctions.valueOfNumber("propName").apply(Double.MAX_VALUE);
      Assert.assertEquals(Double.MAX_VALUE, joDouble.get("propName").getAsDouble(), 0.0d);
    }
    
    @Test
    public void testValueOfBoolean() {
      JsonObject joTrue = JsonFunctions.valueOfBoolean("propName").apply(true);
      Assert.assertEquals(true, joTrue.get("propName").getAsBoolean());

      JsonObject joFalse = JsonFunctions.valueOfBoolean("propName").apply(false);
      Assert.assertEquals(false, joFalse.get("propName").getAsBoolean());
    }
    
    @Test
    public void testValueOfString() {
      JsonObject jo = JsonFunctions.valueOfString("propName").apply("str1");
      Assert.assertEquals("str1", jo.get("propName").getAsString());
    }
    
    @Test
    public void testValueOfCharacter() {
      JsonObject jo = JsonFunctions.valueOfCharacter("propName").apply('c');
      Assert.assertEquals('c', jo.get("propName").getAsCharacter());
    }
}
