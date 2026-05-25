package com.akamai.miniwsa.stats.dto;

import com.akamai.miniwsa.domain.RuleCategory;

public record CategoryRow(RuleCategory category, long count, double avgThreatScore) {}
