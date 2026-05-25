package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.domain.RuleCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttackClassifierTest {

    private final AttackClassifier classifier = new AttackClassifier();

    @Test
    void classify_injection() {
        assertThat(classifier.classify(RuleCategory.INJECTION)).isEqualTo("SQL/Command Injection");
    }

    @Test
    void classify_xss() {
        assertThat(classifier.classify(RuleCategory.XSS)).isEqualTo("Cross-Site Scripting");
    }

    @Test
    void classify_protocolViolation() {
        assertThat(classifier.classify(RuleCategory.PROTOCOL_VIOLATION)).isEqualTo("Protocol Anomaly");
    }

    @Test
    void classify_dataLeakage() {
        assertThat(classifier.classify(RuleCategory.DATA_LEAKAGE)).isEqualTo("Data Exfiltration");
    }

    @Test
    void classify_bot() {
        assertThat(classifier.classify(RuleCategory.BOT)).isEqualTo("Bot Activity");
    }

    @Test
    void classify_dos() {
        assertThat(classifier.classify(RuleCategory.DOS)).isEqualTo("Denial of Service");
    }

    @Test
    void classify_rateLimit() {
        assertThat(classifier.classify(RuleCategory.RATE_LIMIT)).isEqualTo("Rate Limiting");
    }
}
