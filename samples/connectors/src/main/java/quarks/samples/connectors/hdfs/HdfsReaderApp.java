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
package quarks.samples.connectors.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import quarks.connectors.file.FileStreams;
import quarks.connectors.hdfs.HdfsStreams;
import quarks.console.server.HttpServer;
import quarks.providers.development.DevelopmentProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;

import java.io.IOException;
import java.net.URI;

/**
 * Watch a hdfsDirectory for files and convert their contents into a stream.
 */
public class HdfsReaderApp {
    private final String hdfsDirectory;
    private final FileSystem hdfs;
    private static final String baseLeafname = "HdfsReaderSample";

    public static void main(String[] args) throws Exception {
        if (args.length != 1)
            throw new Exception("missing HDFS URI");
        HdfsReaderApp reader = new HdfsReaderApp(args[0]);
        reader.run();
    }

    /**
     *
     * @param hdfsDirectory an existing directory to watch for file
     */
    public HdfsReaderApp(String hdfsDirectory) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsDirectory);
        this.hdfs =FileSystem.get(conf);
        Path path = new Path(hdfsDirectory);

        if (hdfs.exists(path) && hdfs.isDirectory(path)){
            this.hdfsDirectory = hdfsDirectory;
        }else {
            throw new IllegalArgumentException("HDFS directory doesn't exist");
        }
        HdfsAdmin admin = new HdfsAdmin(URI.create(hdfsDirectory), conf);
    }
    
    public void run() throws Exception {
        DevelopmentProvider tp = new DevelopmentProvider();
        
        // build the application / topology
        
        Topology t = tp.newTopology("HDFSSample consumer");

        // watch for files
        TStream<String> pathNames = FileStreams.directoryWatcher(t, () -> hdfsDirectory);
        
        // create a stream containing the files' contents.
        // use a preFn to include a separator in the results.
        // use a postFn to delete the file once its been processed.
        TStream<String> contents = HdfsStreams.textFileReader(pathNames,
                tuple -> "<PRE-FUNCTION> "+tuple, 
                (tuple,exception) -> {
                    // exercise a little caution in case the user pointed
                    // us at a directory with other things in it
                    if (tuple.contains("/"+baseLeafname+"_")) {
                        Path path = new Path(tuple);
                        try {
                            hdfs.delete(path, true);
                        } catch (IOException e) {
                            System.out.println("HDFS File " + tuple + " does not exists");
                            e.printStackTrace();
                        }
                    }
                    return null;
                });
        
        // print out what's being read
        contents.print();
        
        // run the application / topology
        System.out.println("starting the reader watching directory " + hdfsDirectory);
        System.out.println("Console URL for the job: "
                + tp.getServices().getService(HttpServer.class).getConsoleUrl());
        tp.submit(t);
    }

}
