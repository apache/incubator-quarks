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
package org.apache.edgent.topology.plumbing;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

import org.apache.edgent.function.ToIntFunction;
import org.apache.edgent.topology.TStream;

/**
 * A Load Balanced Splitter function.
 * <P>
 * This is intended to be used as an argument to {@link TStream#split(int, ToIntFunction)}.
 * The splitter maintains state for {@code numChannels} splitter channels,
 * tracking whether a channel is busy or free.
 * </P><P>
 * {@link #applyAsInt(Object) applyAsInt} awaits a free channel, marks
 * the channel as busy, and forwards the tuple to the channel.  The end
 * of the channel's pipeline must call {@link #channelDone(int)} to
 * signal that the channel is free.
 * </P>
 *
 * @param <T> Tuple type.
 * @see PlumbingStreams#parallelBalanced(TStream, int, org.apache.edgent.function.BiFunction) parallelBalanced
 */
public class LoadBalancedSplitter<T> implements ToIntFunction<T> {
  private static final long serialVersionUID = 1L;
  private final Semaphore gate;
  private final boolean[] chBusy;
  
  /**
   * Create a new splitter.
   * @param numChannels the number of splitter channels
   */
  public LoadBalancedSplitter(int numChannels) {
    if (numChannels < 1)
      throw new IllegalArgumentException("numChannels");
    chBusy = new boolean[numChannels];
    Arrays.fill(chBusy, false);
    gate = new Semaphore(numChannels);
  }
  
  /**
   * Signal that the channel is done processing the splitter supplied tuple.
   * @param channel the 0-based channel number
   */
  public synchronized void channelDone(int channel) {
    if (!chBusy[channel])
      throw new IllegalStateException("channel "+channel+" is not busy");
    chBusy[channel] = false;
    gate.release();
  }
  
  @Override
  public int applyAsInt(T value) {
    try {
      gate.acquire();
      synchronized(this) {
        for (int ch = 0; ch < chBusy.length; ch++) {
          if (!chBusy[ch]) {
            chBusy[ch] = true;
            return ch;
          }
        }
        throw new IllegalStateException("internal error");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted", e);
    }
  }

}
