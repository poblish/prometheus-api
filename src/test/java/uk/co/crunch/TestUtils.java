package uk.co.crunch;

import io.prometheus.client.CollectorRegistry;

import java.util.Collections;

public class TestUtils {

    public static String samplesString(CollectorRegistry registry) {
        return Collections.list( registry.metricFamilySamples() ).toString();
    }
}
