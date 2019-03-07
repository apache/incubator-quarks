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
package org.apache.edgent.connectors.csv;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * Utilities for working with CSV strings.
 * <P>
 * Basically, per IETF RFC-4180:
 * <UL>
 * <LI>Fields are separated by a single character.  The default field
 * separator is a comma.  A different separator may be specified
 * (e.g., a colon, tab).</LI>
 * <LI>All whitespace between field separators is part of the field's parsed value.</LI>
 * <LI>A field may be quoted using the double-quote character.
 * A field containing the field separator must be quoted.
 * The double-quotes surrounding a quoted field are omitted in the
 * field's parsed value.</LI>
 * <LI>A double-quote to be included in a field's parsed value must be
 * represented by a pair of double quotes.</LI>
 * </UL>
 * 
 * Sample use:
 * <pre>{@code
 * // Create a stream of "car" JsonObjects from a "cars.csv" file.
 * String fieldNames = new String[]{"year", "make", "model"});
 *
 * TStream<String> pathnames = org.apache.edgent.org.apache.edgent.topology.strings("cars.csv");
 * TStream<JsonObject> cars = FileStreams.textFileReader(org.apache.edgent.org.apache.edgent.topology, pathnames)
 *    .map(csv -> toJson(parseCsv(csv), fieldNames);
 * cars.print(); 
 * }</pre>
 */
public class Csv {
  private Csv() { }

  /**
   * Parse a CSV string into its fields using comma for the field separator.
   * 
   * @param csv the csv string
   * @return the fields
   * @throws IllegalArgumentException if the csv is malformed
   * 
   * @see #parseCsv(String, char)
   */
  public static List<String> parseCsv(String csv) {
    return parseCsv(csv, ',');
  }
  
  /**
   * Parse a CSV string into its fields using the specified field separator.
   * 
   * @param csv the csv string
   * @param separator the separator to use
   * @return the fields
   * @throws IllegalArgumentException if the csv is malformed
   * 
   * @see #parseCsv(String)
   */
  public static List<String> parseCsv(String csv, char separator) {
    final char QC = '"';
    List<String> list = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    
    boolean inQuote = false;
    for (int i=0; i<csv.length(); i++) {
      char c = csv.charAt(i);
      if (c == QC) {
        if (i+1 < csv.length() && csv.charAt(i+1) == QC) {
          // a quoted quote yields a quote. no affect on inQuote status.
          i++;
          field.append(QC);
          continue;
        }
        else {
          inQuote = !inQuote;  // either in or out now
          
          // if now IN, it must be the start of a field
          if (inQuote) {
            // because of quoted quote handling the field can have just quotes
            for (int j = 0; j < field.length(); j++) {
              if (field.charAt(j) != QC)
                break; // trigger the malformed check below
            }
          }
          else {
            // if now OUT, it must be the end of a field
            if (!inQuote) {
              if (i+1 == csv.length() || csv.charAt(i+1) == separator)
                ; // ok
              else {
                inQuote = true; // a lie but trigger the malformed check below
                break;
              }
            }
          }
        }
      }
      else if (c == separator) {
        if (inQuote) {
          field.append(c);
        } else {
          list.add(field.toString());
          field.setLength(0);
        }
      }
      else {
        field.append(c);
      }
    }
    if (inQuote)
      throw new IllegalArgumentException("malformed csv string: unbalanced quotes in csv: " + csv);
    
    if (field.length() != 0) {
      list.add(field.toString());
    }
    
    return list;    
  }

  /**
   * Create a {@link JsonObject} containing the specified {@code fieldNames}
   * properties each with its corresponding value from {@code fields}.
   * <P>
   * Each property is set as a string value.
   * The {@code JsonObject.getAsJsonPrimitive().getAs*()} methods allowing
   * accessing the property as the requested type.
   * </P>
   * <P>
   * A field is omitted from the JsonObject if its corresponding
   * field name is null or the empty string.
   *  
   * @param fields the field values
   * @param fieldNames the corresponding field value names
   * @return the JsonObject
   * @throws IllegalArgumentException if the number of fields and the number
   * of fieldNames don't match
   */
  public static JsonObject toJson(List<String> fields, String... fieldNames) {
    if (fields.size() != fieldNames.length) {
      throw new IllegalArgumentException("Mismatched number of fields and names");
    }
    JsonObject jo = new JsonObject();
    for (int i = 0; i < fieldNames.length; i++) {
      String name = fieldNames[i];
      
      // skip the field if so indicated
      if (name == null || name.isEmpty())
        continue;
      
      jo.addProperty(name, fields.get(i));
    }
    return jo;
  }

}
