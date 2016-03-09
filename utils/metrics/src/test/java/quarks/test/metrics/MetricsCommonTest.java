/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015,2016 
*/
package quarks.test.metrics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import quarks.function.Consumer;
import quarks.metrics.oplets.CounterOp;
import quarks.metrics.oplets.RateMeter;
import quarks.oplet.JobContext;
import quarks.oplet.Oplet;
import quarks.oplet.OpletContext;
import quarks.oplet.core.AbstractOplet;
import quarks.oplet.core.Peek;

public class MetricsCommonTest {
    @Test
    public void counterOpHierachy() {
        assertTrue(Oplet.class.isAssignableFrom(CounterOp.class));
        assertTrue(AbstractOplet.class.isAssignableFrom(CounterOp.class));
        assertTrue(Peek.class.isAssignableFrom(CounterOp.class));
    }
    
    @Test
    public void rateMeterHierachy() {
        assertTrue(Oplet.class.isAssignableFrom(RateMeter.class));
        assertTrue(AbstractOplet.class.isAssignableFrom(RateMeter.class));
        assertTrue(Peek.class.isAssignableFrom(RateMeter.class));
    }
    
    @Test
    public void metricNameRateMeter() throws Exception {
        Context<Object,Object> ctx = new Context<>();
        ctx.addService(MetricRegistry.class, new MetricRegistry());
        
        RateMeter<Object> op = new RateMeter<>();
        op.initialize(ctx);
        assertNotNull(op.getMetricName());
        op.close();
    }

    @Test
    public void metricNullNameRateMeter() throws Exception {
        Context<Object,Object> ctx = new Context<>();
        RateMeter<Object> op = new RateMeter<>();
        
        op.initialize(ctx);
        assertNull(op.getMetricName());
        op.close();
    }

    @Test
    public void metricNameCounter() throws Exception {
        Context<Object,Object> ctx = new Context<>();
        ctx.addService(MetricRegistry.class, new MetricRegistry());
        
        CounterOp<Object> op = new CounterOp<>();
        op.initialize(ctx);
        assertNotNull(op.getMetricName());
        op.close();
    }

    @Test
    public void metricNullNameCounter() throws Exception {
        Context<Object,Object> ctx = new Context<>();
        CounterOp<Object> op = new CounterOp<>();
        
        op.initialize(ctx);
        assertNull(op.getMetricName());
        op.close();
    }

    private static class Context<I, O> implements OpletContext<I, O> {
        private final Map<Class<?>, Object> services = new HashMap<>();

        public <T> T addService(Class<T> serviceClass, T service) {
            return serviceClass.cast(services.put(serviceClass, service));
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return serviceClass.cast(services.get(serviceClass));
        }

        @Override
        public int getInputCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOutputCount() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("serial")
        @Override
        public List<? extends Consumer<O>> getOutputs() {
            List<Consumer<O>> outputs = new ArrayList<>();
            outputs.add(0, new Consumer<O>() {
                @Override
                public void accept(O value) {}
            });
            return outputs;
        }

        @Override
        public JobContext getJobContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String uniquify(String name) {
            return "unique." + name;
        }
    }
}
