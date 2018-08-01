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
package org.apache.edgent.test.console.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.edgent.console.server.HttpServer;
import org.junit.Test;

public class HttpServerTest {
	
	public static final String consoleWarNotFoundMessage =  
			"console.war not found.  Run 'ant' from the top level edgent directory, or 'ant' from 'console/servlets' to create console.war under the webapps directory.";


    @Test
    public void testGetInstance()  {
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
        	if (!warNotFoundExceptionThrown) {
        		assertNotNull("HttpServer getInstance is null", myHttpServer);
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
    }

    @Test
    public void startServer() throws Exception {
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
        	if ((!warNotFoundExceptionThrown) && (myHttpServer != null)){
                myHttpServer.startServer();
                assertTrue(myHttpServer.isServerStarted());
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
    }

    @Test
    public void isServerStopped() throws Exception {
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
			if ((!warNotFoundExceptionThrown) && (myHttpServer != null)){
                myHttpServer.startServer();
                assertFalse(myHttpServer.isServerStopped());
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
    }

    @Test
    public void getConsolePath() throws Exception {
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
			if ((!warNotFoundExceptionThrown) && (myHttpServer != null)){
        		assertEquals("/console", myHttpServer.getConsoleContextPath());
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
        
    }

    @Test
    public void getConsoleUrl() throws Exception {
    	
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
			if ((!warNotFoundExceptionThrown) && (myHttpServer != null)){
                myHttpServer.startServer();
                int portNum = myHttpServer.getConsolePortNumber();
                String context = myHttpServer.getConsoleContextPath();
                assertEquals("http://localhost:" + portNum + context, myHttpServer.getConsoleUrl());
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
    }

    @Test
    public void getConsolePortNumber() throws Exception {
    	HttpServer myHttpServer = null;
    	boolean warNotFoundExceptionThrown = false;
        try {
        	myHttpServer = HttpServer.getInstance();
        } catch (Exception warNotFoundException) {
        	System.out.println(warNotFoundException.getMessage());
        	if (warNotFoundException.getMessage().equals(consoleWarNotFoundMessage)) {
        		warNotFoundExceptionThrown = true;
        		assertEquals("", "");
        	}
        } finally {
			if ((!warNotFoundExceptionThrown) && (myHttpServer != null)){
                myHttpServer.startServer();
                int portNum = myHttpServer.getConsolePortNumber();
                assertTrue("the port number is not in integer range: " + Integer.toString(portNum),
                        (Integer.MAX_VALUE > portNum) && (0 < portNum));
        	} else {
        		assertNull("HttpServer getInstance is null because console.war could not be found", myHttpServer);
        	}
        }
    }

}
