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
package org.apache.edgent.test.fvt.iot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.runtime.appservice.AppService;
import org.apache.edgent.runtime.jsoncontrol.JsonControlService;
import org.apache.edgent.topology.mbeans.ApplicationServiceMXBean;
import org.apache.edgent.topology.services.ApplicationService;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class IotAppServiceIT {
    
    @Test
    public void testAppService() throws Exception {
        
        DirectProvider provider = new DirectProvider();
        
        JsonControlService control = new JsonControlService();
        provider.getServices().addService(ControlService.class, control);        
        AppService.createAndRegister(provider, provider);
        
        IotTestApps.registerApplications(provider);       
        
        JsonObject submitAppOne = newSubmitRequest("AppOne");        
        JsonElement crr = control.controlRequest(submitAppOne);      
        assertTrue(crr.getAsBoolean());
    }
    
    @Test(expected=ClassNotFoundException.class)
    public void testAppsNotInClasspath() throws ClassNotFoundException {
        Class.forName("org.apache.edgent.test.org.apache.edgent.org.apache.edgent.topology.services.TestApplications");
    }
    
    @Test
    //@Ignore("This test relies on an existing war in a given location ... need to refactor this")
    public void testAppServiceJar() throws Exception {
        
        DirectProvider provider = new DirectProvider();
        
        JsonControlService control = new JsonControlService();
        provider.getServices().addService(ControlService.class, control);        
        AppService.createAndRegister(provider, provider);

        File testAppsJar = getServerJar();
        assertNotNull(testAppsJar);
        assertTrue(testAppsJar.exists());
        System.out.println("Using server jar at: " + testAppsJar.toString());

        URL testAppsJarURL = testAppsJar.toURI().toURL();
        JsonObject registerJar = newRegisterJarRequest(testAppsJarURL.toExternalForm());       
        JsonElement crr = control.controlRequest(registerJar);    
        assertTrue(crr.getAsBoolean());
        
        Thread.sleep(500);
        
        JsonObject submitAppTwo = newSubmitRequest("SecondJarApp");        
        crr = control.controlRequest(submitAppTwo);      
        assertTrue(crr.getAsBoolean());
        
        Thread.sleep(500);
    }
    
    public static JsonObject newSubmitRequest(String name) {
        JsonObject submitApp = new JsonObject();   
        submitApp.addProperty(JsonControlService.TYPE_KEY, ApplicationServiceMXBean.TYPE);
        submitApp.addProperty(JsonControlService.ALIAS_KEY, ApplicationService.ALIAS);
        JsonArray args = new JsonArray();
        args.add(new JsonPrimitive(name));
        args.add(new JsonObject());
        submitApp.addProperty(JsonControlService.OP_KEY, "submit");
        submitApp.add(JsonControlService.ARGS_KEY, args); 
        
        return submitApp;
    }

    public static JsonObject newRegisterJarRequest(String jarURL) {
        JsonObject submitApp = new JsonObject();   
        submitApp.addProperty(JsonControlService.TYPE_KEY, ApplicationServiceMXBean.TYPE);
        submitApp.addProperty(JsonControlService.ALIAS_KEY, ApplicationService.ALIAS);
        JsonArray args = new JsonArray();
        args.add(new JsonPrimitive(jarURL));
        args.add(new JsonObject());
        submitApp.addProperty(JsonControlService.OP_KEY, "registerJar");
        submitApp.add(JsonControlService.ARGS_KEY, args); 
        
        return submitApp;
    }

    private File getServerJar() {
        return new File("target/test-resources/test-appservice-applications.jar");
    }

}
