/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quarks.samples.topology;

import quarks.console.server.HttpServer;
import quarks.providers.development.DevelopmentProvider;
import quarks.topology.TStream;
import quarks.topology.Topology;

import java.util.EnumMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SplitWithEnumSample {

    public enum LogSeverityEnum {

        ALERT(1), CRITICAL(2), ERROR(3), WARNING(4), NOTICE(5), INFO(6), DEBUG(7);

        private final int code;

        LogSeverityEnum(final int code) {
            this.code = code;
        }
    }

    public static void main(String[] args) throws Exception {
        DevelopmentProvider dtp = new DevelopmentProvider();

        Topology t = dtp.newTopology("SplitWithEnumSample");

        Random r = new Random();

        LogSeverityEnum[] values = LogSeverityEnum.values();
        TStream<String> d = t.poll(() -> values[r.nextInt(values.length)].toString()+ "_Log", 500, TimeUnit.MILLISECONDS);

        EnumMap<LogSeverityEnum, TStream<String>> categories = d
            .split(LogSeverityEnum.class, e -> LogSeverityEnum.valueOf(e.split("_")[0]));

        TStream<String> warnStream = categories.get(LogSeverityEnum.WARNING);
        TStream<String> errorStream = categories.get(LogSeverityEnum.ERROR);
        TStream<String> infoStream = categories.get(LogSeverityEnum.INFO);

        warnStream.sink(data -> System.out.println("warnStream = " + data));
        errorStream.sink(data -> System.out.println("errorStream = " + data));
        infoStream.sink(data -> System.out.println("infoStream = " + data));

        dtp.submit(t);

        System.out.println(dtp.getServices().getService(HttpServer.class).getConsoleUrl());
    }
}
