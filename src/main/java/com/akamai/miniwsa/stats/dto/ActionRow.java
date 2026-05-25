package com.akamai.miniwsa.stats.dto;

import com.akamai.miniwsa.domain.Action;

public record ActionRow(Action action, long count) {}
