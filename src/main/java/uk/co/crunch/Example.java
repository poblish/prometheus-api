package uk.co.crunch;

import uk.co.crunch.api.PrometheusMetrics;
import uk.co.crunch.api.PrometheusMetrics.Context;

import static com.google.common.base.Preconditions.checkNotNull;

public class Example {

    private final PrometheusMetrics metrics;

    // @Inject
    public Example(final PrometheusMetrics metrics) {
        this.metrics = checkNotNull(metrics);
    }

    public void onUserLogin(Object event) {
        metrics.gauge("Sessions.open").inc();
    }

    public void onUserLogout(Object event) {
        metrics.gauge("Sessions.open").dec();
    }

    public void onError(Object event) {
        metrics.error("generic", "Generic errors");
    }

    public String handleLogin() {
        try (Context timer = metrics.timer("Sessions.handleLogin", "Login times").time()) {
            return "Login handled!";
        }
    }
}
