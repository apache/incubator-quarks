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

public class MyClass2 {
	private MyClass1 mc1, mc2;
	private Double d1;
	private String s1;

	MyClass2(MyClass1 mc1, MyClass1 mc2, Double d, String s) {
		this.mc1 = mc1; 
		this.mc2=mc2; 
		this.d1=d; 
		this.s1=s; 
	}

	MyClass1 getMc1() {
		return mc1; 
	}

	MyClass1 getMc2() {
		return mc2; 
	}

	Double getD1() {
		return d1; 
	}

	String getS1() {
		return s1; 
	}
	public void setMc1(MyClass1 mc) { 
		mc1 = mc; 
	}
	public void setMc2(MyClass1 mc) {
		mc2 = mc; 
	}
	public void setD1(Double d) {
		d1 = d; 
	}

	public void setS1(String s) {
		s1 = s; 
	}
	public String toString() {
		return "mc1: "+mc1.toString() + " mc2: " + mc2.toString() + " d1: "+ d1 + " s1: " + s1; 
	}
}


