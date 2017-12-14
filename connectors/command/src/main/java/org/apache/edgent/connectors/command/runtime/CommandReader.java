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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.edgent.function.Supplier;

/**
 * Create a {@code Supplier<Iterable<String>>} to ingest a command's output.
 * <P>
 * The supplied {@code cmd} is used to start the command
 * and restart it upon process termination/error if so configured.
 * </P>
 * <P>
 * The iterator returned by {@link Iterable#iterator()} returns
 * {@code hasNext()==true} until a read from {@link Process#getOutputStream()}
 * returns EOF or an IOError.
 */
public class CommandReader extends CommandConnector implements Supplier<Iterable<String>>, AutoCloseable {
  private static final long serialVersionUID = 1L;
  private Iterator<String> currentSupplierIterator;
  
  /**
   * Create a supplier of UTF8 strings from a command's output.
   * 
   * @param cmd the {@link ProcessBuilder} to use to start the command
   * @param restart when true, restart the command upon termination, EOF, or
   * read error.
   */
  public CommandReader(ProcessBuilder cmd, boolean restart) {
    super(cmd, restart);
  }

  protected void start() throws InterruptedException {
    super.start();
    currentSupplierIterator = new ProcessReader(getCurrentProcess()).get().iterator();
  }
  
  protected void closeProcess() {
    currentSupplierIterator = null;
    super.closeProcess();
  }
  
  @Override
  public Iterable<String> get() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {

          @Override
          public boolean hasNext() {
            try {
              for(;;) {
                if (currentSupplierIterator != null) {
                  boolean hasNext = currentSupplierIterator.hasNext();
                  if (hasNext) {
                    return true;
                  }
                  else {
                    // no more from that process.  close and loop/retry.
                    closeProcess();
                  }
                }
                else if (canStart()) {
                  start(); // and loop/retry
                }
                else {
                  return false; // no more input
                }
              }
            }
            catch (InterruptedException e) {
              return false;
            }
          }

          @Override
          public String next() {
            if (currentSupplierIterator != null)
              return currentSupplierIterator.next();
            else
              throw new NoSuchElementException();
          }
          
        };
      }
      
    };
  }

}