package com.socialpublish.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = PlatformListValidator.class)
@Documented
public @interface PlatformList {

    String message() default "One or more platform values are invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
