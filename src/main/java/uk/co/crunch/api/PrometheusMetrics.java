package uk.co.crunch.api;

import com.google.common.annotations.VisibleForTesting;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.io.Closeable;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

// More friendly, MetricRegistry-inspired Prometheus API wrapper
// FIXME Ideally want to inject application name into this (or create Spring wrapper)

public class PrometheusMetrics {
    private final ConcurrentMap<String,Metric> metrics = new ConcurrentHashMap<>();
    private final CollectorRegistry registry;
    private final String metricNamePrefix;
    private io.prometheus.client.Counter errorCounter;
    private Properties descriptionMappings = new Properties();

    public PrometheusMetrics() {
        this.registry = new CollectorRegistry(true);
        this.metricNamePrefix = "";
    }

    public PrometheusMetrics(final CollectorRegistry registry, final String metricNamePrefix) {
        this.registry = checkNotNull(registry);
        this.metricNamePrefix = fixIntendedName( checkNotNull(metricNamePrefix) ) + "_";
    }

    @VisibleForTesting
    public void setDescriptionMappings(final Properties props) {
        this.descriptionMappings = checkNotNull(props);
    }

    // Map Dropwizard Timer to a Prometheus Summary (I think)
    public Summary timer(String name) {
        return summary(name);
    }

    public Summary timer(String name, String desc) {
        return summary(name, desc);
    }

    public Histogram histogram(String name) {
        return getOrAdd(name, empty(), MetricBuilder.HISTOGRAMS);
    }

    public Histogram histogram(String name, String desc) {
        return getOrAdd(name, of(desc), MetricBuilder.HISTOGRAMS);
    }

    public Summary summary(String name) {
        return getOrAdd(name, empty(), MetricBuilder.SUMMARIES);
    }

    public Summary summary(String name, String desc) {
        return getOrAdd(name, of(desc), MetricBuilder.SUMMARIES);
    }

    public Counter counter(String name) {
        return getOrAdd(name, empty(), MetricBuilder.COUNTERS);
    }

    public Counter counter(String name, String desc) {
        return getOrAdd(name, of(desc), MetricBuilder.COUNTERS);
    }

    public Gauge gauge(String name) {
        return getOrAdd(name, empty(), MetricBuilder.GAUGES);
    }

    public Gauge gauge(String name, String desc) {
        return getOrAdd(name, of(desc), MetricBuilder.GAUGES);
    }

    public Error error(String name) {
        return incrementError(name, empty());
    }

    public Error error(String name, String desc) {
        return incrementError(name, of(desc));
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
    private <T extends Metric> T getOrAdd(String name, Optional<String> desc, MetricBuilder<T> builder) {
        final String adjustedName = metricNamePrefix + fixIntendedName(name);

        // Get/check existing local metric
        final Metric metric = metrics.get(adjustedName);
        if (metric != null) {
            if (builder.isInstance(metric)) {
                return (T) metric;
            }
            throw new IllegalArgumentException(adjustedName + " is already used for a different type of metric");
        }

        final String description = desc.orElse( firstNonNull( descriptionMappings.getProperty(adjustedName), adjustedName) );
        final T newMetric = builder.newMetric( adjustedName, description, this.registry);

        if (metrics.putIfAbsent(adjustedName, newMetric) != null) {
            throw new IllegalArgumentException("A metric named " + adjustedName + " already exists");
        }

        return (T) newMetric;
    }

    private Error incrementError(final String name, Optional<String> desc) {
        io.prometheus.client.Counter.Child counter = getErrorCounter(desc).labels(name);
        counter.inc();
        return new Error(counter);
    }

    @SuppressWarnings("unchecked")
    private synchronized io.prometheus.client.Counter getErrorCounter(Optional<String> desc) {
        if (this.errorCounter == null) {
            final String adjustedName = metricNamePrefix + "errors";
            final String description = desc.orElse( firstNonNull( descriptionMappings.getProperty(adjustedName), adjustedName) );
            this.errorCounter = registerPrometheusMetric( io.prometheus.client.Counter.build().name(adjustedName).help(description).labelNames("error_type").create(), registry);
        }
        return this.errorCounter;
    }

    private static String fixIntendedName(String name) {
        return name.replace('.','_')
                .replace('-','_')
                .replace('#','_')
                .toLowerCase();
    }

    private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Counter( registerPrometheusMetric(io.prometheus.client.Counter.build().name(name).help(desc).create(), registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Gauge> GAUGES = new MetricBuilder<Gauge>() {
            @Override
            public Gauge newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Gauge( registerPrometheusMetric( io.prometheus.client.Gauge.build().name(name).help(desc).create(), registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Gauge.class.isInstance(metric);
            }
        };

        MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Histogram( registerPrometheusMetric( io.prometheus.client.Histogram.build().name(name).help(desc).create(), registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<Summary> SUMMARIES = new MetricBuilder<Summary>() {
            @Override
            public Summary newMetric(final String name, final String desc, final CollectorRegistry registry) {
                return new Summary( registerPrometheusMetric( io.prometheus.client.Summary.build()
                        .name(name)
                        .help(desc)
                        .quantile(0.5, 0.01)    // Median
                        .quantile(0.75, 0.01)   // 75th percentile (1% tolerated error)
                        .quantile(0.9, 0.01)    // 90th percentile
                        .quantile(0.95, 0.01)   // 95th percentile
                        .quantile(0.99, 0.01)   // 99th percentile
                        .quantile(0.999, 0.01)  // 99.9th percentile
                        .create(), registry) );
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Summary.class.isInstance(metric);
            }
        };

        T newMetric(String name, String desc, final CollectorRegistry registry);
        boolean isInstance(Metric metric);
    }

    private static <T extends Collector> T registerPrometheusMetric(final T metric, CollectorRegistry registry) {
        try {
            registry.register(metric);
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Collector already registered")) {
                throw e;
            }
        }
        return metric;
    }

    private interface Metric {}

    public interface Context extends Closeable {
        void close();
    }

    public static class Counter implements Metric  {

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

    public static class Gauge implements Metric  {

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

    public static class Error implements Metric  {

        final private io.prometheus.client.Counter.Child promMetric;

        Error(final io.prometheus.client.Counter.Child promMetric) {
            this.promMetric = promMetric;
        }

        public double count() {
            return this.promMetric.get();
        }
    }

    public static class Summary implements Metric  {

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

        public Context time() {
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

    public static class Histogram implements Metric  {

        final private io.prometheus.client.Histogram promMetric;

        Histogram(final io.prometheus.client.Histogram promMetric) {
            this.promMetric = promMetric;
        }

        public Context time() {
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
