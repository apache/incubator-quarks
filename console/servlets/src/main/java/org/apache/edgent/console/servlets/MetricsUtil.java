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

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.edgent.console.servlets.MetricsGson.OpMetric;
import org.apache.edgent.console.servlets.MetricsGson.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MetricsUtil {
	
	private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	private static final Logger logger = LoggerFactory.getLogger(MetricsUtil.class);

	static Iterator<ObjectInstance> getCounterObjectIterator(String jobId) {
		ObjectName counterObjName = null;
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("*:jobId=").append(jobId);
		sbuf.append(",type=metric.counters,*");

        // i.e, edgent.providers.development:jobId=JOB-0,opId=OP_4,name=TupleRateMeter.edgent.oplet.JOB_0.OP_4,type=metric.meters
		try {
			counterObjName = new ObjectName(sbuf.toString());
		} catch (MalformedObjectNameException e) {
		    logger.error("Error caught while initializing ObjectName", e);
		}
		Set<ObjectInstance> counterInstances = mBeanServer.queryMBeans(counterObjName, null);
		return counterInstances.iterator();
		
	}
	static Iterator<ObjectInstance> getMeterObjectIterator(String jobId) {
		ObjectName meterObjName = null;
			
			StringBuilder sbuf1 = new StringBuilder();
			sbuf1.append("*:jobId=").append(jobId);
			sbuf1.append(",type=metric.meters,*");

			try {
				meterObjName = new ObjectName(sbuf1.toString());
			} catch (MalformedObjectNameException e) {
			    logger.error("Error caught while initializing ObjectName", e);
			}
			

		Set<ObjectInstance> meterInstances = mBeanServer.queryMBeans(meterObjName, null);
		// return only those beans that are part of the job
		return meterInstances.iterator();
	}
	
	static MetricsGson getAvailableMetricsForJob(String jobId, Iterator<ObjectInstance> meterIterator, Iterator<ObjectInstance> counterIterator) {
		MetricsGson gsonJob = new MetricsGson();
		gsonJob.setJobId(jobId);
		while (meterIterator.hasNext()) {
			ObjectInstance meterInstance = meterIterator.next();
			ObjectName mObjName = meterInstance.getObjectName(); 
			String opName = mObjName.getKeyProperty("opId");

				if (!opName.equals("")) {
					MBeanInfo mBeanInfo;
					try {
						mBeanInfo = mBeanServer.getMBeanInfo(mObjName);
					} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
					    logger.error("Exception caught while getting MBeanInfo", e);
					    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
					}

			    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
			    		OpMetric aMetric = gsonJob.new OpMetric();
		    			aMetric.type = "meter";
		    			aMetric.name = attributeInfo.getName();
				 
	    				try {
	    					aMetric.value = String.valueOf(mBeanServer.getAttribute(mObjName, aMetric.name));
	    				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
							| ReflectionException e) {
	    				    logger.error("Exception caught while accessing MBean", e);
	    				}
		    			
		    			// if the op associated with this metric is not in the job add it
		    			Operator theOp = gsonJob.getOp(opName);
		    			if (theOp == null) {
						    theOp = gsonJob.new Operator(opName);
		    			}
		    			theOp.metrics.add(aMetric);
			    	}
				}
		}

		while (counterIterator.hasNext()) {
			ObjectInstance counterInstance = counterIterator.next();
			ObjectName cObjName = counterInstance.getObjectName();
			String opName1 = cObjName.getKeyProperty("opId");

			if (!opName1.equals("")) {
			MBeanInfo mBeanInfo;
			try {
				mBeanInfo = mBeanServer.getMBeanInfo(cObjName);
			} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			    logger.error("Exception caught while getting MBeanInfo", e);
			    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
			}

	    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
	    		OpMetric aMetric = gsonJob.new OpMetric();
    			aMetric.type = "counter";
    			aMetric.name = attributeInfo.getName();
				try {
					aMetric.value = String.valueOf(mBeanServer.getAttribute(cObjName, aMetric.name));
				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
					| ReflectionException e) {
				    logger.error("Exception caught while accessing MBean", e);
				}
    			Operator theOp = gsonJob.getOp(opName1);
				if (theOp == null) {
                    theOp = gsonJob.new Operator(opName1);
				}
    			theOp.metrics.add(aMetric);
	    	}
			}
		}
		
		return gsonJob;

	}
	
	/**
	 * Get all the rate metrics, i.e, one minute rate, fifteen minute rate, mean rate, etc  for a job
	 * @param jobId id (e.g, "JOB_0")
	 * 
	 * @return  all metrics for this job if there are any
	 */
	static MetricsGson getAllRateMetrics(String jobId) {
		MetricsGson gsonJob = new MetricsGson();
		gsonJob.setJobId(jobId);
		
		Iterator<ObjectInstance> meterIterator = MetricsUtil.getMeterObjectIterator(jobId);
		while (meterIterator.hasNext()) {
			ObjectInstance meterInstance = meterIterator.next();
			ObjectName mObjName = meterInstance.getObjectName();
			//i.e, edgent.providers.development:jobId=JOB-0,opId=OP_4,name=TupleRateMeter.edgent.oplet.JOB_0.OP_4,type=metric.meters
			String jobName = mObjName.getKeyProperty("jobId");
			String opName = mObjName.getKeyProperty("opId");

			if (jobId.equals(jobName)) {
				MBeanInfo mBeanInfo;
			
				try {
					mBeanInfo = mBeanServer.getMBeanInfo(mObjName);
				} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
				    logger.error("Exception caught while getting MBeanInfo", e);
				    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
				}
				
		    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
		    		String name = attributeInfo.getName();
    				OpMetric aMetric = gsonJob.new OpMetric();
    				aMetric.name = name;
    				aMetric.type = attributeInfo.getType();
    				try {
						aMetric.value = String.valueOf(mBeanServer.getAttribute(mObjName, name));
    				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
							| ReflectionException e) {
					    logger.error("Exception caught while accessing MBean", e);
    				}
					 Operator theOp = gsonJob.getOp(opName);
					 if (theOp == null) {
					    theOp = gsonJob.new Operator(opName);
					 }
					 theOp.metrics.add(aMetric);
		    	}
			}
		}
		
		Iterator<ObjectInstance> counterIterator = MetricsUtil.getCounterObjectIterator(jobId);
		while (counterIterator.hasNext()) {
			ObjectInstance counterInstance = counterIterator.next();
			ObjectName cObjName = counterInstance.getObjectName();
			String opName1 = cObjName.getKeyProperty("opId");

			if (!opName1.equals("")) {
			MBeanInfo mBeanInfo;
			try {
				mBeanInfo = mBeanServer.getMBeanInfo(cObjName);
			} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			    logger.error("Exception caught while getting MBeanInfo", e);
			    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
			}

	    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
	    		OpMetric aMetric = gsonJob.new OpMetric();
    			aMetric.type = "counter";
    			aMetric.name = attributeInfo.getName();
				try {
					aMetric.value = String.valueOf(mBeanServer.getAttribute(cObjName, aMetric.name));
				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
					| ReflectionException e) {
				    logger.error("Exception caught while accessing MBean", e);
				}
    			Operator theOp = gsonJob.getOp(opName1);
				if (theOp == null) {
					theOp = gsonJob.new Operator(opName1);
				}
    			theOp.metrics.add(aMetric);
	    	}
			}
		}
		return gsonJob;
	}
	// format for metricName is "name:RateUnit,type:meter"
	static MetricsGson getMetric(String jobId, String metricName, Iterator<ObjectInstance> metricIterator, Iterator<ObjectInstance> counterIterator) {

		MetricsGson gsonJob = new MetricsGson();
		gsonJob.setJobId(jobId);
		String[] desiredParts = metricName.split(",");
		String desName = "";
		if (!desiredParts[0].equals("")) {
			String[] nameA = desiredParts[0].split(":");
			desName = nameA[1];
		}
		
		while (metricIterator.hasNext()) {
			ObjectInstance meterInstance = metricIterator.next();
			ObjectName mObjName = meterInstance.getObjectName();
			//i.e, edgent.providers.development:jobId=JOB-0,opId=OP_4,name=TupleRateMeter.edgent.oplet.JOB_0.OP_4,type=metric.meters
			String jobName = mObjName.getKeyProperty("jobId");
			String opName = mObjName.getKeyProperty("opId");

			if (jobId.equals(jobName)) {
				MBeanInfo mBeanInfo;
			
				try {
					mBeanInfo = mBeanServer.getMBeanInfo(mObjName);
				} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
				    logger.error("Exception caught while getting MBeanInfo", e);
				    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
				}
				
		    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
		    		String name = attributeInfo.getName();
	    			 if(name.equals(desName)) {
	    				 OpMetric aMetric = gsonJob.new OpMetric();
	    				 aMetric.name = name;
	    				 aMetric.type = attributeInfo.getType();
	    				 try {
							aMetric.value = String.valueOf(mBeanServer.getAttribute(mObjName, name));
						} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
								| ReflectionException e) {
						    logger.error("Exception caught while accessing MBean", e);
						}
    					Operator theOp = gsonJob.getOp(opName);
    					if (theOp == null) {
    					    theOp = gsonJob.new Operator(opName);
    					}
	    				 theOp.metrics.add(aMetric);
	    			 }
		    	}
			}
		}
		
		while (counterIterator.hasNext()) {
			ObjectInstance counterInstance = counterIterator.next();
			ObjectName cObjName = counterInstance.getObjectName();
			String jobName1 = cObjName.getKeyProperty("jobId");
			String opName1 = cObjName.getKeyProperty("opId");
			

			if (jobId.equals(jobName1)) {
				MBeanInfo mBeanInfo;
			
				try {
					mBeanInfo = mBeanServer.getMBeanInfo(cObjName);
				} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
				    logger.error("Exception caught while getting MBeanInfo", e);
				    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
				}
				
		    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
		    		String name = attributeInfo.getName();

	    			 if(name.equals(desName)) {
	    				 OpMetric aMetric = gsonJob.new OpMetric();
	    				 aMetric.name = name;
	    				 aMetric.type = attributeInfo.getType();
	    				 try {
							aMetric.value = String.valueOf(mBeanServer.getAttribute(cObjName, name));
						} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
								| ReflectionException e) {
						    logger.error("Exception caught while accessing MBean", e);
						}
    					Operator theOp = gsonJob.getOp(opName1);
    					if (theOp == null) {
    					    theOp = gsonJob.new Operator(opName1);
    					}
	    				 theOp.metrics.add(aMetric);
	    			 }
		    	}
			}
		}	
		return gsonJob;
	}
}
