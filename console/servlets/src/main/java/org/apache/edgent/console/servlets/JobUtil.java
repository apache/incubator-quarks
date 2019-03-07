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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JobUtil {
	
	private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	private static final Logger logger = LoggerFactory.getLogger(JobUtil.class);

	static String getJobsInfo(ObjectName jobObjName) {
        Set<ObjectInstance> jobInstances = mBeanServer.queryMBeans(jobObjName, null);
        
        Iterator<ObjectInstance> jobIterator = jobInstances.iterator();
        StringBuilder json = new StringBuilder("[");
        int counter = 0;
        while (jobIterator.hasNext()) {
        	if (counter > 0) {
        		json.append(",");
        	}
        	ObjectInstance jobInstance = jobIterator.next();
            ObjectName jobObjectName = jobInstance.getObjectName();
            MBeanInfo mBeanInfo;
			try {
				mBeanInfo = mBeanServer.getMBeanInfo(jobObjectName);
			} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			    logger.error("Exception caught while getting MBeanInfo", e);
				throw new RuntimeException("Exception caught while getting MBeanInfo", e);
			}
            
            /*
             * Get the names of all the attributes
             */
			Set<String> names = new HashSet<> ();
	    	for (MBeanAttributeInfo attributeInfo : mBeanInfo.getAttributes()) {
	    			names.add(attributeInfo.getName());
	    	}
	    	// now construct the job json and add it to the string buffer
	    	StringBuilder s = new StringBuilder();
	    	s.append("{\"");
			for (String attr : names) {
				s.append(attr);
				s.append("\":\"");
				try {
					s.append((String) mBeanServer.getAttribute(jobObjectName, attr));
				} catch (AttributeNotFoundException | InstanceNotFoundException
						| MBeanException | ReflectionException e) {
					logger.error("Exception caught while accessing MBean", e);
				}
				s.append("\",\"");
			}
	    	// remove the trailing ,\
	    	s.deleteCharAt(s.length()-1);
	    	s.deleteCharAt(s.length()-1);
	    	json.append(s.toString()).append("}");
	    	counter++;
        }
        json.append("]");
        return json.toString();
	}
	
	static String getJobGraph(ObjectName jobObjName) {
        Set<ObjectInstance> jobInstances = mBeanServer.queryMBeans(jobObjName, null);
        Iterator<ObjectInstance> jobIterator = jobInstances.iterator();
        ObjectInstance jobInstance = null;

        if(jobIterator.hasNext()) {
        	jobInstance = jobIterator.next();
        }
        String gSnapshot = "";
        if (jobInstance != null) {
            ObjectName jobObjectName = jobInstance.getObjectName();
            MBeanInfo mBeanInfo;
			try {
				mBeanInfo = mBeanServer.getMBeanInfo(jobObjectName);
			} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			    logger.error("Exception caught while getting MBeanInfo", e);
			    throw new RuntimeException("Exception caught while getting MBeanInfo", e);
			}

	    	/*
	    	 * Now get the org.apache.edgent.graph for the job
	    	 */
	    	for (MBeanOperationInfo operationInfo: mBeanInfo.getOperations()) {
	    		if (operationInfo.getName().equals("graphSnapshot")) {
	    			try {
						gSnapshot = (String) mBeanServer.invoke(jobObjectName, "graphSnapshot",null, null);
					} catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
					    logger.error("Exception caught while invoking operation on MBean", e);
                        throw new RuntimeException("Exception caught while invoking operation on MBean", e);
                    }
	    		}
	    	}
	    }
		return gSnapshot;
	}
}
