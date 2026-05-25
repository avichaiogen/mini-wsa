package com.akamai.miniwsa.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/**
 * Centralised exception handler — converts exceptions to safe HTTP responses.
 *
 * OWASP rule: responses NEVER expose stack traces, DB error messages, or internal class names.
 * All details are either field-level validation messages (safe) or a generic string (for parse/unknown errors).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles @Valid failures on request bodies.
     * Returns the specific field(s) and constraint message(s) so the caller knows what to fix.
     * Example: {"error": "Validation failed", "details": ["eventId: must not be blank"]}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return Map.of("error", "Validation failed", "details", details);
    }

    /**
     * Handles @RequestParam type mismatches (e.g. configId=abc when Long is expected, or
     * an unparseable ISO-8601 date). Returns 400 instead of the default 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.debug("Type mismatch: {}", ex.getMessage());
        return Map.of("error", "Invalid parameter format");
    }

    /**
     * Handles malformed JSON and unknown enum values (e.g. "severity": "EXTREME").
     * Jackson wraps both as HttpMessageNotReadableException.
     * We return a generic message — not the Jackson error text, which leaks field names and valid values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleParseError(HttpMessageNotReadableException ex) {
        log.debug("Request parse error: {}", ex.getMessage());
        return Map.of("error", "Invalid request format");
    }

    /**
     * Catch-all for any unexpected exception.
     * Logs the full stack trace internally but returns only a generic message to the caller.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return Map.of("error", "Internal server error");
    }
}
