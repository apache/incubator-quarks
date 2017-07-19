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
package org.apache.edgent.metrics;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.apache.edgent.execution.services.ServiceContainer;
import org.apache.edgent.function.BiConsumer;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

/**
 * Utility helpers for configuring and starting a Metric {@code JmxReporter}
 * or a {@code ConsoleReporter}.
 * <p>
 * This class is not thread safe.
 */
public class MetricsSetup {
    private static final TimeUnit durationsUnit = TimeUnit.MILLISECONDS;
    private static final TimeUnit ratesUnit = TimeUnit.SECONDS;
    private static final String FOLDER_METRICS = "/metrics";

    private final MetricRegistry metricRegistry;
    private MBeanServer mBeanServer;

    /**
     * Returns a new {@link MetricsSetup} for configuring metrics.
     *
     * @param services ServiceContainer to use to add services to
     * @param registry the registry to use for the application
     * @return a {@link MetricsSetup} instance
     */
    public static MetricsSetup withRegistry(ServiceContainer services, MetricRegistry registry) {
        final MetricsSetup setup = new MetricsSetup(registry);
        services.addService(MetricRegistry.class, registry);
        services.addCleaner(setup.new MetricOpletCleaner());
        return setup;
    }

    /**
     * Use the specified {@code MBeanServer} with this metric setup.
     *  
     * @param mBeanServer the MBean server used by the metric JMX reporter
     * @return this
     */
    public MetricsSetup registerWith(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
        return this;
    }

    /**
     * Starts the metric {@code JMXReporter}. If no MBeanServer was set, use 
     * the virtual machine's platform MBeanServer.
     * @param jmxDomainName JMX domain name to use when creating JMX MXBean object names.
     * @return this
     */
    public MetricsSetup startJMXReporter(String jmxDomainName) {
        final JmxReporter reporter = JmxReporter.forRegistry(registry()).
                registerWith(mbeanServer())
                .inDomain(jmxDomainName).createsObjectNamesWith(new MetricObjectNameFactory())
                .convertDurationsTo(durationsUnit).convertRatesTo(ratesUnit)
                .filter(MetricFilter.ALL).build();
        reporter.start();
        return this;
    }

    /**
     * Starts the metric {@code CsvReporter}. If no MBeanServer was set, use the
     * virtual machine's platform MBeanServer.
     * 
     * @param pathMetrics
     *            pathname where the metric files are stored. If the path does
     *            not exist or is null is created in a standard directory,
     *            called metrics
     * @return this
     */
    public MetricsSetup startCSVReporter(String pathMetrics) {

        if (pathMetrics == null) { // pathMetrics is NULL
            pathMetrics = createDefaultDirectory();
        } else {
            File directory = new File(pathMetrics);
            if (!directory.exists() && !directory.mkdirs()) {
                pathMetrics = createDefaultDirectory();
            }
        }

        final CsvReporter reporter = CsvReporter.forRegistry(registry()).formatFor(Locale.US).convertRatesTo(ratesUnit)
                .convertDurationsTo(durationsUnit).build(new File(pathMetrics));
        reporter.start(1, TimeUnit.SECONDS);
        return this;
    }
    
    /**
     * Starts the metric {@code ConsoleReporter} polling every second.
     * @return this
     */
    public MetricsSetup startConsoleReporter() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(registry()).convertRatesTo(ratesUnit)
                .convertDurationsTo(durationsUnit).build();
        reporter.start(1, TimeUnit.SECONDS);
        return this;
    }

    private MetricsSetup(MetricRegistry registry) {
        this.metricRegistry = registry;
    }
    
    private MetricRegistry registry() {
        return metricRegistry;
    }

    private MBeanServer mbeanServer() {
        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mBeanServer;
    }

    /**
     * Creates a default directory for metrics.
     * 
     * @return directory.getPath()
     */
    private String createDefaultDirectory() {
        Path currentRelativePath = Paths.get("");
        String pathMetrics = currentRelativePath.toAbsolutePath().toString() + FOLDER_METRICS;
        File directory = new File(pathMetrics);
        
        if (!directory.mkdirs()) {
            // Log: "Could not create the directory log"
        } else {
            // Log: "The directory was created successfully: "
        }

        return directory.getPath();
    }
    
    private class MetricOpletCleaner implements BiConsumer<String, String> {
        
        // Use the project's BiConsumer (serializable) implementation to avoid 
        // a dependency on Java 8's functional interfaces.
        private static final long serialVersionUID = 1L;

        @Override
        public void accept(String jobId, String opletId) {

            // TODO logging System.err.println("CLEANING:" + jobId + " --" + opletId);
            registry().removeMatching(new MetricFilter() {
                @Override
                public boolean matches(String name, Metric metric) {
                    return name.endsWith(
                            new StringBuilder().append('.').append(jobId).append('.').append(opletId).toString());
                }
            });
        }
    }
}
