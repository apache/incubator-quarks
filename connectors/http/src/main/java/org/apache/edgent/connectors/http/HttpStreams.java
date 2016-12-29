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

package org.apache.edgent.connectors.http;

import java.nio.charset.StandardCharsets;

import org.apache.edgent.connectors.http.runtime.HttpRequester;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.function.Function;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.function.UnaryOperator;
import org.apache.edgent.topology.TStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.gson.JsonObject;


/**
 * HTTP streams.
 *
 */
public class HttpStreams {
    
    /**
     * Make an HTTP GET request with JsonObject. <br>
     * 
     * Method specifically works with JsonObjects. For each JsonObject in the stream, 
     * HTTP GET request is executed on provided uri. As a result, Response is added to
     * the response TStream.
     * <br>
     * 
     * Sample usage:<br>
     * 
     * <pre>
     * {@code
     *     DirectProvider ep = new DirectProvider();
     *     Topology topology = ep.newTopology();
     *     final String url = "http://httpbin.org/get?";
     * 
     *     JsonObject request1 = new JsonObject();
     *     request1.addProperty("a", "abc");
     *     request1.addProperty("b", "42");
     * 
     *     TStream<JsonObject> stream = topology.collection(Arrays.asList(request1));
     *     TStream<JsonObject> rc = HttpStreams.getJson(stream,
     *             HttpClients::noAuthentication,
     *             t -> url + "a=" + t.get("a").getAsString() + "&b="
     *                     + t.get("b").getAsString());
     * }
     * </pre>
     * 
     * <br>
     * See <i>HttpTest</i> for example. <br>
     * 
     * @param stream - JsonObject TStream.
     * @param clientCreator - CloseableHttpClient supplier preferably created using {@link HttpClients}
     * @param uri - URI function which returns URI string
     * @return TStream of JsonObject which contains responses of GET requests
     * 
     * @see HttpStreams#requests(TStream, Supplier, Function, Function, BiFunction)
     */
    public static TStream<JsonObject> getJson(TStream<JsonObject> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<JsonObject,String> uri) {
        
        return HttpStreams.<JsonObject,JsonObject>requests(stream, clientCreator,
            t -> HttpGet.METHOD_NAME, uri, HttpResponders.json());
    }
    
    /**
     * Make an HTTP DELETE request with JsonObject. <br>
     * 
     * Method specifically works with JsonObjects. For each JsonObject in the
     * stream, HTTP DELETE request is executed on provided uri. As a result,
     * Response is added to the response TStream. <br>
     * 
     * Sample usage:<br>
     * 
     * <pre>
     * {@code
     *     DirectProvider ep = new DirectProvider();
     *     Topology topology = ep.newTopology();
     *     final String url = "http://httpbin.org/delete?";
     * 
     *     JsonObject request = new JsonObject();
     *     request.addProperty("a", "abc");
     *     request.addProperty("b", "42");
     * 
     *     TStream<JsonObject> stream = topology.collection(Arrays.asList(request));
     *     TStream<JsonObject> rc = HttpStreams.deleteJson(stream,
     *             HttpClients::noAuthentication,
     *             t -> url + "a=" + t.get("a").getAsString() + "&b="
     *                     + t.get("b").getAsString());
     * }
     * </pre>
     * 
     * <br>
     * See <i>HttpTest</i> for example. <br>
     * 
     * @param stream - JsonObject TStream.
     * @param clientCreator - CloseableHttpClient supplier preferably created using {@link HttpClients}
     * @param uri - URI function which returns URI string
     * @return TStream of JsonObject which contains responses of DELETE requests
     * 
     * @see HttpStreams#requests(TStream, Supplier, Function, Function, BiFunction)
     */
    public static TStream<JsonObject> deleteJson(TStream<JsonObject> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<JsonObject,String> uri) {
        
        return HttpStreams.<JsonObject,JsonObject>requests(stream, clientCreator,
            t -> HttpDelete.METHOD_NAME, uri, HttpResponders.json());
    }
    
    /**
     * Make an HTTP POST request with JsonObject. <br>
     * 
     * Method specifically works with JsonObjects. For each JsonObject in the stream, 
     * HTTP POST request is executed on provided uri. Request body is filled using
     * HttpEntity provided by body function. As a result, Response is added to
     * the response TStream.<br>
     * 
     * Sample usage:<br>
     * 
     * <pre>
     * {@code
     *     DirectProvider ep = new DirectProvider();
     *     Topology topology = ep.newTopology();
     *     final String url = "http://httpbin.org/post";
     * 
     *     JsonObject body = new JsonObject();
     *     body.addProperty("foo", "abc");
     *     body.addProperty("bar", 42);
     * 
     *     TStream<JsonObject> stream = topology.collection(Arrays.asList(body));
     *     TStream<JsonObject> rc = HttpStreams.postJson(stream,
     *             HttpClients::noAuthentication, t -> url, t -> t);
     * }
     * </pre>
     * 
     * <br>
     * See HttpTest for example. <br>
     * 
     * @param stream - JsonObject TStream.
     * @param clientCreator - CloseableHttpClient supplier preferably created using {@link HttpClients}
     * @param uri - URI function which returns URI string
     * @param body - Function that returns JsonObject which will be set as a body for the request.
     * @return TStream of JsonObject which contains responses of POST requests
     * 
     * @see HttpStreams#requestsWithBody(TStream, Supplier, Function, Function, Function, BiFunction)
     */
    public static TStream<JsonObject> postJson(TStream<JsonObject> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<JsonObject, String> uri,
            UnaryOperator<JsonObject> body) {

        return HttpStreams.<JsonObject, JsonObject> requestsWithBody(stream,
                clientCreator, t -> HttpPost.METHOD_NAME, uri, 
                t -> new ByteArrayEntity(body.apply(t).toString().getBytes(StandardCharsets.UTF_8)),
                HttpResponders.json());
    }
    
    /**
     * Make an HTTP PUT request with JsonObject. <br>
     * 
     * Method specifically works with JsonObjects. For each JsonObject in the
     * stream, HTTP PUT request is executed on provided uri. Request body is
     * filled using HttpEntity provided by body function. As a result, Response
     * is added to the response TStream.<br>
     * 
     * Sample usage:<br>
     * 
     * <pre>
     * {@code
     *     DirectProvider ep = new DirectProvider();
     *     Topology topology = ep.newTopology();
     *     final String url = "http://httpbin.org/put";
     * 
     *     JsonObject body = new JsonObject();
     *     body.addProperty("foo", "abc");
     *     body.addProperty("bar", 42);
     * 
     *     TStream<JsonObject> stream = topology.collection(Arrays.asList(body));
     *     TStream<JsonObject> rc = HttpStreams.putJson(stream,
     *             HttpClients::noAuthentication, t -> url, t -> t);
     * }
     * </pre>
     * 
     * <br>
     * See HttpTest for example. <br>
     * 
     * @param stream - JsonObject TStream.
     * @param clientCreator - CloseableHttpClient supplier preferably created using {@link HttpClients}
     * @param uri - URI function which returns URI string
     * @param body - Function that returns JsonObject which will be set as a body for the request.
     * @return TStream of JsonObject which contains responses of PUT requests
     * 
     * @see HttpStreams#requestsWithBody(TStream, Supplier, Function, Function, Function, BiFunction)
     */
    public static TStream<JsonObject> putJson(TStream<JsonObject> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<JsonObject, String> uri,
            UnaryOperator<JsonObject> body) {
        return HttpStreams.<JsonObject, JsonObject> requestsWithBody(stream,
                clientCreator, t -> HttpPut.METHOD_NAME, uri, 
                t -> new ByteArrayEntity(body.apply(t).toString().getBytes(StandardCharsets.UTF_8)),
                HttpResponders.json());
    }
    
    /**
     * Make an HTTP request for each tuple on a stream.
     * <UL>
     * <LI>{@code clientCreator} is invoked once to create a new HTTP client
     * to make the requests.
     * </LI>
     *  <LI>
     * {@code method} is invoked for each tuple to define the method
     * to be used for the HTTP request driven by the tuple. A fixed method
     * can be declared using a function such as:
     * <UL style="list-style-type:none"><LI>{@code t -> HttpGet.METHOD_NAME}</LI></UL>
     *  </LI>
     *  <LI>
     * {@code uri} is invoked for each tuple to define the URI
     * to be used for the HTTP request driven by the tuple. A fixed method
     * can be declared using a function such as:
     * <UL style="list-style-type:none"><LI>{@code t -> "http://www.example.com"}</LI></UL>
     *  </LI>
     *  <LI>
     *  {@code response} is invoked after each request that did not throw an exception.
     *  It is passed the input tuple and the HTTP response. The function must completely
     *  consume the entity stream for the response. The return value is present on
     *  the stream returned by this method if it is non-null. A null return results
     *  in no tuple on the returned stream.
     *  
     *  </LI>
     *  </UL>
     *  
     * @param <T> Tuple type for input stream
     * @param <R> Tuple type for output stream
     * @param stream Stream to invoke HTTP requests.
     * @param clientCreator Function to create a HTTP client.
     * @param method Function to define the HTTP method.
     * @param uri Function to define the URI.
     * @param response Function to process the response.
     * @return Stream containing HTTP responses processed by the {@code response} function.
     * 
     * @see HttpClients
     * @see HttpResponders
     */
    public static <T,R> TStream<R> requests(TStream<T> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<T,String> method,
            Function<T,String> uri,
            BiFunction<T,CloseableHttpResponse,R> response) {
        
        return stream.map(new HttpRequester<T,R>(clientCreator, method, uri, response));
    }
    
    /**
     * Make an HTTP request with body for each tuple.<br>
     * 
     * @param <T> Tuple type for input stream
     * @param <R> Tuple type for output stream
     * @param stream Stream to invoke HTTP requests.
     * @param clientCreator Function to create a HTTP client.
     * @param method Function to define the HTTP method.
     * @param uri Function to define the URI.
     * @param body Function to define the HTTP request body
     * @param response Function to process the response.
     * @return Stream containing HTTP responses processed by the {@code response} function.
     * 
     * @see HttpStreams#requests(TStream, Supplier, Function, Function, BiFunction)
     * @see HttpClients
     * @see HttpResponders
     * 
     */
    public static <T, R> TStream<R> requestsWithBody(TStream<T> stream,
            Supplier<CloseableHttpClient> clientCreator,
            Function<T, String> method, 
            Function<T, String> uri,
            Function<T, HttpEntity> body,
            BiFunction<T, CloseableHttpResponse, R> response) {

        return stream.map(new HttpRequester<T, R>(clientCreator, method, uri, body, response));
    }
}

