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
package org.apache.edgent.android.topology;

import org.apache.edgent.android.oplet.RunOnUIThread;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Function;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.plumbing.PlumbingStreams;

import android.app.Activity;

/**
 * Stream utilities for an Android {@code Activity}.
 */
public class ActivityStreams {
    
    /**
    * Sink a stream executing the sinker function on the
    * activity's UI thread.
    * <BR>
    * For each tuple {@code t} on {@code stream}
    * the method {@code sinker.accept(t)} will be
    * called on the UI thread.
    *
    * @param activity Activity
    * @param stream Stream to be sinked.
    * @param sinker Function that will be executed on the UI thread.
    * @return sink element representing termination of this stream.
    *
    * @see org.apache.edgent.topology.TStream#sink(org.apache.edgent.function.Consumer)
    */
    public static <T> TSink<T> sinkOnUIThread(Activity activity, TStream<T> stream, Consumer<T> sinker) { 
        return stream.pipe(new RunOnUIThread<T>(activity)).sink(sinker);
    }
    
    /**
    * Map tuples on a stream executing the mapper function on the
    * activity's UI thread.
    * <BR>
    * For each tuple {@code t} on {@code stream}
    * the method {@code mapper.apply(t)} will be
    * called on the UI thread. The return from the
    * method will be present on the returned stream
    * if it is not null. Any processing downstream
    * executed against the returned stream is executed
    * on a different thread to the UI thread.
    *
    * @param activity Activity
    * @param stream Stream to be sinked.
    * @param mapper Function that will be executed on the UI thread.
    * @param ordered True if tuple ordering must be maintained after the
    * execution on the UI thread. False if ordering is not required.
    * @return Stream that will contain tuples of type {@code U} mapped from this
    *         stream's tuples.
    *
    * @see org.apache.edgent.topology.TStream#map(org.apache.edgent.function.Function)
    */
    public static <T,U> TStream<U> mapOnUIThread(Activity activity, TStream<T> stream, Function<T,U> mapper, boolean ordered) {  
        
        // Switch to the UI thread
        stream = stream.pipe(new RunOnUIThread<T>(activity));
        
        // execute the map on the UI thread
        TStream<U> resultStream = stream.map(mapper);
        
        // Switch back to a non-ui thread
        return PlumbingStreams.isolate(resultStream, ordered);
    }
}
