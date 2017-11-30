package uk.co.crunch.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)  // Only RUNTIME for testability
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AlertRules {
    String groupName() default "";
    AlertRule[] value();
}
