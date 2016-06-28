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
package quarks.runtime.jsoncontrol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import quarks.execution.services.ControlService;
import quarks.execution.services.Controls;

/**
 * Control service that accepts control instructions as JSON objects.
 * <BR>
 * A JSON object representing a control request can be passed
 * to {@link #controlRequest(JsonObject)} to invoke a control
 * operation (method) on a registered MBean.
 * 
 *  @see quarks.runtime.jsoncontrol Format of control request operation
 */
public class JsonControlService implements ControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(ControlService.class);

    /**
     * Key for the type of the control MBean in a JSON request.
     * <BR>
     * Value is {@value}.
     */
    public static final String TYPE_KEY = "type";
    
    /**
     * Key for the alias of the control MBean in a JSON request.
     * <BR>
     * Value is {@value}.     */
    public static final String ALIAS_KEY = "alias";
    
    /**
     * Key for the operation name.
     * <BR>
     * Value is {@value}.
     */
    public static final String OP_KEY = "op";
    
    /**
     * Key for the argument list.
     * If no arguments are required then 
     * {@value} can be missing or an empty list.
     * <BR>
     * Value is {@value}.
     */
    public static final String ARGS_KEY = "args";

    private final Gson gson = new Gson();
    private final Map<String, ControlMBean<?>> mbeans = new HashMap<>();

    private static String getControlId(String type, String id, String alias) {
        return type + ":" + (alias == null ? id : alias);
    }

    /**
     * Handle a JSON control request.
     * 
     * The control action is executed directly
     * using the calling thread.
     * 
     * @param request the request
     * @return JSON response, JSON null if the request was not recognized.
     * @throws Exception on failure
     */
    public JsonElement controlRequest(JsonObject request) throws Exception {
        if (request.has(OP_KEY))
            return controlOperation(request);

        return JsonNull.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * <P>
     * All control service MBeans must be valid according
     * to {@link Controls#isControlServiceMBean(Class)}.
     * </P>
     * 
     * @see Controls#isControlServiceMBean(Class)
     */
    @Override
    public synchronized <T> String registerControl(String type, String id, String alias, Class<T> controlInterface,
            T control) {
        if (!Controls.isControlServiceMBean(controlInterface))
            throw new IllegalArgumentException();
        
        final String controlId = getControlId(type, id, alias);
        if (mbeans.containsKey(controlId)) {
            logger.error("Control id: {} already exists", controlId);
            throw new IllegalStateException();
        }

        logger.trace("Register control id: {}", controlId);
        mbeans.put(controlId, new ControlMBean<T>(controlInterface, control));
        return controlId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unregister(String controlId) {
        logger.trace("Unegister control id: {}", controlId);
        mbeans.remove(controlId);
    }

    /**
     * Handle a control operation.
     * An operation maps to a {@code void} method.
     * @param request Request to be executed.
     * @return JSON boolean true if the request was executed, false if it was not.
     * @throws Exception Exception executing the control instruction. 
     */
    private JsonElement controlOperation(JsonObject request) throws Exception {
        final String type = request.get(TYPE_KEY).getAsString();
        String alias = request.get(ALIAS_KEY).getAsString();
        final String controlId = getControlId(type, null, alias);
        
        logger.trace("Operation - control id: {}", controlId);

        ControlMBean<?> mbean;
        synchronized (this) {
            mbean = mbeans.get(controlId);
        }

        if (mbean == null)
            return new JsonPrimitive(Boolean.FALSE);

        String methodName = request.get(OP_KEY).getAsString();
        
        logger.trace("Operation method - control id: {} method: {}", controlId, methodName);
        
        int argumentCount = 0;
        JsonArray args = null;
        if (request.has(ARGS_KEY)) {
            args = request.getAsJsonArray(ARGS_KEY);
            argumentCount = args.size();
        }
            

        Method method = findMethod(mbean.getControlInterface(), methodName, argumentCount);

        if (method == null)
            return new JsonPrimitive(Boolean.FALSE);

        logger.trace("Execute operation - control id: {} method: {}", controlId, methodName);
        
        executeMethod(method, mbean.getControl(), getArguments(method, args));
        
        logger.trace("Execute completed - control id: {} method: {}", controlId, methodName);

        return new JsonPrimitive(Boolean.TRUE);
    }

    private Method findMethod(Class<?> controlInterface, String name, int argumentCount) {
        Method[] methods = controlInterface.getDeclaredMethods();

        for (Method method : methods) {
            if (!Modifier.isPublic(method.getModifiers()))
                continue;
            
            if (name.equals(method.getName()) && method.getParameterTypes().length == argumentCount)
                return method;
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object[] getArguments(Method method, JsonArray args) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        
        if (paramTypes.length == 0 || args == null || args.size() == 0)
            return null;
        
        assert paramTypes.length == args.size();
        
        Object[] oargs = new Object[paramTypes.length];
        for (int i = 0; i < oargs.length; i++) {
            final Class<?> pt = paramTypes[i];
            final JsonElement arg = args.get(i);
            Object jarg;
            
            if (String.class == pt) {
                if (arg instanceof JsonObject)
                    jarg = gson.toJson(arg);
                else
                    jarg = arg.getAsString();
            }
            else if (Integer.TYPE == pt)
                jarg = arg.getAsInt();
            else if (Long.TYPE == pt)
                jarg = arg.getAsLong();
            else if (Double.TYPE == pt)
                jarg = arg.getAsDouble();
            else if (Boolean.TYPE == pt)
                jarg = arg.getAsBoolean();
            else if (pt.isEnum())
                jarg = Enum.valueOf((Class<Enum>) pt, arg.getAsString());
            else
                throw new UnsupportedOperationException(pt.getName());
            
            oargs[i] = jarg;
        }
        return oargs;
    }

    private void executeMethod(Method method, Object control, Object[] arguments)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        method.invoke(control, arguments);
    }

    @Override
    public <T> T getControl(String type, String alias, Class<T> controlInterface) {
        String controlId = getControlId(type, null, alias);
        ControlMBean<?> bean = getControlMBean(controlId, controlInterface);        
        return bean != null ? controlInterface.cast(bean.getControl()) : null;
    }

    @Override
    public <T> String getControlId(String type, String alias, Class<T> controlInterface) {
        String controlId = getControlId(type, null, alias);
        return getControlMBean(controlId, controlInterface) != null ? controlId : null;
    }

    private synchronized <T> ControlMBean<?> getControlMBean(String controlId, Class<T> controlInterface) {
        ControlMBean<?> bean = mbeans.get(controlId);
        if (bean == null)
            return null;
        if (bean.getControlInterface() != controlInterface)
            return null;
        return bean;
    }
}
