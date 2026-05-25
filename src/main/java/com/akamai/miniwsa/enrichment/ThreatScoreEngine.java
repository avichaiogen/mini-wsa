package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Computes an integer threat score (0–100) for an incoming security event.
 *
 * Scoring components:
 *   - rule.severity : CRITICAL=40, HIGH=30, MEDIUM=20, LOW=10
 *   - action        : DENY=+20, ALERT=+10, MONITOR=+0
 *   - path          : contains "/admin" or "/login" → +15
 *   - priorCount    : ≥5 prior events from same clientIp in last 10 min → +15
 *
 * @param priorCount number of events already stored from the same clientIp in the last 10 minutes,
 *                   queried by IngestionService before this call. Keeping I/O out of this class
 *                   makes it a pure function — no mocking needed in unit tests.
 *
 * Final score is capped at MAX_SCORE (100).
 */
@Component
public class ThreatScoreEngine {

    private static final Logger log = LoggerFactory.getLogger(ThreatScoreEngine.class);

    private static final int MAX_SCORE = 100;

    public int compute(EventRequest req, long priorCount) {
        int score = 0;

        // Severity component
        score += switch (req.getRule().getSeverity()) {
            case CRITICAL -> 40;
            case HIGH     -> 30;
            case MEDIUM   -> 20;
            case LOW      -> 10;
        };

        // Action component
        score += switch (req.getAction()) {
            case DENY    -> 20;
            case ALERT   -> 10;
            case MONITOR -> 0;
        };

        // Sensitive path bonus
        if (req.getPath().contains("/admin") || req.getPath().contains("/login")) {
            score += 15;
        }

        // Repeat-offender bonus: ≥5 prior events from same IP in last 10 min
        if (priorCount >= 5) {
            score += 15;
        }

        int finalScore = Math.min(score, MAX_SCORE);
        log.debug("Computed threatScore={} (priorCount={})", finalScore, priorCount);
        return finalScore;
    }
}
