package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
import org.springframework.stereotype.Component;

/**
 * Computes an integer threat score (0–100) for an incoming security event.
 *
 * Scoring components (Phase 3):
 *   - rule.severity : CRITICAL=40, HIGH=30, MEDIUM=20, LOW=10
 *   - action        : DENY=+20, ALERT=+10, MONITOR=+0
 *   - path          : contains "/admin" or "/login" → +15
 *
 * Phase 4 will add:
 *   - Repeat-offender bonus: +15 if ≥5 prior events from the same clientIp in the last 10 min (A4)
 *
 * Final score is capped at MAX_SCORE (100).
 */
@Component
public class ThreatScoreEngine {

    private static final int MAX_SCORE = 100;

    public int compute(EventRequest req) {
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

        // Repeat-offender bonus (+15) — added in Phase 4

        return Math.min(score, MAX_SCORE);
    }
}
