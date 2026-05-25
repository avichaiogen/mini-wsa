package com.akamai.miniwsa.samples.dto;

import java.util.List;

public record SamplesResponse(long total, int limit, int offset, List<EventResponse> events) {}
