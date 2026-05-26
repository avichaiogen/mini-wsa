package com.akamai.miniwsa.stats;

import com.akamai.miniwsa.repository.SecurityEventRepository;
import com.akamai.miniwsa.stats.dto.AttackerStats;
import com.akamai.miniwsa.stats.dto.CategoryStats;
import com.akamai.miniwsa.stats.dto.PathStats;
import com.akamai.miniwsa.stats.dto.StatsSummaryResponse;
import com.akamai.miniwsa.stats.dto.TimeRange;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private static final int TOP_N = 10;

    private final SecurityEventRepository repository;

    public StatsSummaryResponse getSummary(Long configId, Instant from, Instant to) {
        var pageable = PageRequest.of(0, TOP_N);

        log.debug("Stats query: configId={}, from={}, to={}", configId, from, to);

        long total = repository.countFiltered(configId, from, to);

        var byCategory = repository.countByCategory(configId, from, to).stream()
                .collect(Collectors.toMap(
                        r -> r.category(),
                        r -> new CategoryStats(r.count(), r.avgThreatScore())));

        var byAction = repository.countByAction(configId, from, to).stream()
                .collect(Collectors.toMap(
                        r -> r.action(),
                        r -> r.count()));

        List<AttackerStats> topAttackers = repository.topAttackers(configId, from, to, pageable);

        List<PathStats> topPaths = repository.topTargetedPaths(configId, from, to, pageable);

        log.debug("Stats result: total={}, categories={}, topAttackers={}, topPaths={}",
                total, byCategory.size(), topAttackers.size(), topPaths.size());

        return new StatsSummaryResponse(
                configId,
                new TimeRange(from, to),
                total,
                byCategory,
                byAction,
                topAttackers,
                topPaths);
    }
}
