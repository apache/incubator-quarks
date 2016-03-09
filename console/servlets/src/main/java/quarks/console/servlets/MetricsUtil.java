/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.console.servlets;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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

import quarks.console.servlets.MetricsGson.OpMetric;
import quarks.console.servlets.MetricsGson.Operator;

final class MetricsUtil {
	
	static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	static Iterator<ObjectInstance> getCounterObjectIterator(String jobId) {
		ObjectName counterObjName = null;
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("*:jobId=" + jobId);
		sbuf.append(",type=metric.counters,*");

        // i.e, quarks.providers.development:jobId=JOB-0,opId=OP_4,name=TupleRateMeter.quarks.oplet.JOB_0.OP_4,type=metric.meters
		try {
			counterObjName = new ObjectName(sbuf.toString());
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Set<ObjectInstance> counterInstances = mBeanServer.queryMBeans(counterObjName, null);
		return counterInstances.iterator();
		
	}
	static Iterator<ObjectInstance> getMeterObjectIterator(String jobId) {
		ObjectName meterObjName = null;
			
			StringBuffer sbuf1 = new StringBuffer();
			sbuf1.append("*:jobId=" + jobId);
			sbuf1.append(",type=metric.meters,*");

			try {
				meterObjName = new ObjectName(sbuf1.toString());
			} catch (MalformedObjectNameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		Set<ObjectInstance> meterInstances = mBeanServer.queryMBeans(meterObjName, null);
		// return only those beans that are part of the job
		return meterInstances.iterator();
	}
	
	static MetricsGson getAvailableMetricsForJob(String jobId, Iterator<ObjectInstance> meterIterator, Iterator<ObjectInstance> counterIterator) {
		MetricsGson gsonJob = new MetricsGson();
		ArrayList<Operator> counterOps = new ArrayList<Operator>();
		gsonJob.setJobId(jobId);
		while (meterIterator.hasNext()) {
			ArrayList<OpMetric> metrics = null;
			ObjectInstance meterInstance = (ObjectInstance)meterIterator.next();
			ObjectName mObjName = meterInstance.getObjectName(); 
			String opName = mObjName.getKeyProperty("opId");

			Operator anOp = null;
				if (!opName.equals("")) {
					MBeanInfo mBeanInfo = null;
					try {
						mBeanInfo = mBeanServer.getMBeanInfo(mObjName);
					} catch (IntrospectionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InstanceNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ReflectionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
			    		OpMetric aMetric = gsonJob.new OpMetric();
		    			aMetric.type = "meter";
		    			aMetric.name = attributeInfo.getName();
		    			// if the name of the metric is "RateUnit", get the value as well
		    			if (aMetric.name.equals("RateUnit")) {				 
		    				try {
		    					aMetric.value = String.valueOf(mBeanServer.getAttribute(mObjName, aMetric.name));
		    				} catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
								| ReflectionException e) {
		    					// TODO Auto-generated catch block
		    					e.printStackTrace();
		    				}
		    			}
		    			// if the op associated with this metric is not in the job add it
		    			if (!gsonJob.isOpInJob(opName)) {
						    anOp = gsonJob.new Operator();
						    gsonJob.addOp(anOp);
						    anOp.opId = opName;
						    counterOps.add(anOp); // why do I have this?
						    metrics = new ArrayList<OpMetric>();
		    			} 
		    			metrics.add(aMetric);
			    	}
			    	gsonJob.setOpMetrics(anOp, metrics);
				}
			

	    	
		}

		while (counterIterator.hasNext()) {
			ArrayList<OpMetric> metrics = null;
			ObjectInstance counterInstance = (ObjectInstance)counterIterator.next();
			ObjectName cObjName = counterInstance.getObjectName();
			String opName1 = cObjName.getKeyProperty("opId");

			Operator anOp = null;
			if (!opName1.equals("")) {
			MBeanInfo mBeanInfo = null;
			try {
				mBeanInfo = mBeanServer.getMBeanInfo(cObjName);
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReflectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
	    		OpMetric aMetric = gsonJob.new OpMetric();
    			aMetric.type = "counter";
    			aMetric.name = attributeInfo.getName();
    			Operator theOp = gsonJob.getOp(opName1);
				if (theOp == null) {
					anOp = gsonJob.new Operator();
					gsonJob.addOp(anOp);
					anOp.opId = opName1;
					metrics = new ArrayList<OpMetric>();
					gsonJob.setOpMetrics(anOp, metrics);
				} else {
					// get the op
					metrics = theOp.metrics;
				}
    			metrics.add(aMetric);
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
		String[] nameA = new String[2];
		String desName = "";
		if (!desiredParts[0].equals("")) {
			nameA = desiredParts[0].split(":");
			desName = nameA[1];
		}
		
		while (metricIterator.hasNext()) {
			ArrayList<OpMetric> metrics = null;
			ObjectInstance meterInstance = (ObjectInstance)metricIterator.next();
			ObjectName mObjName = meterInstance.getObjectName();
			//i.e, quarks.providers.development:jobId=JOB-0,opId=OP_4,name=TupleRateMeter.quarks.oplet.JOB_0.OP_4,type=metric.meters
			String jobName = mObjName.getKeyProperty("jobId");
			String opName = mObjName.getKeyProperty("opId");
			Operator anOp = null;

			if (jobId.equals(jobName)) {
				MBeanInfo mBeanInfo = null;
			
				try {
					mBeanInfo = mBeanServer.getMBeanInfo(mObjName);
				} catch (IntrospectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstanceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ReflectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					if (!gsonJob.isOpInJob(opName)) {
    					    anOp = gsonJob.new Operator();
    					    gsonJob.addOp(anOp);
    					    anOp.opId = opName;
    					    metrics = new ArrayList<OpMetric>();
    					    gsonJob.setOpMetrics(anOp, metrics);
    					} else {
    						anOp = gsonJob.getOp(opName);
    						metrics = anOp.metrics;
    					}
	    				 metrics.add(aMetric);
	    			 }
		    	}
		    	
			}
	    	
		}
		
		while (counterIterator.hasNext()) {
			ArrayList<OpMetric> metrics = null;
			ObjectInstance counterInstance = (ObjectInstance)counterIterator.next();
			ObjectName cObjName = counterInstance.getObjectName();
			String jobName1 = cObjName.getKeyProperty("jobId");
			String opName1 = cObjName.getKeyProperty("opId");
			

			Operator anOp = null;
			if (jobId.equals(jobName1)) {
				MBeanInfo mBeanInfo = null;
			
				try {
					mBeanInfo = mBeanServer.getMBeanInfo(cObjName);
				} catch (IntrospectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstanceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ReflectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					if (!gsonJob.isOpInJob(opName1)) {
    					    anOp = gsonJob.new Operator();
    					    gsonJob.addOp(anOp);
    					    anOp.opId = opName1;
    					    metrics = new ArrayList<OpMetric>();
    					    gsonJob.setOpMetrics(anOp, metrics);
    					} else {
    						anOp = gsonJob.getOp(opName1);
    						metrics = anOp.metrics;
    					}
	    				 metrics.add(aMetric);
	    			 }
		    	}
			}
		}	
		return gsonJob;
	}
	
}
