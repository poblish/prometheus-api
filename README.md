# Demonstration of an alternative Prometheus API

* API geared aimed at application developers rather than Prometheus experts.
* Name-based metrics; no need to create instances or handle registration.
* Simpler, cleaner timer / histogram syntax, via resource/try
* Aim is to simplify Prometheus adoption, reduce excessive code intrusion.
* Lexical compatibility with Codahale/Dropwizard API for Barclays/Insurance, simplifying complete migration.
* Prometheus API can be used directly for more advanced cases (hopefully not necessary)

## Example:

```java
private final PrometheusMetrics metrics;

public void onUserLogin(Object event) {
    metrics.gauge("Sessions.open").inc();
    metrics.counter("Sessions.total").inc();
}

public String handleLogin() {
    try (Context timer = metrics.summary("Sessions.handleLogin").time()) {
        return "Login handled!";
    }
}
```

## Features:

#### Application prefix:

```java
@Value("${spring.application.name}")
private String appName;

@Bean
public PrometheusMetrics prometheusMetrics(CollectorRegistry collector) {
    return new PrometheusMetrics(collector, appName);
}
```

#### Optional descriptions:

```java
metrics.timer("transaction");
metrics.timer("transaction", "Transaction time");
```

#### Error counts implemented via labels:

```java
metrics.error("salesforce");

assertThat(registry.getSampleValue("myapp_errors", new String[]{"error_type"}, new String[]{"salesforce"})).isEqualTo(1.0d);

// ...

metrics.error("stripe_transaction");

assertThat(registry.getSampleValue("myapp_errors", new String[]{"error_type"}, new String[]{"stripe_transaction"})).isEqualTo(1.0d);
```

#### All names sanitised to ensure no invalid characters