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
package quarks.execution.services;

/**
 * Service that provides a control mechanism.
 * <BR>
 * The control service allows applications and Quarks itself to
 * register control interfaces generically. The style of a control interface
 * is similar to a JMX Management Bean (MBean), specifically a JMX MXBean.
 * <BR>
 * No dependency is created on any JMX interface to allow running on systems
 * that do not support JMX, such as Android.
 * <P>
 * Different implementations of the control service provide the mechanism
 * to execute methods of the control interfaces. For example
 * {@link quarks.runtime.jmxcontrol.JMXControlService JMXControlService}
 * registers the MBeans in the JMX platform MBean server.
 * <BR>
 * The control service is intended to allow remote execution of a control interface
 * through any mechanism. The control service provides operations and attributes
 * similar to JMX. It does not provide notifications.
 * </P>
 * <P>
 * An instance of a control service MBean is defined by its:
 * </P>
 * <UL>
 * <LI> A type </LI>
 * <LI> A identifier - Unique within the current execution context.</LI>
 * <LI> An alias - Optional, but can be combined with the control MBeans's type 
 * to logically identify a control MBean. </LI>
 * <LI> A Java interface - This defines what operations can be executed
 * against the control MBean.</LI>
 * </UL>
 * <P>
 * A remote system should be able to specify an operation on an
 * control server MBean though its alias and type. For example
 * an application might be submitted with a fixed name
 * <em>PumpAnalytics</em> (as its alias)
 * to allow its {@link quarks.execution.mbeans.JobMXBean JobMXBean}
 * to be determined remotely using a combination of
 * {@link quarks.execution.mbeans.JobMXBean#TYPE JobMXBean.TYPE}
 * and <em>PumpAnalytics</em>.
 * </P>
 * <P>
 * Control service implementations may be limited in their capabilities,
 * for example when using the JMX control service the full capabilities
 * of JMX can be used, such as complex types in a control service MBean interface.
 * Portable applications would limit themselves to a smaller subset of
 * capabilities, such as only primitive types and enums.
 * <BR>
 * The method {@link Controls#isControlServiceMBean(Class)} defines
 * the minimal supported interface for any control service.
 * </P>
 */
public interface ControlService {

    /**
     * Register a control MBean.
     *
     * @param <T> Control MBean type
     * @param type Type of the control MBean.
     * @param id
     *            Unique identifier for the control MBean.
     * @param alias
     *            Alias for the control MBean. Required to be unique within the context
     *            of {@code type}.
     * @param controlInterface
     *            Public interface for the control MBean.
     * @param control
     *            The control MBean
     * @return unique identifier that can be used to unregister an control MBean.
     */
    <T> String registerControl(String type, String id, String alias, Class<T> controlInterface, T control);
    
    /**
     * Unregister a control bean registered by {@link #registerControl(String, String, String, Class, Object)}
     * @param controlId control's registration identifier returned by {@code registerControl}
     */
    void unregister(String controlId);
    
    /**
     * Return a control Mbean registered with this service.
     * 
     * @param <T> Control MBean type
     * @param type Type of the control MBean.
     * @param alias Alias for the control MBean.
     * @param controlInterface
     *              Public interface of the control MBean. 
     * @return Control Mbean or null if a matching MBean is not registered.
     */
    <T> T getControl(String type, String alias, Class<T> controlInterface);

    /**
     * Return the unique identifier for a control Mbean registered with 
     * this service.
     * 
     * @param <T> Control MBean type
     * @param type Type of the control MBean.
     * @param alias Alias for the control MBean.
     * @param controlInterface
     *              Public interface of the control MBean. 
     * @return unique identifier that can be used to unregister a control 
     *              MBean or null if a matching MBean is not registered.
     */
    <T> String getControlId(String type, String alias, Class<T> controlInterface);
}
