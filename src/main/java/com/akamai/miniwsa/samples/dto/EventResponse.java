package com.akamai.miniwsa.samples.dto;

import com.akamai.miniwsa.domain.Action;

import java.time.Instant;

public record EventResponse(
        String eventId,
        Instant timestamp,
        Instant receivedAt,
        Long configId,
        String policyId,
        String clientIp,
        String hostname,
        String path,
        String method,
        Integer statusCode,
        String userAgent,
        RuleResponse rule,
        Action action,
        GeoLocationResponse geoLocation,
        Integer requestSize,
        Integer responseSize,
        String attackType,
        Integer threatScore
) {}
