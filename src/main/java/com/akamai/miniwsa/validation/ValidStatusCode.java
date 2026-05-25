package com.akamai.miniwsa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StatusCodeValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStatusCode {
    String message() default "must be a valid HTTP status code (100-599)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
