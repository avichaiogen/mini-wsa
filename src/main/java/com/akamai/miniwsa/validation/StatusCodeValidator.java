package com.akamai.miniwsa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StatusCodeValidator implements ConstraintValidator<ValidStatusCode, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull handles null separately
        return value >= 100 && value <= 599;
    }
}
