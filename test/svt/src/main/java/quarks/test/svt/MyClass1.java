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
package quarks.test.svt;

public class MyClass1 {
    private String s1, s2;
    private Double d1;

    MyClass1(String str1, String str2, Double d1) {
        this.s1 = str1; this.s2=str2; this.d1=d1; 
    }

    String getS1() {
        return s1; 
    }

    String getS2() {
        return s2; 
    }

    Double getD1() {
        return d1; 
    }

    public void setS1(String s) { 
        s1 = s; 
    }
    public void setS2(String s) {
        s2 = s; 
    }
    public void setD1(Double d) {
        d1 = d; 
    }

    public String toString() {
        return "s1: "+s1+" s2: "+s2 + " d1: "+d1; 
    }

}
