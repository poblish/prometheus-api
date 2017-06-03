package org.test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.TestableTimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.test.PrometheusMetrics.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// FIXME:
// Class prefix?
// [done] Handle dots in names, Perhaps real name = Class name + '_' + corrected supplied name
// [partial] Descriptions?
// ==> Allow them to be specified... but if not...
// ==> Tidier to pull from properties file if name-mapping exists, else default them
// Why not: metrics.timer("Test_calc1") instead of metrics.timer("Test_calc1").time() ??
// What about quantiles?  https://github.com/prometheus/client_java#summary
// More on Labels?  https://github.com/prometheus/client_java#labels
// [rejected] Genericise Summary and Histogram <T>
// Support Labels?? https://github.com/prometheus/client_java#labels
// [rejected] Support Meters?? http://metrics.dropwizard.io/3.1.0/getting-started/

public class PrometheusMetricsTest {
    private PrometheusMetrics metrics;
    private CollectorRegistry registry;

    @Before
    public void setUp() throws IOException {
        TestableTimeProvider.install();

        registry = new CollectorRegistry();
        metrics = new PrometheusMetrics();
        metrics.setCollectorRegistry(registry);

        final Properties props = new Properties();
        try (Reader r = Files.newReader( new File("src/test/resources/app.properties"), Charsets.UTF_8)) {
            props.load(r);
        }

        metrics.setDescriptionMappings(props);
    }

    @Test
    public void testDropwizardTimerCompatibility() {
        try (Context timer = metrics.timer("Test.timer#a").time()) {
            System.out.println("Hi");
        }

        assertThat(samplesString(registry)).isEqualTo("[Name: Test_timer_a Type: SUMMARY Help: Test.timer#a Samples: [Name: Test_timer_a_count LabelNames: [] labelValues: [] Value: 1.0, Name: Test_timer_a_sum LabelNames: [] labelValues: [] Value: 1.979E-6]]");
        assertThat(registry.getSampleValue("Test_timer_a_sum") * 1E+9).isEqualByComparingTo(1979d);
    }

    @Test
    public void testDropwizardHistogramCompatibility() {
        metrics.histogram("response-sizes").update(34535);
        assertThat(samplesString(registry)).isEqualTo("[Name: response_sizes Type: HISTOGRAM Help: Response Sizes なお知らせ (bytes) Samples: [Name: response_sizes_bucket LabelNames: [le] labelValues: [0.005] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.01] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.025] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.05] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.075] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.1] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.25] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.5] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [0.75] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [1.0] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [2.5] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [5.0] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [7.5] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [10.0] Value: 0.0, Name: response_sizes_bucket LabelNames: [le] labelValues: [+Inf] Value: 1.0, Name: response_sizes_count LabelNames: [] labelValues: [] Value: 1.0, Name: response_sizes_sum LabelNames: [] labelValues: [] Value: 34535.0]]");
        assertThat(registry.getSampleValue("response_sizes_sum")).isEqualByComparingTo(34535d);
    }

    private String getTimedValueDemonstratingFriendlyTimingSyntax() {
        try (Context timer = metrics.histogram("Test_calc1").time()) {
            return "Hi";
        }
    }

    @Test
    public void testHistograms() {
        getTimedValueDemonstratingFriendlyTimingSyntax();

        assertThat(samplesString(registry)).isEqualTo("[Name: Test_calc1 Type: HISTOGRAM Help: Test_calc1 Samples: [Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.005] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.01] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.025] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.05] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.075] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.1] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.25] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.5] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [0.75] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [1.0] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [2.5] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [5.0] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [7.5] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [10.0] Value: 1.0, Name: Test_calc1_bucket LabelNames: [le] labelValues: [+Inf] Value: 1.0, Name: Test_calc1_count LabelNames: [] labelValues: [] Value: 1.0, Name: Test_calc1_sum LabelNames: [] labelValues: [] Value: 1.979E-6]]");
        assertThat(registry.getSampleValue("Test_calc1_sum") * 1E+9).isEqualByComparingTo(1979d);
    }

    @Test
    public void testHistogramWithExplicitDesc() {
        try (Context timer = metrics.histogram("MyName", "MyDesc").time()) {
            // Something
        }

        assertThat(samplesString(registry)).isEqualTo("[Name: MyName Type: HISTOGRAM Help: MyDesc Samples: [Name: MyName_bucket LabelNames: [le] labelValues: [0.005] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.01] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.025] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.05] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.075] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.1] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.25] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.5] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [0.75] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [1.0] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [2.5] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [5.0] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [7.5] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [10.0] Value: 1.0, Name: MyName_bucket LabelNames: [le] labelValues: [+Inf] Value: 1.0, Name: MyName_count LabelNames: [] labelValues: [] Value: 1.0, Name: MyName_sum LabelNames: [] labelValues: [] Value: 1.979E-6]]");
        assertThat(registry.getSampleValue("MyName_sum") * 1E+9).isEqualByComparingTo(1979d);
    }

    @Test
    public void testSummaryTimers() {
        try (Context timer = metrics.summary("Test_calc1").time()) {
            System.out.println("First");
        }

        try (Context timer = metrics.summary("Test_calc1").time()) {
            System.out.println("Second");
        }

        assertThat(samplesString(registry)).isEqualTo("[Name: Test_calc1 Type: SUMMARY Help: Test_calc1 Samples: [Name: Test_calc1_count LabelNames: [] labelValues: [] Value: 2.0, Name: Test_calc1_sum LabelNames: [] labelValues: [] Value: 3.958E-6]]");
        // assertThat(registry.getSampleValue("Test_calc1")).isGreaterThan(9999);
    }

    @Test
    public void testSummaryObservations() {
        metrics.summary("Vals").observe(1212.213412).observe(3434.34234).observe(3.1415926535875);

        assertThat(samplesString(registry)).isEqualTo("[Name: Vals Type: SUMMARY Help: Vals Samples: [Name: Vals_count LabelNames: [] labelValues: [] Value: 3.0, Name: Vals_sum LabelNames: [] labelValues: [] Value: 4649.697344653588]]");
    }

    @Test
    public void testCounter() {
        final double expected = System.nanoTime();

        metrics.counter("counter_1", "My first counter").inc(expected);
        assertThat(registry.getSampleValue("counter_1")).isEqualTo(expected);

        assertThat(samplesString(registry)).startsWith("[Name: counter_1 Type: COUNTER Help: My first counter Samples:");

        metrics.counter("counter_1").inc();
        assertThat(registry.getSampleValue("counter_1")).isEqualTo(expected + 1);
    }

    @Test
    public void testGauge() {
        final double expected = System.nanoTime();
        assertThat(registry.getSampleValue("g_1")).isNull();

        metrics.gauge("g_1").inc(expected);
        assertThat(registry.getSampleValue("g_1")).isEqualTo(expected);

        metrics.gauge("g_1").inc();
        assertThat(registry.getSampleValue("g_1")).isEqualTo(expected + 1);

        metrics.gauge("g_1").dec();
        assertThat(registry.getSampleValue("g_1")).isEqualTo(expected);

        metrics.gauge("g_1").dec(1981);
        assertThat(registry.getSampleValue("g_1")).isEqualTo(expected - 1981);
    }

    private static String samplesString(CollectorRegistry registry) {
        return Collections.list( registry.metricFamilySamples() ).toString();
    }
}