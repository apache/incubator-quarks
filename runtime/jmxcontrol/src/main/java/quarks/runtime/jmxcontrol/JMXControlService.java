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
package quarks.runtime.jmxcontrol;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import quarks.execution.services.ControlService;

/**
 * Control service that registers control objects
 * as MBeans in a JMX server.
 *
 */
public class JMXControlService implements ControlService {
	
	private final MBeanServer mbs;
	private final String domain;
	private final Hashtable<String,String> additionalKeys;
	
	/**
	 * JMX control service using the platform MBean server.
	 * @param domain Domain the MBeans are registered in.
	 */
	public JMXControlService(String domain, Hashtable<String,String> additionalKeys) {
		mbs = ManagementFactory.getPlatformMBeanServer();
		this.domain = domain;
		this.additionalKeys = additionalKeys;
	}
	
	
	/**
	 * Get the MBean server being used by this control service.
	 * @return MBean server being used by this control service.
	 */
	public MBeanServer getMbs() {
		return mbs;
	}
	
	/**
     * Get the JMX domain being used by this control service.
     * @return JMX domain being used by this control service.
     */
	public String getDomain() {
        return domain;
    }

	/**
	 * 
	 * Register a control object as an MBean.
	 * 
	 * {@inheritDoc}
	 * 
	 * The MBean is registered within the domain returned by {@link #getDomain()}
	 * and an `ObjectName` with these keys:
	 * <UL>
	 * <LI>type</LI> {@code type}
	 * <LI>interface</LI> {@code controlInterface.getName()}
	 * <LI>id</LI> {@code type}
	 * <LI>alias</LI> {@code alias}
	 * </UL>
	 * 
	 */
	@Override
	public <T> String registerControl(String type, String id, String alias, Class<T> controlInterface, T control) {
		Hashtable<String,String> table = new Hashtable<>();
		
		table.put("type", ObjectName.quote(type));
		table.put("interface", ObjectName.quote(controlInterface.getName()));
		table.put("id", ObjectName.quote(id));
		if (alias != null)
		   table.put("alias", ObjectName.quote(alias));
		
		additionalNameKeys(table);
			
        try {
            ObjectName on = ObjectName.getInstance(getDomain(), table);
            getMbs().registerMBean(control, on);

            return on.getCanonicalName();
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }

	}
	
	protected void additionalNameKeys(Hashtable<String,String> table) {
	    table.putAll(additionalKeys);
	}
	
	@Override
	public void unregister(String controlId) {
		try {
            mbs.unregisterMBean(ObjectName.getInstance(controlId));
        } catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException
                | NullPointerException e) {
            throw new RuntimeException(e);
        }
	}

    @Override
    public <T> Set<T> getControls(Class<T> controlInterface) {
        try {
            MBeanServer mBeanServer = getMbs();
            Set<ObjectName> names = getObjectNamesForInterface(controlInterface.getName());
            
            Set<T> controls = new HashSet<T>();
            for (ObjectName on : names) {
                controls.add(JMX.newMXBeanProxy(mBeanServer, on, controlInterface));
            }
            return controls;
        }
        catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<ObjectName> getObjectNamesForInterface(String interfaceName) 
            throws MalformedObjectNameException {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(getDomain()).
                append(":interface=").append(ObjectName.quote(interfaceName)).
                append(",*");
        ObjectName objName = new ObjectName(sbuf.toString());

        MBeanServer mBeanServer = getMbs();
        return mBeanServer.queryNames(objName, null);
    }
}
