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

import java.lang.reflect.Method;

/**
 * Utilities for the control service.
 *
 * @see ControlService
 */
public class Controls {
    
    /**
     * Test to see if an interface represents a valid
     * control service MBean.
     * All implementations of {@code ControlService}
     * must support control MBeans for which this
     * method returns true.
     * <BR>
     * An interface is a valid control service MBean if
     * all of the following are true:
     * <UL>
     * <LI>An interface that does not extend any other interface.</LI>
     * <LI>Not be parameterized</LI>
     * <LI>Method parameters and return types restricted to these types:
     * <UL>
     * <LI>{@code String, boolean, int, long, double}.</LI>
     * <LI>Any enumeration</LI>
     * </UL> 
     * </UL>
     * @param controlInterface
     * @return True
     */
    public static boolean isControlServiceMBean(Class<?> controlInterface) {

        if (!controlInterface.isInterface())
            return false;
        
        if (controlInterface.getInterfaces().length != 0)
            return false;
        
        if (controlInterface.getTypeParameters().length != 0)
            return false;
        
        for (Method cim : controlInterface.getDeclaredMethods()) {
            if (cim.getReturnType() != Void.TYPE
                    && !validType(cim.getReturnType()))
                return false;
            
            for (Class<?> ptt : cim.getParameterTypes()) {
                if (!validType(ptt))
                    return false;
            }
        }
        
        return true;
    }

    static boolean validType(Class<?> type) {
        if (String.class == type)
            return true;
        if (Boolean.TYPE == type)
            return true;
        if (Integer.TYPE == type)
            return true;
        if (Long.TYPE == type)
            return true;
        if (Double.TYPE == type)
            return true;
        if (type.isEnum())
            return true;
        
        return false;
    }
}
