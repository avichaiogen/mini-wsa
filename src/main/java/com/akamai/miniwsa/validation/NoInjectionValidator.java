package com.akamai.miniwsa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NoInjectionValidator implements ConstraintValidator<NoInjection, String> {

    private static final Pattern DANGEROUS = Pattern.compile(
        "\\x00"                 // null byte
        + "|</?script"          // XSS script tags
        + "|javascript:"        // XSS href/src
        + "|vbscript:"          // XSS (legacy IE)
        + "|on[a-z]+=\\s*[\"']?" // XSS event handlers: onclick=, onload=, …
        + "|\\.\\./|\\.\\.\\\\" // path traversal: ../ or ..\
        + "|union\\s+select"    // SQL injection
        + "|drop\\s+table"      // SQL injection
        + "|insert\\s+into"     // SQL injection
        + "|delete\\s+from"     // SQL injection
        + "|'\\s*or\\s*'"       // SQL injection: ' or '
        + "|'\\s*and\\s*'"      // SQL injection: ' and '
        + "|--\\s",             // SQL line comment
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank handles null
        return !DANGEROUS.matcher(value).find();
    }
}
