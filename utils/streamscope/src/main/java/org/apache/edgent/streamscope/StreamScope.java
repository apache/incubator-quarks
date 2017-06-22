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
package org.apache.edgent.streamscope;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Functions;
import org.apache.edgent.function.Predicate;
import org.apache.edgent.streamscope.mbeans.StreamScopeRegistryMXBean;

/**
 * A Stream "oscilloscope" for capturing stream tuples for analysis / debug.
 * <P>
 * A {@code StreamScope} is expected to be used as parameter to
 * {@link org.apache.edgent.streamscope.oplets.StreamScope} oplet.
 * </P><P>
 * A {@link TriggerManager} controls which tuples are captured.
 * A {@link BufferManager} controls the retention policy for captured tuples.
 * </P><P>
 * A {@link Sample} is created for each captured tuple containing the tuple
 * (not copied) and capture timestamps.  Samples are retrieved using {@link #getSamples()}.
 * </P><P>
 * Sample capture can be enabled/disabled ({@link #setEnabled(boolean)}.
 * It is disabled by default.  Capture can also be paused and resumed via
 * the {@code TriggerManager}.
 * </P><P>
 * StreamScope instances are typically registered in and located via
 * a {@link StreamScopeRegistry} runtime service 
 * and {@link StreamScopeRegistryMXBean} runtime ControlService.
 * </P>
 * @see StreamScopeRegistry
 * See {@code org.apache.edgent.providers.development.DevelopmentProvider}
 * 
 * @param <T> Tuple type
 */
public class StreamScope<T> implements Consumer<T> {
  private static final long serialVersionUID = 1L;
  private final BufferManager<T> buffer = new BufferManager<>();
  private final TriggerManager<T> trigger = new TriggerManager<>();
  private boolean isEnabled;
  
  /**
   * A captured tuple.
   * <P>
   * The Sample captures the tuple, and the system time and nanoTime
   * when the tuple was captured.
   * </P>
   *
   * @param <T> Tuple type.
   */
  public static class Sample<T> {
    private final long ts;
    private final long nanoTime;
    private final T tuple;
    
    Sample(T tuple) {
      this.ts = System.currentTimeMillis();
      this.nanoTime = System.nanoTime();
      this.tuple = tuple;
    }
    
    /**
     * Capture time in msec since the epoch.
     * @return the timestamp
     * @see System#currentTimeMillis()
     */
    public long timestamp() {
      return ts;
    }
    
    /**
     * Capture time in nanoTime.
     * @return the nanoTime
     * @see System#nanoTime()
     */
    public long nanoTime() {
      return nanoTime;
    }
    
    /**
     * The captured tuple.
     * @return the tuple
     */
    public T tuple() {
      return tuple;
    }
    
    @Override
    public String toString() {
      return "ts="+ts+" nano="+nanoTime+" tuple="+tuple;
    }
  }
  
  /**
   * Control the retention of captured tuples.
   * <P>
   * Captured tuples are retained until either:
   * <ul>
   * <li>a maximum retention count is exceeded</li>
   * <li>TODO a maximum retention time is exceeded</li>
   * </ul> 
   * <P>
   * The default configuration is a maxCount of 10.
   * </P>
   * 
   * @param <T> Tuple type
   */
  public static class BufferManager<T> {
    private List<Sample<T>> buffer = Collections.emptyList();
    private int maxCount = 10;
    private long period;
    private TimeUnit unit;
    
    // TODO timer based eviction
    // TODO look at using edgent Windows / WindowImpl for the buffer.
    //      Can window type/"size" be changed?
    //      Does it support simultaneous maxCount & maxAge?
    
    List<Sample<T>> getSamples() {
      return Collections.unmodifiableList(buffer);
    }
    
    /**
     * Set the maximum number of tuples to retain.
     * <P>
     * The capture buffer is cleared.
     * </P>
     * @param maxCount the maximum number of tuples to retain.
     *        Specify 0 to disable count based retention.
     */
    public void setMaxRetentionCount(int maxCount) {
      this.maxCount = maxCount;
      allocate();
    }
    
    /**
     * Set the maximum retention time of a tuple.
     * <P>
     * The capture buffer is cleared.
     * </P>
     * @param age the amount of time to retain a tuple.
     *        Specify 0 to disable age based retention.
     * @param unit {@link TimeUnit}
     */
    public void setMaxRetentionTime(long age, TimeUnit unit) {
      if (age < 0)
        throw new IllegalArgumentException("age");
      Objects.requireNonNull(unit, "unit");
      this.period = age;
      this.unit = unit;
      
      throw new IllegalStateException("setMaxRetentionTime is NYI");
    }
    
    /**
     * Get the number of tuples in the capture buffer.
     * @return the count.
     */
    int getCount() {
      return buffer.size();
    }
    
    void release() {
      buffer = Collections.emptyList();
    }
    
    void allocate() {
      buffer = new LinkedList<>();
    }
    
    void add(Sample<T> sample) {
      if (maxCount > 0 && buffer.size() >= maxCount)
        buffer.remove(0);
      buffer.add(sample);
    }
    
    @Override
    public String toString() {
      return "size="+getCount()+" maxCount="+maxCount+" maxAge="+period+(unit==null ? "" : unit);
    }
    
  }
  
  /**
   * Control what triggers capturing of tuples.
   * <P>
   * The following modes are supported:
   * <ul>
   * <li>continuous - capture every tuple</li> 
   * <li>by-count - capture every nth tuple</li>
   * <li>by-time - capture based on time elapsed since the last capture</li>
   * <li>by-Predicate - capture based on evaluating a predicate</li>
   * </ul>
   * <P>
   * Tuple capture can be temporarily paused via {@link TriggerManager#setPaused(boolean) setPaused}.
   * Pausing capture does not clear the capture buffer.
   * </P><P>
   * Capture processing can be automatically paused when a particular event
   * has occurred via {@link TriggerManager#setPauseOn(Predicate) setPauseOn}.
   * The pause predicate is evaluated after processing each received tuple.
   * Use {@link TriggerManager#setPaused(boolean)} setPaused} to re-enable capture
   * following a triggered pause.
   * </P><P>
   * The default configuration is continuous (by-count==1) and not paused.
   * </P>
   * 
   * @param <T> Tuple type
   */
  public static class TriggerManager<T> {
    private Predicate<T> predicate = Functions.alwaysTrue();
    private Predicate<T> pauseOnPredicate = Functions.alwaysFalse();
    private boolean paused = false;
    
    /**
     * Test if the tuple should be captured.
     * @param tuple the tuple
     * @return true to capture the tuple.
     */
    boolean test(T tuple) {
      if (paused)
        return false;
      boolean b = predicate.test(tuple);
      paused = pauseOnPredicate.test(tuple);
      return b;
    }
    
    /**
     * Set capture paused control
     * @param paused true to pause, false to clear pause.
     */
    public void setPaused(boolean paused) {
      this.paused = paused;
    }
    
    /**
     * Is capture paused?
     * @return true if paused
     */
    public boolean isPaused() {
      return paused;
    }
    
    /**
     * Set a pause-on predicate.  Capture is paused if the predicate
     * returns true;
     * @param predicate the predicate
     * @see Functions#alwaysFalse()
     */
    public void setPauseOn(Predicate<T> predicate) {
      Objects.requireNonNull(predicate, "predicate");
      pauseOnPredicate = predicate;
    }
    
    /**
     * Capture the first and every nth tuple
     * @param count the nth value interval
     */
    public void setCaptureByCount(int count) {
      if (count == 1)
        setCaptureByPredicate(Functions.alwaysTrue());
      else
        setCaptureByPredicate(newByCountPredicate(count));
    }
    
    /**
     * Capture the 1st tuple and then the next tuple after {@code period}
     * {@code unit} time has elapsed since the previous captured tuple.
     * 
     * @param elapsed time to delay until next capture
     * @param unit {@link TimeUnit}
     */
    public void setCaptureByTime(long elapsed, TimeUnit unit) {
      setCaptureByPredicate(newByTimePredicate(elapsed, unit));
    }
    
    /**
     * Capture a tuple if the {@code predicate} test of the tuple returns true.
     * @param predicate the predicate
     */
    public void setCaptureByPredicate(Predicate<T> predicate) {
      Objects.requireNonNull(predicate, "predicate");
      this.predicate = predicate;
    }
    
    private static <T> Predicate<T> newByCountPredicate(int count) {
      if (count < 1)
        throw new IllegalArgumentException("count");
      return new Predicate<T>() {
        private static final long serialVersionUID = 1L;
        int byCount = count;
        int curCount = -1;  // capture 1st and every byCount-nth

        @Override
        public boolean test(T value) {
          return ++curCount % byCount == 0;
        }
      };
    }
    
    private static <T> Predicate<T> newByTimePredicate(long elapsed, TimeUnit unit) {
      if (elapsed < 1)
        throw new IllegalArgumentException("elapsed");
      Objects.requireNonNull(unit, "unit");
      return new Predicate<T>() {
        private static final long serialVersionUID = 1L;
        private long nextTime;

        @Override
        public boolean test(T value) {
          long now = System.currentTimeMillis();
          if (now > nextTime) {
            nextTime = now + unit.toMillis(elapsed);
            return true;
          }
          return false;
        }
      };
    }
    
    @Override
    public String toString() {
      return "paused="+paused+" pauseOnPredicate="+pauseOnPredicate+" predicate="+predicate;
    }

  }
  
  /**
   * Create a new instance.
   * <P>
   * Sample capture is disabled.
   * </P>
   */
  public StreamScope() {
  }
  
  /**
   * Is tuple capture enabled?
   * @return true if enabled.
   */
  public boolean isEnabled() {
    return isEnabled;
  }
  
  /**
   * Enable or disable tuple capture.
   * <P>
   * Disabling releases the capture buffer.
   * </P>
   * @param isEnabled true to enable, false to disable.
   */
  public synchronized void setEnabled(boolean isEnabled) {
    if (this.isEnabled != isEnabled) {
      if (!isEnabled)
        buffer.release();
      buffer.allocate();
      this.isEnabled = isEnabled;
    }
  }
  
  /**
   * Get the {@link BufferManager}
   * @return the manager
   */
  public BufferManager<T> bufferMgr() {
    return buffer;
  }
  
  /**
   * Get the {@link TriggerManager}
   * @return the manager
   */
  public TriggerManager<T> triggerMgr() {
    return trigger;
  }
  
  /**
   * Get all captured tuples.
   * <P>
   * The returned samples are removed from the capture buffer.
   * </P>
   * @return unmodifiable list of captured samples
   */
  public synchronized List<Sample<T>> getSamples() {
    List<Sample<T>> tmp = buffer.getSamples();
    buffer.allocate();
    return tmp;
  }

  /**
   * Get the number of Samples currently captured
   * @return the count
   */
  public synchronized int getSampleCount() {
    return buffer.getCount();
  }

  @Override
  public synchronized void accept(T tuple) {
    if (!isEnabled())
      return;
    if (trigger.test(tuple))
      buffer.add(new Sample<T>(tuple));
  }

  @Override
  public String toString() {
    return "isEnabled="+isEnabled
        +" bufferMgr={"+bufferMgr()+"}"
        +" triggerMgr={"+triggerMgr()+"}";
  }
}

