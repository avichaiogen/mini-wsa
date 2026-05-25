package com.akamai.miniwsa.ingestion;

import com.akamai.miniwsa.domain.GeoLocation;
import com.akamai.miniwsa.domain.Rule;
import com.akamai.miniwsa.domain.SecurityEvent;
import com.akamai.miniwsa.enrichment.AttackClassifier;
import com.akamai.miniwsa.enrichment.ThreatScoreEngine;
import com.akamai.miniwsa.ingestion.dto.EventRequest;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the ingestion pipeline for a batch of security events:
 *   1. Map each EventRequest DTO to a SecurityEvent entity
 *   2. Set server-side receivedAt (never trusted from client — A3)
 *   3. Enrich with attackType (AttackClassifier) and threatScore (ThreatScoreEngine)
 *   4. Persist all events in a single transaction
 *
 * @Transactional enforces A2 (all-or-nothing batch): if any save fails, the entire
 * batch is rolled back. Bean Validation at the controller layer already ensures no
 * invalid event reaches this service, but the transaction provides a DB-level safety net.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IngestionService {

    private final SecurityEventRepository repository;
    private final AttackClassifier attackClassifier;
    private final ThreatScoreEngine threatScoreEngine;

    public void ingest(List<EventRequest> requests) {
        List<SecurityEvent> events = requests.stream()
                .map(this::toEntity)
                .toList();
        repository.saveAll(events);
    }

    /** Maps a validated DTO to a fully-enriched entity ready for persistence. */
    private SecurityEvent toEntity(EventRequest req) {
        SecurityEvent event = new SecurityEvent();

        event.setEventId(req.getEventId());
        event.setTimestamp(req.getTimestamp());
        event.setConfigId(req.getConfigId());
        event.setPolicyId(req.getPolicyId());
        event.setClientIp(req.getClientIp());
        event.setHostname(req.getHostname());
        event.setPath(req.getPath());
        event.setMethod(req.getMethod());
        event.setStatusCode(req.getStatusCode());
        event.setUserAgent(req.getUserAgent());

        event.setRule(new Rule(
                req.getRule().getId(),
                req.getRule().getName(),
                req.getRule().getMessage(),
                req.getRule().getSeverity(),
                req.getRule().getCategory()
        ));

        event.setAction(req.getAction());

        event.setGeoLocation(new GeoLocation(
                req.getGeoLocation().getCountry(),
                req.getGeoLocation().getCity()
        ));

        event.setRequestSize(req.getRequestSize());
        event.setResponseSize(req.getResponseSize());

        // Server-side timestamp — never sourced from client (A3)
        event.setReceivedAt(Instant.now());

        // Enrichment
        event.setAttackType(attackClassifier.classify(req.getRule().getCategory()));
        event.setThreatScore(threatScoreEngine.compute(req));

        return event;
    }
}
