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
package org.apache.edgent.oplet.plumbing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.edgent.function.BiFunction;
import org.apache.edgent.oplet.OpletContext;
import org.apache.edgent.oplet.core.FanIn;

/**
 * A tuple synchronization barrier.
 * <P>
 * {@code Barrier} has n input ports with tuple type {@code T}
 * and one output port with tuple type {@code List<T>}.
 * Once the oplet receives one tuple on each of its input ports,
 * it generates an output tuple containing one tuple from each input port.
 * It then awaits receiving the next collection of tuples.
 * Input port 0's tuple is in list[0], port 1's tuple in list[1], and so on.
 * </P><P>
 * Each input port has an associated queue of size {@code queueCapacity}.
 * The input port's {@code Consumer<T>.accept()} will block if it's queue is full. 
 * </P>
 *
 * @param <T> Type of the tuple.
 */
public class Barrier<T> extends FanIn<T, List<T>> {
    
    private final int queueCapacity;
    private Thread thread;
    private List<LinkedBlockingQueue<T>> iportQueues;
    
    /**
     * Create a new instance.
     * @param queueCapacity size of each input port's blocking queue
     */
    public Barrier(int queueCapacity) {
      this.queueCapacity = queueCapacity;
    }
    
    @Override
    public void initialize(OpletContext<T, List<T>> context) {
        super.initialize(context);
        
        thread = context.getService(ThreadFactory.class).newThread(() -> run());
        
        int numIports = getOpletContext().getInputCount();
        iportQueues = new ArrayList<>(numIports);
        for (int i = 0; i < numIports; i++)
          iportQueues.add(new LinkedBlockingQueue<>(queueCapacity));
        
        setReceiver(receiver());
    }
   
    @Override
    public void start() {
        thread.start();
    }
    
    protected BiFunction<T,Integer,List<T>> receiver() {
      return (tuple, iportIndex) -> {
        accept(tuple, iportIndex);
        return null;
      };
    }
    
    protected void accept(T tuple, int iportIndex) {
      try {
        iportQueues.get(iportIndex).put(tuple);
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }

    private void run() {
        while (!Thread.interrupted()) {
            try {
              List<T> list = new ArrayList<>(iportQueues.size());
              for (LinkedBlockingQueue<T> iport : iportQueues) {
                list.add(iport.take());
              }
              submit(list);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    @Override
    public void close() {
    }
    
}
