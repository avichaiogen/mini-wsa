package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
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

    private static final int MAX_SCORE = 100;

    private static final int SCORE_SEVERITY_CRITICAL  = 40;
    private static final int SCORE_SEVERITY_HIGH      = 30;
    private static final int SCORE_SEVERITY_MEDIUM    = 20;
    private static final int SCORE_SEVERITY_LOW       = 10;

    private static final int SCORE_ACTION_DENY        = 20;
    private static final int SCORE_ACTION_ALERT       = 10;

    private static final int BONUS_SENSITIVE_PATH     = 15;
    private static final int BONUS_REPEAT_OFFENDER    = 15;
    private static final int REPEAT_OFFENDER_THRESHOLD = 5;

    public static final long REPEAT_OFFENDER_WINDOW_MINUTES = 10;

    public int compute(EventRequest req, long priorCount) {
        int score = 0;

        // Severity component
        score += switch (req.getRule().getSeverity()) {
            case CRITICAL -> SCORE_SEVERITY_CRITICAL;
            case HIGH     -> SCORE_SEVERITY_HIGH;
            case MEDIUM   -> SCORE_SEVERITY_MEDIUM;
            case LOW      -> SCORE_SEVERITY_LOW;
        };

        // Action component
        score += switch (req.getAction()) {
            case DENY    -> SCORE_ACTION_DENY;
            case ALERT   -> SCORE_ACTION_ALERT;
            case MONITOR -> 0;
        };

        // Sensitive path bonus
        if (req.getPath().contains("/admin") || req.getPath().contains("/login")) {
            score += BONUS_SENSITIVE_PATH;
        }

        // Repeat-offender bonus: ≥ threshold prior events from same IP in last window
        if (priorCount >= REPEAT_OFFENDER_THRESHOLD) {
            score += BONUS_REPEAT_OFFENDER;
        }

        return Math.min(score, MAX_SCORE);
    }
}
