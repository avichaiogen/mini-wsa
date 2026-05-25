package com.akamai.miniwsa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class HttpMethodValidator implements ConstraintValidator<ValidHttpMethod, String> {

    private static final Set<String> ALLOWED =
            Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank handles null/blank separately
        return ALLOWED.contains(value.toUpperCase());
    }
}
