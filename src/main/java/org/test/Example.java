package org.test;

import org.test.PrometheusMetrics.Context;

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

    public String handleLogin() {
        try (Context timer = metrics.timer("Sessions.handleLogin").time()) {
            return "Login handled!";
        }
    }
}
