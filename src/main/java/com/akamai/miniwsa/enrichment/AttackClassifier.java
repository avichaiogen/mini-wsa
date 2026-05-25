package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.domain.RuleCategory;
import org.springframework.stereotype.Component;

/**
 * Maps a raw rule.category from the DLR to a human-readable attackType string.
 * The mapping is 1-to-1 and exhaustive — every RuleCategory has a defined label.
 */
@Component
public class AttackClassifier {

    public String classify(RuleCategory category) {
        return switch (category) {
            case INJECTION          -> "SQL/Command Injection";
            case XSS                -> "Cross-Site Scripting";
            case PROTOCOL_VIOLATION -> "Protocol Anomaly";
            case DATA_LEAKAGE       -> "Data Exfiltration";
            case BOT                -> "Bot Activity";
            case DOS                -> "Denial of Service";
            case RATE_LIMIT         -> "Rate Limiting";
        };
    }
}
