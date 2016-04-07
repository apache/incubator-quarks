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
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import quarks.connectors.file.FileStreams;
import quarks.connectors.file.FileWriterCycleConfig;
import quarks.connectors.file.FileWriterFlushConfig;
import quarks.connectors.file.FileWriterPolicy;
import quarks.connectors.file.FileWriterRetentionConfig;
import quarks.console.server.HttpServer;
import quarks.providers.development.DevelopmentProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Write a TStream<String> to files.
 */
public class HdfsWriterApp {
    private final String hdfsDirectory;
    private final String basePathname;
    private static final String baseLeafname = "HdfsWriterSample";

    public static void main(String[] args) throws Exception {
        if (args.length != 1)
            throw new Exception("missing HDFS URI");
        HdfsWriterApp writer = new HdfsWriterApp(args[0]);
        writer.run();
    }

    /**
     *
     * @param directory an existing directory to create files in
     */
    public HdfsWriterApp(String hdfsDirectory) throws IOException {
        HdfsAdmin admin = new HdfsAdmin(URI.create(hdfsDirectory), new Configuration());
        if (!dir.exists())
            throw new IllegalArgumentException("directory doesn't exist");
        this.hdfsDirectory = hdfsDirectory;
        basePathname = directory+"/"+baseLeafname;
    }
    
    public void run() throws Exception {
        DevelopmentProvider tp = new DevelopmentProvider();
        
        // build the application / topology
        
        Topology t = tp.newTopology("FileSample producer");
        
        FileWriterPolicy<String> policy = new FileWriterPolicy<String>(
                FileWriterFlushConfig.newImplicitConfig(),
                FileWriterCycleConfig.newCountBasedConfig(5),
                FileWriterRetentionConfig.newFileCountBasedConfig(3));

        // create a tuple stream to write out
        AtomicInteger cnt = new AtomicInteger();
        TStream<String> stream = t.poll(() -> {
                String str = String.format("sample tuple %d %s",
                        cnt.incrementAndGet(), new Date().toString());
                System.out.println("created tuple: "+str);
                return str;
            }, 1, TimeUnit.SECONDS);
        
        // write the stream
        FileStreams.textFileWriter(stream, () -> basePathname, () -> policy);
        
        // run the application / topology
        System.out.println("starting the producer writing to directory " + directory);
        System.out.println("Console URL for the job: "
                + tp.getServices().getService(HttpServer.class).getConsoleUrl());
        tp.submit(t);
    }

}
