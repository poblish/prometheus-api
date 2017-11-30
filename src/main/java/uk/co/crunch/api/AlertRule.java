package uk.co.crunch.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)  // Only RUNTIME for testability
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AlertRule {
    String name();
    String duration();

    String[] metricNames();
    String rule();

    // Labels...
    Severity severity() default Severity.PAGE;
    Label[] labels() default {};

    // Annotations...
    String summary();
    String description();
    String confluenceLink();
    Annotation[] annotations() default {};

    enum Severity {
        PAGE, WARNING
    }

    @interface Label {
        String name();
        String value();
    }

    @interface Annotation {
        String name();
        String value();
    }
}
