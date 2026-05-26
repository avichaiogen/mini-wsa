package com.akamai.miniwsa.samples;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import com.akamai.miniwsa.samples.dto.EventResponse;
import com.akamai.miniwsa.samples.dto.GeoLocationResponse;
import com.akamai.miniwsa.samples.dto.RuleResponse;
import com.akamai.miniwsa.samples.dto.SamplesResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SamplesService {

    private static final Logger log = LoggerFactory.getLogger(SamplesService.class);

    static final int MAX_LIMIT = 100;

    private final SecurityEventRepository repository;

    public SamplesResponse getSamples(Long configId, Instant from, Instant to,
                                      RuleCategory category, Action action,
                                      int limit, int offset) {
        int effectiveLimit  = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int effectiveOffset = Math.max(offset, 0);
        var pageable = PageRequest.of(effectiveOffset / effectiveLimit, effectiveLimit);

        log.debug("Samples query: configId={}, from={}, to={}, category={}, action={}, limit={}, offset={}",
                configId, from, to, category, action, effectiveLimit, effectiveOffset);

        long total = repository.countSamples(configId, from, to, category, action);
        List<EventResponse> events = repository
                .findSamples(configId, from, to, category, action, pageable)
                .stream()
                .map(e -> new EventResponse(
                        e.getEventId(),
                        e.getTimestamp(),
                        e.getReceivedAt(),
                        e.getConfigId(),
                        e.getPolicyId(),
                        e.getClientIp(),
                        e.getHostname(),
                        e.getPath(),
                        e.getMethod(),
                        e.getStatusCode(),
                        e.getUserAgent(),
                        new RuleResponse(
                                e.getRule().getId(),
                                e.getRule().getName(),
                                e.getRule().getMessage(),
                                e.getRule().getSeverity(),
                                e.getRule().getCategory()),
                        e.getAction(),
                        new GeoLocationResponse(
                                e.getGeoLocation().getCountry(),
                                e.getGeoLocation().getCity()),
                        e.getRequestSize(),
                        e.getResponseSize(),
                        e.getAttackType(),
                        e.getThreatScore()))
                .toList();

        log.debug("Samples result: total={}, returned={}, page offset={}", total, events.size(), effectiveOffset);
        return new SamplesResponse(total, effectiveLimit, effectiveOffset, events);
    }
}
