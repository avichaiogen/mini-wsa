package com.akamai.miniwsa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IpAddressValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIpAddress {
    String message() default "must be a valid IPv4 or IPv6 address";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
