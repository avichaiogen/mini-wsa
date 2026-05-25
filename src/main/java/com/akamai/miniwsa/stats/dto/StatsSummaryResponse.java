package com.akamai.miniwsa.stats.dto;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatsSummaryResponse(
        Long configId,
        TimeRange timeRange,
        long totalEvents,
        Map<RuleCategory, CategoryStats> byCategory,
        Map<Action, Long> byAction,
        List<AttackerStats> topAttackers,
        List<PathStats> topTargetedPaths
) {}
