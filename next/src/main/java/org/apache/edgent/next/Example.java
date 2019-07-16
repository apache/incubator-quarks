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
package org.apache.edgent.next;

import java.util.concurrent.TimeUnit;

public class Example {

    public static void main(String[] args) {
        // Example 1 - Generic handler
        from("s7://192.168.167.210/0/0")
            .scrape(10, TimeUnit.SECONDS)
                .field("%DB500.DBX10:BOOL")
                .field("%DB500.DBX10:BOOL", Boolean.class)
            .handle((field, result, clazz) -> System.out.println(field + ": " + result));

        // Example 2 - Store to JDBC
        from("s7://192.168.167.210/0/0")
            .scrape(10, TimeUnit.SECONDS)
                .field("%DB500.DBX10:BOOL")
                .field("%DB500.DBX10:BOOL", Boolean.class)
            .transform(new JsonTransformer())
            .to(new JdbcSink());

        // Example 3 - Store to InfluxDB
        from("s7://192.168.167.210/0/0")
            .scrape(10, TimeUnit.SECONDS)
                .field("%DB500.DBX10:BOOL")
                .field("%DB500.DBX10:BOOL", Boolean.class)
            .to(new InfluxDbSink());

        // Example 4 - CRUNCH
        from("s7://192.168.167.210/0/0")
            .scrape(10, TimeUnit.SECONDS)
                .field("%DB500.DBX10:BOOL", Boolean.class).analyze()
                    .flank(UP).handle(...)
    }

    public static ScraperBuilder from(String plcConnection) {
        return new ScraperBuilder(plcConnection);
    }

    public static class ScraperBuilder {
        private final String connection;

        public ScraperBuilder(String connection) {
            this.connection = connection;
        }

        public FieldCollector scrape(int time, TimeUnit unit) {
            return new FieldCollector(this);
        }
    }

    private static class FieldCollector {
        private final ScraperBuilder scraperBuilder;

        public FieldCollector(ScraperBuilder scraperBuilder) {
            this.scraperBuilder = scraperBuilder;
        }

        private FieldCollector field(String fieldAdress) {
            return this;
        }

        private FieldCollector field(String fieldAdress, Class<?> clazz) {
            return this;
        }

        private void handle(ResultHandler handler) {

        }
    }

    @FunctionalInterface
    private interface ResultHandler {

        void handle(String fieldName, Object result, Class<?> clazz);

    }
}