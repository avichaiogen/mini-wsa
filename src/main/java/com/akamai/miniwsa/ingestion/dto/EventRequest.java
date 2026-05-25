package com.akamai.miniwsa.ingestion.dto;

import com.akamai.miniwsa.domain.Action;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Input DTO for a single security event (DLR).
 *
 * Does NOT include: id, attackType, threatScore, receivedAt — all set server-side.
 *
 * Jackson deserialises Instant from ISO-8601 strings (e.g. "2026-05-20T14:32:10Z").
 * Unknown enum values for action, rule.severity, or rule.category cause a 400 via
 * GlobalExceptionHandler (Jackson throws HttpMessageNotReadableException).
 *
 * @Valid on rule and geoLocation cascades Bean Validation into the nested DTOs.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventRequest {

    @NotBlank @Size(max = 100)
    private String eventId;

    // Instant maps from/to ISO-8601 string; Jackson's JavaTimeModule handles the conversion.
    @NotNull
    private Instant timestamp;

    @NotNull
    private Long configId;

    @NotBlank @Size(max = 100)
    private String policyId;

    @NotBlank @Size(max = 45)
    private String clientIp;

    @NotBlank @Size(max = 255)
    private String hostname;

    @NotBlank @Size(max = 2048)
    private String path;

    @NotBlank @Size(max = 10)
    private String method;

    @NotNull
    private Integer statusCode;

    @NotBlank @Size(max = 512)
    private String userAgent;

    // @Valid cascades validation into RuleRequest fields
    @Valid @NotNull
    private RuleRequest rule;

    @NotNull
    private Action action;

    // @Valid cascades validation into GeoLocationRequest fields
    @Valid @NotNull
    private GeoLocationRequest geoLocation;

    @NotNull @Min(0)
    private Integer requestSize;

    @NotNull @Min(0)
    private Integer responseSize;
}
