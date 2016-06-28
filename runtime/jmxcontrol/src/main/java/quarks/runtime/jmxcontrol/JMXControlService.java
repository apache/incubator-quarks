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
	 * @param additionalKeys additional name/value keys to add to the generated JMX object names
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

            return getControlId(on);
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
    public <T> T  getControl(String type, String alias, Class<T> controlInterface) {
        MBeanServer mBeanServer = getMbs();
        ObjectName name = getObjectNameForInterface(type, alias, controlInterface);
        return name != null ? JMX.newMXBeanProxy(mBeanServer, name, controlInterface) : null;
    }

    @Override
    public <T> String getControlId(String type, String alias, Class<T> controlInterface) {
        return getControlId(getObjectNameForInterface(type, alias, controlInterface));
    }

    private <T> ObjectName getObjectNameForInterface(String type, String alias, Class<T> controlInterface) {
        try {
            Set<ObjectName> names = getObjectNamesForInterface(type, alias, controlInterface.getName());
            
            if (names.isEmpty())
                return null;
            if (names.size() != 1)
                throw new RuntimeException("Alias " + alias + " not unique for type " + type);
    
            ObjectName name = null;
            for (ObjectName on : names) {
                name = on;
                break;
            }
            return name;
        }
        catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getControlId(ObjectName on) {
        return on != null ? on.getCanonicalName() : null;
    }

    private Set<ObjectName> getObjectNamesForInterface(String type, String alias, String interfaceName) 
            throws MalformedObjectNameException {
        
        Hashtable<String,String> table = new Hashtable<>();       
        table.put("interface", ObjectName.quote(interfaceName));
        table.put("type", ObjectName.quote(type));
        if (alias != null)
            table.put("alias", ObjectName.quote(alias));
        ObjectName objName = new ObjectName(getDomain(), table);
        
        // Add the wildcard for any other properties.
        objName = new ObjectName(objName.getCanonicalName()+",*");

        MBeanServer mBeanServer = getMbs();
        return mBeanServer.queryNames(objName, null);
    }
}
