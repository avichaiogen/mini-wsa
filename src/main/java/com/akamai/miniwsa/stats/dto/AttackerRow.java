package com.akamai.miniwsa.stats.dto;

public record AttackerRow(String clientIp, long count, double avgThreatScore) {}
