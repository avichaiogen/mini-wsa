package com.akamai.miniwsa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoInjectionValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoInjection {
    String message() default "contains invalid content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
