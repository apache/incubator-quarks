/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.metrics.oplets;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import quarks.oplet.OpletContext;
import quarks.oplet.core.Peek;

/**
 * Base for metrics oplets which use a single metric object.
 */
public abstract class SingleMetricAbstractOplet<T> extends Peek<T> {

    private static final long serialVersionUID = -6679532037136159885L;
    private final String shortMetricName;
    private String metricName;

    protected SingleMetricAbstractOplet(String name) {
        this.shortMetricName = name;
    }

    /**
     * Returns the name of the metric used by this oplet for registration.
     * The name uniquely identifies the metric in the {@link MetricRegistry}.
     * <p>
     * The name of the metric is {@code null} prior to oplet initialization,
     * or if this oplet has not been initialized with a 
     * {@code MetricRegistry}.
     * 
     * @return the name of the metric used by this oplet.
     */
    public String getMetricName() {
        return metricName;
    }

    protected abstract Metric getMetric();

    @Override
    public final void initialize(OpletContext<T, T> context) {
        super.initialize(context);

        MetricRegistry registry = context.getService(MetricRegistry.class);
        if (registry != null) {
            this.metricName = context.uniquify(shortMetricName);
            registry.register(getMetricName(), getMetric());
        }
    }

    @Override
    public final void close() throws Exception {
        MetricRegistry registry = getOpletContext().getService(MetricRegistry.class);
        if (registry != null) {
            registry.remove(getMetricName());
        }
    }
}
