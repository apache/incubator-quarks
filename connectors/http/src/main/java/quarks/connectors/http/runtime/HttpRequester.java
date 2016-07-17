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

package quarks.connectors.http.runtime;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import quarks.function.BiFunction;
import quarks.function.Function;
import quarks.function.Supplier;

/**
 * Function that processes HTTP requests at runtime.
 * 
 * @param <T> Tuple type of request stream
 * @param <R> Tuple type of result stream
 */
public class  HttpRequester<T,R> implements Function<T,R>{
    
    private static final long serialVersionUID = 1L;
    
    private final Supplier<CloseableHttpClient> clientCreator;
    private final Function<T,String> method;
    private final Function<T,String> url;
    private final BiFunction<T,CloseableHttpResponse,R> responseProcessor;
    private final Function<T, HttpEntity> entity;
    
    private CloseableHttpClient client;
    
    public HttpRequester(
            Supplier<CloseableHttpClient> clientCreator,
            Function<T,String> method,
            Function<T,String> url,
            BiFunction<T,CloseableHttpResponse,R> responseProcessor) {
        this.clientCreator = clientCreator;
        this.method = method;
        this.url = url;
        this.responseProcessor = responseProcessor;
        this.entity = null;
    }
    
    public HttpRequester(
            Supplier<CloseableHttpClient> clientCreator,
            Function<T,String> method,
            Function<T,String> url,
            Function<T, HttpEntity> entity,
            BiFunction<T,CloseableHttpResponse,R> responseProcessor) {
        this.clientCreator = clientCreator;
        this.method = method;
        this.url = url;
        this.entity = entity;
        this.responseProcessor = responseProcessor;
    }

    @Override
    public R apply(T t) {
        
        if (client == null)
            client = clientCreator.get();
        
        String m = method.apply(t);
        String uri = url.apply(t);
        HttpUriRequest request;
        
        switch (m) {
        
        case HttpGet.METHOD_NAME:
            request = new HttpGet(uri);
            break;
        case HttpDelete.METHOD_NAME:
            request = new HttpDelete(uri);
            break;
        case HttpPost.METHOD_NAME:
            request = new HttpPost(uri);
            break;
        case HttpPut.METHOD_NAME:
            request = new HttpPut(uri);
            break;
            
        default:
            throw new IllegalArgumentException();
        }
        
        // If entity is not null means http request should have a body
        if (entity != null) {
            
            HttpEntity body = entity.apply(t);

            if (request instanceof HttpEntityEnclosingRequest == false) {
                throw new IllegalArgumentException("Http request does not support body");
            }
            
            ((HttpEntityEnclosingRequest) request).setEntity(body);
        }
        
        try {
            try (CloseableHttpResponse response = client.execute(request)) {
                return responseProcessor.apply(t, response);
            }
             
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}