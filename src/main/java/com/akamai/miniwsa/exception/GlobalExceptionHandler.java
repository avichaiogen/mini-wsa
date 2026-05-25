package com.akamai.miniwsa.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralised exception handler — converts exceptions to safe HTTP responses.
 *
 * OWASP rule: responses NEVER expose stack traces, DB error messages, or internal class names.
 * All details are either field-level validation messages (safe) or a generic string.
 * Logging: 400s at WARN, 500s at ERROR with full stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles @Valid failures on request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Validation failed: {}", details);
        return Map.of("error", "Validation failed", "details", details);
    }

    /**
     * Handles constraint violations on @RequestParam / @PathVariable (Spring Boot 4 / Spring 6.1+).
     * MethodArgumentNotValidException only fires for @RequestBody; method-level params throw this.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        log.warn("Method validation failed: {}", details);
        return Map.of("error", "Validation failed", "details", details);
    }

    /**
     * Handles @RequestParam type mismatches (e.g. configId=abc when Long expected, unparseable date).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {}", ex.getMessage());
        return Map.of("error", "Invalid parameter format");
    }

    /**
     * Handles malformed JSON and unknown enum values. Returns a generic message — not the
     * Jackson error text, which would leak field names and valid values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleParseError(HttpMessageNotReadableException ex) {
        log.warn("Request parse error: {}", ex.getMessage());
        return Map.of("error", "Invalid request format");
    }

    /**
     * Handles invalid domain objects (rule, action, geoLocation, event-level issues).
     * Includes the field name when available so the caller knows what to fix.
     */
    @ExceptionHandler({InvalidRuleException.class, InvalidActionException.class,
                       InvalidGeoLocationException.class, InvalidEventException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidInput(MiniWsaException ex) {
        log.warn("Invalid input: field={}, message={}", ex.getField(), ex.getSafeMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getSafeMessage());
        if (ex.getField() != null) body.put("field", ex.getField());
        return body;
    }

    /**
     * Handles resource-not-found lookups — returns 404 with a safe message.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getSafeMessage());
        return Map.of("error", ex.getSafeMessage());
    }

    /**
     * Handles batch persistence failures — server fault, never exposes DB internals.
     */
    @ExceptionHandler(IngestionBatchException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleIngestionBatch(IngestionBatchException ex) {
        log.error("Ingestion batch failed", ex);
        return Map.of("error", "Internal server error");
    }

    /**
     * Handles requests to URLs that have no matching controller (404).
     * Requires spring.mvc.throw-exception-if-no-handler-found=true and
     * spring.web.resources.add-mappings=false in application.yml.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNoHandlerFound(Exception ex) {
        log.warn("No handler found: {}", ex.getMessage());
        return Map.of("error", "Resource not found");
    }

    /**
     * Handles requests that use the wrong HTTP method on a known endpoint (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return Map.of("error", "HTTP method not allowed");
    }

    /**
     * Handles requests with a missing or unsupported Content-Type header (415).
     * Common cause: POST to /ingest without Content-Type: application/json.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Map<String, Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return Map.of("error", "Unsupported media type");
    }

    /**
     * Catch-all for any unexpected exception. Logs full stack trace internally only.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return Map.of("error", "Internal server error");
    }
}
