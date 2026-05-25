package com.akamai.miniwsa.stats.dto;

public record AttackerStats(String clientIp, long count, double avgThreatScore) {}
