package org.test;

import com.google.common.annotations.VisibleForTesting;
import io.prometheus.client.CollectorRegistry;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

// More friendly, MetricRegistry-inspired Prometheus API wrapper
// FIXME Ideally want to inject application name into this (or create Spring wrapper)
public class PrometheusMetrics {
    private final ConcurrentMap<String,Metric> metrics;
    private CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    public PrometheusMetrics() {
        this.metrics = new ConcurrentHashMap<String,Metric>();
    }

    @VisibleForTesting
    public void setCollectorRegistry(final CollectorRegistry registry) {
        this.registry = checkNotNull(registry);
    }

    // Map Dropwizard Timer to a Prometheus Summary (I think)
    public Summary timer(String name) {
        return summary(name);
    }

    public Histogram histogram(String name) {
        return getOrAdd(name, MetricBuilder.HISTOGRAMS);
    }

    public Summary summary(String name) {
        return getOrAdd(name, MetricBuilder.SUMMARIES);
    }

    public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    public Gauge gauge(String name) {
        return getOrAdd(name, MetricBuilder.GAUGES);
    }

//    public void inc(String name) {
//        // Increment, whether Counter or Gauge
//        counter(name).inc();
//    }
//
//    public void inc(String name, double incr) {
//        counter(name).inc(incr);
//    }
//
//    public void dec(String name) {
//        gauge(name).dec();
//    }
//
//    public void dec(String name, double incr) {
//        gauge(name).dec(incr);
//    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        }
        else if (metric == null) {
            try {
                return register(name, builder.newMetric( fixIntendedName(name), name, this.registry));
            }
            catch (IllegalArgumentException e) {
                if (e.getMessage().startsWith("Invalid metric name")) {
                    throw e;
                }

                final Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }

    private static String fixIntendedName(String name) {
        return name.replace('.','_').replace('-','_').replace('#','_');
    }

    private <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        if (metrics.putIfAbsent(name, metric) != null) {
            throw new IllegalArgumentException("A metric named " + name + " already exists");
        }

        return metric;
    }

    private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Counter( io.prometheus.client.Counter.build().name(name).help(desc).register(registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Gauge> GAUGES = new MetricBuilder<Gauge>() {
            @Override
            public Gauge newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Gauge( io.prometheus.client.Gauge.build().name(name).help(desc).register(registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Gauge.class.isInstance(metric);
            }
        };

        MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Histogram( io.prometheus.client.Histogram.build().name(name).help(desc).register(registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<Summary> SUMMARIES = new MetricBuilder<Summary>() {
            @Override
            public Summary newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Summary( io.prometheus.client.Summary.build().name(name).help(desc).register(registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Summary.class.isInstance(metric);
            }
        };

        T newMetric(String name, String desc, final CollectorRegistry registry);
        boolean isInstance(Metric metric);
    }

    private interface Metric {}

    public interface Context extends Closeable {
        void close();
    }

    static class Counter implements Metric  {

        final private io.prometheus.client.Counter promMetric;

        Counter(final io.prometheus.client.Counter promMetric) {
            this.promMetric = promMetric;
        }

        public Counter inc() {
            this.promMetric.inc();
            return this;
        }

        public Counter inc(double incr) {
            this.promMetric.inc(incr);
            return this;
        }
    }

    static class Gauge implements Metric  {

        final private io.prometheus.client.Gauge promMetric;

        Gauge(final io.prometheus.client.Gauge promMetric) {
            this.promMetric = promMetric;
        }

        public Gauge inc() {
            this.promMetric.inc();
            return this;
        }

        public Gauge inc(double incr) {
            this.promMetric.inc(incr);
            return this;
        }

        public Gauge dec() {
            this.promMetric.dec();
            return this;
        }

        public Gauge dec(double incr) {
            this.promMetric.dec(incr);
            return this;
        }
    }

    static class Summary implements Metric  {

        final private io.prometheus.client.Summary promMetric;

        Summary(final io.prometheus.client.Summary promMetric) {
            this.promMetric = promMetric;
        }

        public Summary update(double value) {
            return observe(value);
        }

        public Summary observe(double value) {
            this.promMetric.observe(value);
            return this;
        }

        Context time() {
            return new TimerContext( promMetric.startTimer() );
        }

        private static class TimerContext implements Context {

            final io.prometheus.client.Summary.Timer requestTimer;

            TimerContext(final io.prometheus.client.Summary.Timer requestTimer) {
                this.requestTimer = requestTimer;
            }

            @Override
            public void close() {
                requestTimer.observeDuration();
            }
        }
    }

    static class Histogram implements Metric  {

        final private io.prometheus.client.Histogram promMetric;

        Histogram(final io.prometheus.client.Histogram promMetric) {
            this.promMetric = promMetric;
        }

        Context time() {
            return new TimerContext( promMetric.startTimer() );
        }

        public Histogram update(double value) {
            return observe(value);
        }

        public Histogram observe(double value) {
            this.promMetric.observe(value);
            return this;
        }

        private static class TimerContext implements Context {

            final io.prometheus.client.Histogram.Timer requestTimer;

            TimerContext(final io.prometheus.client.Histogram.Timer requestTimer) {
                this.requestTimer = requestTimer;
            }

            @Override
            public void close() {
                requestTimer.observeDuration();
            }
        }
    }
}
