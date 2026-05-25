package com.akamai.miniwsa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HttpMethodValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidHttpMethod {
    String message() default "must be a valid HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
