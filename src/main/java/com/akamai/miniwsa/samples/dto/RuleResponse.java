package com.akamai.miniwsa.samples.dto;

import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.Severity;

public record RuleResponse(
        String id,
        String name,
        String message,
        Severity severity,
        RuleCategory category
) {}
