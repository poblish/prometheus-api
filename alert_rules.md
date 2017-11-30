### Aim:

* Reduce distance between/separation of metrics and alerting rules for Java services:
  * So developers understand these are linked development activities
  * Break association with 'infrastructure configuration'
* Allow automated validation of alerting rules at compile-time
* Allow enforcement of alerting rules at compile-time, e.g. `[WARN] 5 Prometheus metrics defined, but only 2 covered by alerting rules` / `no alerting rules defined`
* `@AlertRule` API provides guidance towards correctly completing an `ALERT` rule, with defaults and auto-completion only where uncontentious. (It would not be wise or helpful to try to create a rule builder.)

### Implementation:

* Plugin can already identify metric usage, and will be able to read the `@AlertRule` annotations.
* Providing the metric names can be parsed from the `rule` queries, it will be easy to compare the two sets.
* Plugin will be able to validate some parts of the `@AlertRule` and auto/complete some parts, e.g. expand Confluence URLs, prepend 'prefix' to rule names
* Plugin will aggregate all rules and export one or more rule files (either 1.x or 2.x YAML) file into the build.
* Rule files exposed as artifacts, which need to get aggregated and pushed to Prometheus.

### Discussion points:

* Parsing the metric names from the `rule` queries would be ugly. A more parseable alternative to:

        @AlertRule(name = "InsuranceTrafficIncrease",
        rule = "IF avg_over_time(insurance_requests_per_second[1m]) ...",

  is:

        @AlertRule(name = "InsuranceTrafficIncrease",
        metrics={"insurance_requests_per_second"},
        rule = "IF avg_over_time($1[1m]) ...",

  That might be annoying for the developer who would prefer to just paste a tested rule, OTOH this level of abstraction allows the developer to use the "internal" metric name throughout their code, rather than the fully normalised name that Prometheus needs:

          @AlertRule(name = "TrafficIncrease",
          metrics={"requests per second"},
          rule = "IF avg_over_time($1[1m]) ...",

*

### Possible improvements:

* `summary`, `description`, `confluenceLink` would pollute the code less if they pointed to external property values instead of inline text. However, that would complicate the API, and make it harder for the Plugin to validate the actual values.
