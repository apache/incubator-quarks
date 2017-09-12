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
package org.apache.edgent.test.connectors.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.http.HttpClients;
import org.apache.edgent.connectors.http.HttpResponders;
import org.apache.edgent.connectors.http.HttpStreams;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.tester.Condition;
import org.apache.edgent.topology.tester.Tester;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * These tests go against http://httpbin.org
 * a freely available web-server for testing requests.
 *
 */
public class HttpTest {

    private static final String prop1 = "abc";
    private static final String prop2 = "42";

    public String getProp1() {
        return prop1;
    }

    public String getProp2() {
        return prop2;
    }

    @Test
    public void testGet() throws Exception {
        
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/get";
        
        TStream<String> rc = HttpStreams.<String,String>requests(
                topology.strings(url),
                HttpClients::noAuthentication,
                t-> HttpGet.METHOD_NAME,
                t->t,
                HttpResponders.inputOn200());
        
        Tester tester =  topology.getTester();
        
        Condition<List<String>> endCondition = tester.streamContents(rc, url);
        
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.valid());
    }
    
    @Test
    public void testPost() throws Exception {
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/post";
        
        TStream<String> stream = topology.strings(url);
        TStream<String> rc = HttpStreams.<String, String>requestsWithBody(
                stream, HttpClients::noAuthentication,
                t -> HttpPost.METHOD_NAME, 
                t-> t, t-> new ByteArrayEntity(t.getBytes()),
                HttpResponders.inputOn200());
        
        Tester tester = topology.getTester();
        
        Condition<List<String>> endCondition = tester.streamContents(rc, url);
        
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.valid());
    }
    
    @Test
    public void testPut() throws Exception {
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/put";
        
        TStream<String> stream = topology.strings(url);
        TStream<String> rc = HttpStreams.<String, String>requestsWithBody(
                stream, HttpClients::noAuthentication,
                t -> HttpPut.METHOD_NAME, 
                t-> t, t-> new ByteArrayEntity(t.getBytes()),
                HttpResponders.inputOn200());
        
        Tester tester = topology.getTester();
        
        Condition<List<String>> endCondition = tester.streamContents(rc, url);
        
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.valid());
    }
    
    @Test
    public void testDelete() throws Exception {
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/delete";
        
        TStream<String> stream = topology.strings(url);
        TStream<String> rc = HttpStreams.<String, String>requests(
                stream, HttpClients::noAuthentication,
                t -> HttpDelete.METHOD_NAME, 
                t-> t, HttpResponders.inputOn200());
        
        Tester tester = topology.getTester();
        
        Condition<List<String>> endCondition = tester.streamContents(rc, url);
        
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.valid());
    }
    
    @Test
    public void testStatusCode() throws Exception {
        
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/status/";
        
        TStream<Integer> rc = HttpStreams.<Integer,Integer>requests(
                topology.collection(Arrays.asList(200, 404, 202)),
                HttpClients::noAuthentication,
                t-> HttpGet.METHOD_NAME,
                t-> url + Integer.toString(t),
                (t,resp) -> resp.getStatusLine().getStatusCode());
        
        Tester tester =  topology.getTester();
        
        Condition<List<Integer>> endCondition = tester.streamContents(rc, 200, 404, 202);
        
        tester.complete(ep,  new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.valid());
    }
    
    /**
     * Test basic authentication, first with valid user/password
     * and then with invalid (results in 401).
     * @throws Exception on failure
     */
    @Test
    public void testBasicAuthentication() throws Exception {
        
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        String url = "http://httpbin.org/basic-auth/";
        
        TStream<Integer> rc = HttpStreams.<String,Integer>requests(
                topology.strings("A", "B"),
                () -> HttpClients.basic("usA", "pwdA4"),
                t-> HttpGet.METHOD_NAME,
                t-> url + "us" + t + "/pwd" + t + "4",
                (t,resp) -> resp.getStatusLine().getStatusCode());
        
        Tester tester =  topology.getTester();
        
        Condition<List<Integer>> endCondition = tester.streamContents(rc, 200, 401);
        
        tester.complete(ep,  new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.getResult().toString(), endCondition.valid());
    }
    
    @Test
    public void testJsonGet() throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("a", getProp1());
        request.addProperty("b", getProp2());

        testJsonGet(request);
    }

    public void testJsonGet(JsonObject request) throws Exception {
        
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        final String url = "http://httpbin.org/get?";
        
        TStream<JsonObject> rc = HttpStreams.getJson(
                topology.collection(Arrays.asList(request)),
                HttpClients::noAuthentication,
                t-> url + "a=" + t.get("a").getAsString() + "&b=" + t.get("b").getAsString()
                );
        
        TStream<Boolean> resStream = rc.map(j -> {
            assertTrue(j.has("request"));
            assertTrue(j.has("response"));
            JsonObject req = j.getAsJsonObject("request");
            JsonObject res = j.getAsJsonObject("response");
            
            assertTrue(res.has("status"));
            assertTrue(res.has("entity"));           
            
            assertEquals(req, res.getAsJsonObject("entity").getAsJsonObject("args"));
            return true;
        }
        );
        
        rc.print();
         
        Tester tester =  topology.getTester();
        
        Condition<List<Boolean>> endCondition = tester.streamContents(resStream, true);
        
        tester.complete(ep,  new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.getResult().toString(), endCondition.valid());
    }
    
    @Test
    public void testJsonDelete() throws Exception {
        
        DirectProvider ep = new DirectProvider();
        
        Topology topology = ep.newTopology();
        
        final String url = "http://httpbin.org/delete?";
        
        JsonObject request = new JsonObject();
        request.addProperty("a", getProp1());
        request.addProperty("b", getProp2());
        
        TStream<JsonObject> stream = topology.collection(Arrays.asList(request));
        TStream<JsonObject> rc = HttpStreams.deleteJson(
                stream, HttpClients::noAuthentication,
                t-> url + "a=" + t.get("a").getAsString() + "&b=" + t.get("b").getAsString()
                );
        
        TStream<Boolean> resStream = rc.map(j -> {
            assertTrue(j.has("request"));
            assertTrue(j.has("response"));
            JsonObject req = j.getAsJsonObject("request");
            JsonObject res = j.getAsJsonObject("response");
            
            assertTrue(res.has("status"));
            assertTrue(res.has("entity"));           
            
            assertEquals(req, res.getAsJsonObject("entity").getAsJsonObject("args"));
            return true;
        }
        );
        
        rc.print();
         
        Tester tester =  topology.getTester();
        
        Condition<List<Boolean>> endCondition = tester.streamContents(resStream, true);
        
        tester.complete(ep,  new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        
        assertTrue(endCondition.getResult().toString(), endCondition.valid());
    }
    
    @Test
    public void testJsonPost() throws Exception {

        DirectProvider ep = new DirectProvider();

        Topology topology = ep.newTopology();

        final String url = "http://httpbin.org/post";

        JsonObject body = new JsonObject();
        body.addProperty("foo", getProp1());
        body.addProperty("bar", getProp2());

        TStream<JsonObject> stream = topology.collection(Arrays.asList(body));
        TStream<JsonObject> rc = HttpStreams.postJson(
                stream, HttpClients::noAuthentication, t -> url,
                t -> t);
        TStream<Boolean> resStream = rc.map(j -> {
            assertTrue(j.has("request"));
            assertTrue(j.has("response"));
            JsonObject req = j.getAsJsonObject("request");
            JsonObject res = j.getAsJsonObject("response");

            assertTrue(res.has("status"));
            assertTrue(res.has("entity"));

            assertEquals(req, res.getAsJsonObject("entity").getAsJsonObject("json"));
            return true;
        });

        rc.print();
        Tester tester = topology.getTester();
        Condition<List<Boolean>> endCondition = tester.streamContents(resStream, true);
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        assertTrue(endCondition.getResult().toString(), endCondition.valid());
    }

    @Test
    public void testJsonPut() throws Exception {

        DirectProvider ep = new DirectProvider();

        Topology topology = ep.newTopology();

        final String url = "http://httpbin.org/put";

        JsonObject body = new JsonObject();
        body.addProperty("foo", getProp1());
        body.addProperty("bar", getProp2());

        TStream<JsonObject> stream = topology.collection(Arrays.asList(body));
        TStream<JsonObject> rc = HttpStreams.putJson(
                stream, HttpClients::noAuthentication, t -> url,
                t -> t);
        TStream<Boolean> resStream = rc.map(j -> {
            assertTrue(j.has("request"));
            assertTrue(j.has("response"));
            JsonObject req = j.getAsJsonObject("request");
            JsonObject res = j.getAsJsonObject("response");

            assertTrue(res.has("status"));
            assertTrue(res.has("entity"));

            assertEquals(req, res.getAsJsonObject("entity").getAsJsonObject("json"));
            return true;
        });

        rc.print();
        Tester tester = topology.getTester();
        Condition<List<Boolean>> endCondition = tester.streamContents(resStream, true);
        tester.complete(ep, new JsonObject(), endCondition, 10, TimeUnit.SECONDS);
        assertTrue(endCondition.getResult().toString(), endCondition.valid());
    }
}
