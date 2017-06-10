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
package org.apache.edgent.test.connectors.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.edgent.connectors.csv.Csv;
import org.apache.edgent.test.providers.direct.DirectTopologyTestBase;
import org.junit.Test;

import com.google.gson.JsonObject;

public class CsvTest extends DirectTopologyTestBase {

  @Test
  public void testParse() {
    testParse("abc,1,def", new String[]{"abc", "1", "def"});
    // spaces are part of the field
    testParse("ab c, 1 ,d ef", new String[]{"ab c", " 1 ", "d ef"});
    
    // check alternate separator
    testParse("ab,c;1;d,ef", ";", new String[]{"ab,c", "1", "d,ef"});
    
    // check empty field
    testParse("abc,,def", new String[]{"abc", "", "def"});
    
    // simple quoting - no quoted quotes;  with and w/o embedded separator
    testParse("\"ab c\",\"d ef\"", new String[]{"ab c", "d ef"});
    testParse("\"ab,,c\",\"d ef\"", new String[]{"ab,,c", "d ef"});

    // simple quoted quotes - not embedded in a quoted field
    testParse("\"\"ab c,d ef", new String[]{"\"ab c", "d ef"});
    testParse("\"\"\"\"ab c,d ef", new String[]{"\"\"ab c", "d ef"});
    // middle
    testParse("ab \"\"c,d ef", new String[]{"ab \"c", "d ef"});
    testParse("ab \"\"\"\"c,d ef", new String[]{"ab \"\"c", "d ef"});
    // end
    testParse("ab c\"\",d ef", new String[]{"ab c\"", "d ef"});
    testParse("ab c\"\"\"\",d ef", new String[]{"ab c\"\"", "d ef"});
    // beginning, middle and end
    testParse("\"\"ab \"\"c\"\",d ef", new String[]{"\"ab \"c\"", "d ef"});
    testParse("\"\"\"\"ab \"\"\"\"c\"\"\"\",d ef", new String[]{"\"\"ab \"\"c\"\"", "d ef"});
    
    // quoted quotes in a quoted field
    testParse("\"\"\"ab \"\"c\"\"\",d ef", new String[]{"\"ab \"c\"", "d ef"});
    testParse("\"\"\"\"\"ab \"\"\"\"c\"\"\"\"\",d ef", new String[]{"\"\"ab \"\"c\"\"", "d ef"});
    
    testParseMalformed("\"ab c,d ef");  // non-escaped quote at start of field, or missing end of quoted field
    testParseMalformed("ab \"c,d ef");  // non-escaped quote in middle of field
    testParseMalformed("ab c\",d ef");  // non-escaped quote in end of field, or missing start of quoted field
    testParseMalformed("ab c,d ef\"");  // non-escaped quote in end of field, or missing start of quoted field
  }
 
  private void testParse(String csv, String[] expected) {
    testParse(csv, null, expected);
  }

  private void testParse(String csv, String sep, String[] expected) {
    List<String> exp = Arrays.asList(expected);
    List<String> fields;
    if (sep == null)
      fields = Csv.parseCsv(csv);
    else
      fields = Csv.parseCsv(csv, sep.charAt((0)));
    assertEquals("csv: "+csv, exp, fields);
  }

  private void testParseMalformed(String csv) {
    try {
      List<String> fields = Csv.parseCsv(csv);
      fail("expected malformed for csv: "+csv+" but got fields: "+fields);
    }
    catch(IllegalArgumentException e) {
      System.out.println("Got expected exception for malformed for csv: "+csv+"  :" + e); // expected one
    }
  }
  
  @Test 
  public void testToJson() {
    List<String> fields = Arrays.asList("one","two","three");
    String[] names = new String[]{"fieldOne","fieldTwo","fieldThree"};
    String[] names2 = new String[]{"fieldOne",null,"fieldThree"};
    String[] names3 = new String[]{"fieldOne","fieldTwo",""};
    
    testToJson(fields, names);
    testToJson(fields, names2);
    testToJson(fields, names3);
    try {
      testToJson(fields, names[0], names[1]);  // field/name length mismatch
    }
    catch(IllegalArgumentException e) {
      System.out.println("Got expected exception: " + e);
    }
  }
  
  private void testToJson(List<String> fields, String... fieldNames) {
    JsonObject jo = Csv.toJson(fields, fieldNames);
    
    for (int i = 0; i < fieldNames.length; i++) {
      String value = fields.get(i);
      String name = fieldNames[i];
      if (name != null && !name.isEmpty()) {
        assertEquals("name:"+name, value, jo.get(name).getAsString());
      }
    }
  }
}
