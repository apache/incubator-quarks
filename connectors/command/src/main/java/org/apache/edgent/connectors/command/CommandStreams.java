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
package org.apache.edgent.connectors.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.command.runtime.CommandReader;
import org.apache.edgent.connectors.command.runtime.CommandWriter;
import org.apache.edgent.function.Consumer;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * Connector for creating a TStream from a Command's / OS Process's output
 * and sinking a TStream to a Command's / OS Process's input.
 * <P>
 * e.g., run a network monitor command (like Tiger Shark) and ingest its output.
 */
public class CommandStreams {
  private CommandStreams() {}

  /**
   * Tokenize the specified {@code cmdString} in the exact same manner as
   * done by {@link Runtime#exec(String)}.
   * <P>
   * This function provides a convenience for creating a {@link ProcessBuilder}
   * for use by the other CommandStreams methods. 
   * </P>
   * <P>
   * Sample use:
   * <pre>{@code
   * ProcessBuilder cmd = new ProcessBuilder(tokenize("sh someShellCmd.sh and args"));
   * TStream<String> stream = CommandStreams.generate(org.apache.edgent.org.apache.edgent.topology, cmd);
   * }</pre>
   * 
   * @param cmdString a command string
   * @return the tokens
   */
  public static List<String> tokenize(String cmdString) {
    List<String> command = new ArrayList<>();
    StringTokenizer tok = new StringTokenizer(cmdString);
    while (tok.hasMoreTokens()) 
      command.add(tok.nextToken());
    return command;
  }
  
  /**
   * Create an endless {@code TStream<String>} from a long running command's output.
   * <P>
   * The supplied {@code cmd} is used to start the command.
   * A tuple is created for each UTF8 line read from the command's
   * {@link Process#getInputStream() output}.
   * The tuples contain output from stderr if cmd is configured to 
   * {@link ProcessBuilder#redirectErrorStream(boolean) redirect stderr to stdout}.
   * The command is restarted if a read from the command's output stream
   * returns EOF or an error. 
   * </P>
   * <P>
   * This is a convenience function equivalent to
   * {@code org.apache.edgent.org.apache.edgent.topology.generate(endlessCommandReader(cmd))}.
   * </P>
   * <P>
   * Sample use: create a stream of tuples for the output from a 
   * continuously running and restartable command:
   * <pre>{@code
   * ProcessBuilder cmd = new ProcessBuilder("myCommand");
   * TStream<String> cmdOutput = CommandStreams.generate(org.apache.edgent.org.apache.edgent.topology, cmd);
   * cmdOutput.print();
   * }</pre>
   * 
   * @param topology the org.apache.edgent.org.apache.edgent.topology to add the source stream to
   * @param cmd the {@link ProcessBuilder} to start the command
   * @return the source {@code TStream<String>}
   * 
   * @see #endlessCommandReader(ProcessBuilder)
   * @see #tokenize(String)
   */
  public static TStream<String> generate(Topology topology, ProcessBuilder cmd) {
    return topology.generate(endlessCommandReader(cmd));
  }
  
  /**
   * Create a {@code TStream<String>} from a periodically run command's output.
   * <P>
   * The supplied {@code cmd} is used to start the command
   * at the specified {@code period}.
   * The command's UTF8 {@link Process#getInputStream() output} is read until EOF
   * and a {@code List<String>} tuple is created containing the collected output.
   * The tuples contain output from stderr if the cmd is configured to 
   * {@link ProcessBuilder#redirectErrorStream(boolean) redirect stderr to stdout}.  
   * </P>
   * <P>
   * This is a convenience function equivalent to
   * {@code org.apache.edgent.org.apache.edgent.topology.poll(commandReaderList(cmd), period, units)}.
   * </P>
   * <P>
   * Sample use: create a stream of tuples containing the output 
   * from a periodically run command:
   * <pre>{@code
   * ProcessBuilder cmd = new ProcessBuilder("date");
   * TStream<List<String>> cmdOutput = 
   *      CommandStreams.periodicSource(org.apache.edgent.org.apache.edgent.topology, cmd, 2, TimeUnit.SECONDS);
   * cmdOutput.print();
   * }</pre>
   * 
   * @param topology the org.apache.edgent.org.apache.edgent.topology to add the source stream to
   * @param cmd the {@link ProcessBuilder} to start the command
   * @param period the period to run the command and collect its output
   * @param units TimeUnit for {@code period}
   * @return the source {@code TStream<List<String>>}
   * 
   * @see #commandReaderList(ProcessBuilder)
   * @see #tokenize(String)
   */
  public static TStream<List<String>> periodicSource(Topology topology,
      ProcessBuilder cmd, long period, TimeUnit units) {
    return topology.poll(commandReaderList(cmd), period, units);
  }
  
  /**
   * Sink a {@code TStream<String>} to a command's input.
   * <P>
   * The supplied {@code cmd} is used to start the command.
   * Each tuple is written as UTF8 and flushed to the command's {@link Process#getOutputStream() input}.
   * The command is restarted if a write encounters an error. 
   * </P>
   * <P>
   * While each write is followed by a flush() that only helps to
   * reduce the time it takes to notice that cmd has failed and restart it.
   * Supposedly "successfully written and flushed" values are not guaranteed to
   * have been received by a cmd across restarts.
   * </P>
   * <P>
   * This is a convenience function equivalent to
   * {@code stream.sink(commandWriter(cmd))}
   * </P>
   * <P>
   * Sample use: write a stream of tuples to the input of a command:
   * <pre>{@code
   * TStream<String> stream = org.apache.edgent.org.apache.edgent.topology.strings("one", "two", "three");
   * ProcessBuilder cmd = new ProcessBuilder("cat").redirectOutput(new File("/dev/stdout"));
   * CommandStreams.sink(stream, cmd);
   * }</pre>
   * 
   * @param stream the stream to sink
   * @param cmd the {@link ProcessBuilder} to start the command
   * @return a {@link TSink}
   * 
   * @see #commandWriter(ProcessBuilder)
   * @see #tokenize(String)
   */
  public static TSink<String> sink(TStream<String> stream, ProcessBuilder cmd) {
    return stream.sink(commandWriter(cmd));
  }
    
  /**
   * Create an endless {@code Supplier<String>} for ingesting a long running command's output.
   * <P>
   * This method is particularly helpful in creating a sensor or source connector
   * class that hides the fact that it uses a command, enabling it to be used
   * like any other sensor/connector.
   * </P>
   * For example:
   * <pre><code>
   * // ingest the sensor data
   * TStream&lt;MySensorData&gt; stream = org.apache.edgent.org.apache.edgent.topology.generate(new MySensor());
   *
   * // MySensor class
   * class MySensor implements Supplier&lt;MySensorData&gt; {
   *   private String[] cmd = new String[] {"mySensorCmd", "arg1"};
   *   private Supplier&lt;String&gt; commandReader = 
   *     CommandStreams.endlessCommandReader(new ProcessBuilder(cmd));
   *       
   *   // implement Supplier&lt;MySensorData&gt;.get()
   *   public MySensorData get() {
   *     // get the next line from the cmd and create a MySensorData tuple from it
   *     return createMySensorData(commandReader.get());
   *   }
   * }
   * </code></pre>
   * <P>
   * The supplied {@code cmd} is used to start the command.
   * A call to {@link Supplier#get()} reads the next UTF8 line from the command's
   * {@link Process#getInputStream() output}.
   * The returned strings contain output from stderr if the cmd is configured to 
   * {@link ProcessBuilder#redirectErrorStream(boolean) redirect stderr to stdput}.  
   * The command is restarted if a read from the command's output stream
   * returns EOF or an error.
   * </P>
   * 
   * @param cmd the {@link ProcessBuilder} to start the command
   * @return the {@code Supplier<String>}
   * 
   * @see #generate(Topology, ProcessBuilder)
   * @see #tokenize(String)
   */
  public static Supplier<String> endlessCommandReader(ProcessBuilder cmd) {
    return new Supplier<String>() {
      private static final long serialVersionUID = 1L;
      Supplier<Iterable<String>> reader = new CommandReader(cmd, true);
      Iterator<String> iter = null;
      @Override
      public String get() {
        if (iter == null) {
          iter = reader.get().iterator();
        }
        if (iter.hasNext()) {
          return iter.next();
        }
        else {
          // presumably a shutdown condition
          return null;
        }
      }
    };
  }
  
  /**
   * Create a {@code Supplier<List<String>>} to ingest a command's output.
   * <P>
   * This method is particularly helpful in creating a sensor or source connector
   * class that hides the fact that it uses a command, enabling it to be used
   * like any other sensor/connector.
   * </P>
   * For example:
   * <pre><code>
   * // ingest the sensor data
   * TStream&lt;MySensorData&gt; stream = org.apache.edgent.org.apache.edgent.topology.periodicSource(new MySensor());
   *
   * // MySensor class
   * class MySensor implements Supplier&lt;MySensorData&gt; {
   *   private String[] cmd = new String[] {"mySensorCmd", "arg1"};
   *   private Supplier&lt;List&lt;String&gt;&gt; commandReader = 
   *     CommandStreams.commandReaderList(new ProcessBuilder(cmd));
   *       
   *   // implement Supplier&lt;MySensorData&gt;.get()
   *   public MySensorData get() {
   *     // get the cmd output and create a MySensorData tuple from it
   *     return createMySensorData(commandReader.get());
   *   }
   * }
   * </code></pre>
   * <P>
   * The supplied {@code cmd} is used to start the command.
   * A call to {@link Supplier#get()} reads the command's UTF8
   * {@link Process#getInputStream() input stream} until an EOF or error
   * and returns a {@code List<String>} of the collected input.
   * The tuples contain output from stderr if the cmd is configured to 
   * {@link ProcessBuilder#redirectErrorStream(boolean) redirect stderr to stdout}.
   * </P>
   * 
   * @param cmd the {@link ProcessBuilder} to start the command
   * @return the {@code Supplier<List<String>>} for the command
   * 
   * @see #periodicSource(Topology, ProcessBuilder, long, TimeUnit)
   * @see #tokenize(String)
   */
  public static Supplier<List<String>> commandReaderList(ProcessBuilder cmd) {
    return new Supplier<List<String>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public List<String> get() {
        try (CommandReader supplier
                = new CommandReader(cmd, false))
        {
          Iterator<String> iter = supplier.get().iterator();
          List<String> list = new ArrayList<>();
          while (iter.hasNext())
            list.add(iter.next());
          return list;
        }
      }
    };
  }
 
  /**
   * Create a {@code Consumer<String>} to write UTF8 string data to a command's input.
   * <P>
   * This method is particularly helpful in creating a sink connector
   * that hides the fact that it uses a command, enabling it to be used
   * like a native connector.
   * </P>
   * For example:
   * <pre><code>
   * // sink a stream to my connector
   * TStream&lt;MySensorData&gt; stream = ...;
   * stream.sink(new MySinkConnector());
   *
   * // MySinkConnector class
   * class MySinkConnector implements Consumer&lt;MySensorData&gt; {
   *   private String[] cmd = new String[] {"mySinkCmd", "arg1"};
   *   private Consumer&lt;String&gt; commandWriter = 
   *     CommandStreams.commandWriter(new ProcessBuilder(cmd));
   *       
   *   // implement Consumer&lt;MySensorData&gt;.accept()
   *   public void accept(MySensorData data) {
   *     // convert the data to a string and write it to the cmd
   *     commandWriter.accept(convertMySensorData(data));
   *   }
   * }
   * </code></pre>
   * <P>
   * The supplied {@link ProcessBuilder cmd} is used to start the command.
   * Each call to {@link Consumer#accept(Object) accept(String)} writes a 
   * UTF8 string to the command's {@link Process#getOutputStream() input}.
   * Each write is followed by a flush.
   * The command is restarted if a write encounters an error. 
   * </P>
   * <P>
   * While each write is followed by a flush() that only helps to
   * reduce the time it takes to notice that cmd has failed and restart it.
   * Supposedly "successfully written and flushed" values are not guaranteed to
   * have been received by a cmd across restarts.
   * </P>
   * 
   * @param cmd the {@link ProcessBuilder} to start the command
   * @return the {@code Consumer<String>} for the command
   * 
   * @see #sink(TStream, ProcessBuilder)
   * @see #tokenize(String)
   */
  public static Consumer<String> commandWriter(ProcessBuilder cmd) {
    return new CommandWriter(cmd, true);
  }

}
