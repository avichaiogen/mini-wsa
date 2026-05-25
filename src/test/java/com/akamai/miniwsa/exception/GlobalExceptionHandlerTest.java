package com.akamai.miniwsa.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // --- handleInvalidInput ---

    @Test
    void handleInvalidInput_invalidRule_returnsFieldAndMessage() {
        InvalidRuleException ex = new InvalidRuleException("rule", "Rule is required");

        Map<String, Object> body = handler.handleInvalidInput(ex);

        assertThat(body).containsEntry("error", "Rule is required")
                        .containsEntry("field", "rule");
    }

    @Test
    void handleInvalidInput_invalidAction_returnsFieldAndMessage() {
        InvalidActionException ex = new InvalidActionException("action", "Action is required");

        Map<String, Object> body = handler.handleInvalidInput(ex);

        assertThat(body).containsEntry("error", "Action is required")
                        .containsEntry("field", "action");
    }

    @Test
    void handleInvalidInput_invalidGeoLocation_returnsFieldAndMessage() {
        InvalidGeoLocationException ex = new InvalidGeoLocationException("geoLocation", "GeoLocation is required");

        Map<String, Object> body = handler.handleInvalidInput(ex);

        assertThat(body).containsEntry("error", "GeoLocation is required")
                        .containsEntry("field", "geoLocation");
    }

    @Test
    void handleInvalidInput_invalidEvent_nullField_noFieldKeyInResponse() {
        InvalidEventException ex = new InvalidEventException(null, "Event data is invalid");

        Map<String, Object> body = handler.handleInvalidInput(ex);

        assertThat(body).containsEntry("error", "Event data is invalid")
                        .doesNotContainKey("field");
    }

    // --- handleNotFound ---

    @Test
    void handleNotFound_returnsMessageWithoutFieldKey() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Event not found");

        Map<String, Object> body = handler.handleNotFound(ex);

        assertThat(body).containsEntry("error", "Event not found")
                        .doesNotContainKey("field");
    }

    // --- handleIngestionBatch ---

    @Test
    void handleIngestionBatch_returnsGenericMessage_noCause() {
        IngestionBatchException ex = new IngestionBatchException(
                "Failed to persist event batch", new DataIntegrityViolationException("DB error"));

        Map<String, Object> body = handler.handleIngestionBatch(ex);

        assertThat(body).containsEntry("error", "Internal server error")
                        .doesNotContainKey("field")
                        .doesNotContainKey("cause");
    }

    @Test
    void handleIngestionBatch_doesNotExposeDbMessage() {
        IngestionBatchException ex = new IngestionBatchException(
                "Failed to persist event batch", new DataIntegrityViolationException("secret db detail"));

        Map<String, Object> body = handler.handleIngestionBatch(ex);

        assertThat(body.values()).doesNotContain("secret db detail");
    }

    // --- handleNoHandlerFound ---

    @Test
    void handleNoHandlerFound_noHandlerFoundException_returns404Body() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/v1/unknown", null);

        Map<String, Object> body = handler.handleNoHandlerFound(ex);

        assertThat(body).containsEntry("error", "Resource not found")
                        .doesNotContainKey("field");
    }

    @Test
    void handleNoHandlerFound_noResourceFoundException_returns404Body() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/static/missing.css", "missing.css");

        Map<String, Object> body = handler.handleNoHandlerFound(ex);

        assertThat(body).containsEntry("error", "Resource not found");
    }

    // --- handleMethodNotAllowed ---

    @Test
    void handleMethodNotAllowed_returns405Body() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

        Map<String, Object> body = handler.handleMethodNotAllowed(ex);

        assertThat(body).containsEntry("error", "HTTP method not allowed");
    }

    // --- handleUnsupportedMediaType ---

    @Test
    void handleUnsupportedMediaType_returns415Body() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("text/plain is not supported");

        Map<String, Object> body = handler.handleUnsupportedMediaType(ex);

        assertThat(body).containsEntry("error", "Unsupported media type");
    }
}
