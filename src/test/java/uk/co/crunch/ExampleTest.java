package uk.co.crunch;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.TestableTimeProvider;
import org.junit.Before;
import org.junit.Test;
import uk.co.crunch.api.PrometheusMetrics;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.crunch.TestUtils.samplesString;

public class ExampleTest {
    private CollectorRegistry registry;

    @Before
    public void setUp() throws IOException {
        TestableTimeProvider.install();
        registry = new CollectorRegistry();
    }

    @Test
    public void testExample() {
        final Example ex = new Example( new PrometheusMetrics(registry, "Example") );

        assertThat(registry.getSampleValue("example_sessions_open")).isNull();
        assertThat(registry.getSampleValue("example_errors", new String[]{"error_type"}, new String[]{"generic"})).isNull();

        ex.handleLogin();
        ex.onUserLogin("");
        assertThat(registry.getSampleValue("example_sessions_open")).isEqualTo(1);

        ex.onUserLogout("");
        assertThat(registry.getSampleValue("example_sessions_open")).isEqualTo(0);

        ex.onError( new Throwable() );
        assertThat(registry.getSampleValue("example_errors", new String[]{"error_type"}, new String[]{"generic"})).isEqualTo(1.0d);

        final String contents = samplesString(registry);
        assertThat(contents).contains("Name: example_errors Type: COUNTER Help: Generic errors Samples: [Name: example_errors LabelNames: [error_type] labelValues: [generic] Value: 1.0]");
        assertThat(contents).contains("Name: example_sessions_handlelogin Type: SUMMARY Help: Login times Samples: [Name: example_sessions_handlelogin_count LabelNames: [] labelValues: [] Value: 1.0, Name: example_sessions_handlelogin_sum LabelNames: [] labelValues: [] Value: 1.979E-6]");
        assertThat(contents).contains("Name: example_sessions_open Type: GAUGE Help: example_sessions_open Samples: [Name: example_sessions_open LabelNames: [] labelValues: [] Value: 0.0]");
    }
}
