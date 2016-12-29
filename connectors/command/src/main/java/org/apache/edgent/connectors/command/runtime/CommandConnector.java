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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for source / sink specific command connectors.
 * <P>
 * The lifetime of a CommandConnector is that of its Command's execution lifetime.
 * In the case of a "one shot" command (e.g., a periodicSource's cmd) the
 * lifetime may be brief - {@code restart==false}.
 * </P>
 * <P>
 * Many command connector uses will involve long running commands, sources
 * and sinks that want to be robust in the face of inadvertent command
 * termination/failures - (@code restart==true}.
 * </P>
 */
abstract class CommandConnector implements AutoCloseable {
  static final Logger logger = LoggerFactory.getLogger(CommandConnector.class);

  private final ProcessBuilder cmd;
  private final boolean restart;
  private Process currentProcess;
  private long numStarts;
  private long lastStartTimestamp;
  private final int restartDelayMsec = 1_000;
  
  
  CommandConnector(ProcessBuilder cmd, boolean restart) {
    this.cmd = cmd;
    this.restart = restart;
  }
  
  protected boolean canStart() {
    return restart || numStarts==0;
  }
  
  protected Process getCurrentProcess() {
    return currentProcess;
  }
  
  protected void start() throws InterruptedException {
    if (!canStart())
      throw new IllegalStateException();
    closeProcess();
    try {
      numStarts++;
      // ensure we don't thrash on continuous restarts
      long now = System.currentTimeMillis();
      if (now < lastStartTimestamp + restartDelayMsec) {
        logger.info("Sleeping before restarting cmd {}", toCmdForMsg());
        Thread.sleep(restartDelayMsec);
        now = System.currentTimeMillis();
      }
      lastStartTimestamp = now;
      
      currentProcess = cmd.start();
      
      logger.debug("Started cmd {}", toCmdForMsg());
    }
    catch (IOException e) {
      logger.error("Unable to start cmd {}", toCmdForMsg(), e);
    }
  }
  
  protected void closeProcess() {
    if (currentProcess != null) {
      if (currentProcess.getOutputStream() != null) {
        try {
          currentProcess.getOutputStream().close();
        }
        catch (IOException e) {
          logger.error("Unable to close OutputStream to cmd {}", toCmdForMsg(), e);
        }
      }
      currentProcess.destroy();
      currentProcess = null;
    }
  }

  @Override
  public void close() {
    closeProcess();
  }
  
  String toCmdForMsg() {
    return cmd.command().toString();
  }

}
