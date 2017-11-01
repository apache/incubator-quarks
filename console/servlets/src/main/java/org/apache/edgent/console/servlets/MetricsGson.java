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
package org.apache.edgent.console.servlets;

import java.util.ArrayList;
import java.util.Iterator;


public class MetricsGson {

    private String jobId = null;
    private ArrayList<Operator> ops = new ArrayList<>();
	
	class Operator {
		final String opId;
		final ArrayList<OpMetric> metrics;
		Operator(String name) {
		    opId = name;
		    metrics = new ArrayList<>();
		    MetricsGson.this.addOp(this);
		}
	}
	
	class OpMetric {
		// the primitive type of the metric
		String type = null;
		String name = null;
		String value;
	}
	
	public String getJobId () {
		return this.jobId;
	}
	
	public void setJobId(String id) {
		this.jobId = id;
	}
	
	public void addOp(Operator theOp) {
		this.ops.add(theOp);
	}
	
	public Operator getOp(String opId) {
		if (this.ops.size() == 0) {
			return null;
		}
		
		Iterator<Operator> opsIterator = this.ops.iterator();
		while (opsIterator.hasNext()) {
			Operator op = opsIterator.next();
			if (getOpId(op).equals(opId)) {
				return op;
			}
		}
		return null;
	}
	
	public ArrayList<Operator> getOps() {
		return this.ops;
	}
	
	public boolean isOpInJob(String opId){
		if (this.ops.size() == 0) {
			return false;
		}
		
		Iterator<Operator> opsIterator = this.ops.iterator();
		while (opsIterator.hasNext()) {
			Operator op = opsIterator.next();
			if (getOpId(op).equals(opId)) {
				return true;
			}
		}
		return false;
	}
	
	public String getOpId(Operator anOperator) {
		return anOperator.opId;
	}
	
	public void setOps(ArrayList<Operator> operators){
		this.ops = operators;
	}
	
	public ArrayList<OpMetric> getOpMetrics(Operator theOp) {
		return theOp.metrics;
	}
	
}
