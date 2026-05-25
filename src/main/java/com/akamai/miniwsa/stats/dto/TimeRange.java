package com.akamai.miniwsa.stats.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimeRange(Instant from, Instant to) {}
