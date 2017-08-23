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
package org.apache.edgent.connectors.command.runtime;

import org.apache.edgent.function.Consumer;

/**
 * A {@code Consumer<String>>} to write data to a command's input.
 * <P>
 * The supplied {@code cmd} is used to start the command
 * and restart it upon process termination/error if so configured.
 * </P>
 */
public class CommandWriter extends CommandConnector implements Consumer<String>, AutoCloseable {
  private static final long serialVersionUID = 1L;
  private ProcessWriter currentConsumer;
  
  /**
   * Create a consumer to write UTF8 string data to a command's input.
   * <P>
   * Each write is followed by a flush() though that only helps to
   * reduce the time it takes to notice that a cmd has failed.
   * Supposedly "successfully written and flushed" values are not guaranteed to
   * have been received by a cmd even following restart.
   * </P>
   * 
   * @param cmd the builder to use to start the process
   * @param restart true to restart the process upon termination or
   * write error.
   */
  public CommandWriter(ProcessBuilder cmd, boolean restart) {
    super(cmd, restart);
  }

  protected void start() throws InterruptedException {
    super.start();
    currentConsumer = new ProcessWriter(getCurrentProcess());
  }
  
  protected void closeProcess() {
    currentConsumer = null;
    super.closeProcess();
  }

  @Override
  public void accept(String value) {
    for (;;) {
      try {
        if (currentConsumer != null) {
          try {
            currentConsumer.accept(value);
            logger.trace("WROTE: {}", value);
            return;
          } 
          catch (RuntimeException e) {
            closeProcess(); // and loop/retry
          }
        }
        else if (canStart()) {
          logger.debug("STARTING for: {}", value);
          start();  // and loop/retry
        }
        else {
          // not restartable. toss it on the floor
          return;
        }
      }
      catch (InterruptedException e) {
        // toss it on the floor
        return;
      }
    }
  }

}