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
package org.apache.edgent.test.providers.direct.appservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.runtime.appservice.AppService;
import org.apache.edgent.topology.services.ApplicationService;
import org.junit.Test;

public class AppServiceTest {
    
    
    @Test
    public void testRegisterTopology() {
        DirectProvider direct = new DirectProvider();
        ApplicationService appService = AppService.createAndRegister(direct, direct);
        
        assertTrue(appService.getApplicationNames().isEmpty());
        
        appService.registerTopology("FirstApp", (t,c) -> t.strings("a"));      
        assertEquals(1, appService.getApplicationNames().size());       
        assertTrue(appService.getApplicationNames().contains("FirstApp"));
        
        appService.registerTopology("SecondApp", (t,c) -> t.strings("b"));      
        assertEquals(2, appService.getApplicationNames().size());       
        assertTrue(appService.getApplicationNames().contains("FirstApp"));
        assertTrue(appService.getApplicationNames().contains("SecondApp"));
    }
    
    @Test
    public void testRegisterJar() throws Exception {
        DirectProvider direct = new DirectProvider();
        ApplicationService appService = AppService.createAndRegister(direct, direct);
        
        String qd = System.getProperty("edgent.test.root.dir");
        assertNotNull(qd);
        File testAppsJar = new File(qd, "api/topology/build/lib/test/edgent.api.topology.APPS.TEST.jar");
        assertTrue(testAppsJar.exists());
        
        URL testAppsJarURL = testAppsJar.toURI().toURL();

        appService.registerJar(testAppsJarURL.toExternalForm(), null);
        
        assertEquals(3, appService.getApplicationNames().size());
        assertTrue(appService.getApplicationNames().contains("FirstJarApp"));
        assertTrue(appService.getApplicationNames().contains("SecondJarApp"));
        assertTrue(appService.getApplicationNames().contains("ThirdJarApp"));  
    }
}
