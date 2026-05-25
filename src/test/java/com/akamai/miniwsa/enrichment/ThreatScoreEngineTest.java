package com.akamai.miniwsa.enrichment;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.ingestion.dto.EventRequest;
import com.akamai.miniwsa.ingestion.dto.GeoLocationRequest;
import com.akamai.miniwsa.ingestion.dto.RuleRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ThreatScoreEngineTest {

    private final ThreatScoreEngine engine = new ThreatScoreEngine();

    // --- Severity component ---

    @Test
    void compute_criticalSeverity() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/api/data"), 0)).isEqualTo(60);
    }

    @Test
    void compute_highSeverity() {
        assertThat(engine.compute(buildRequest(Severity.HIGH, Action.DENY, "/api/data"), 0)).isEqualTo(50);
    }

    @Test
    void compute_mediumSeverity() {
        assertThat(engine.compute(buildRequest(Severity.MEDIUM, Action.DENY, "/api/data"), 0)).isEqualTo(40);
    }

    @Test
    void compute_lowSeverity() {
        assertThat(engine.compute(buildRequest(Severity.LOW, Action.DENY, "/api/data"), 0)).isEqualTo(30);
    }

    // --- Action component ---

    @Test
    void compute_alertAction() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.ALERT, "/api/data"), 0)).isEqualTo(50);
    }

    @Test
    void compute_monitorAction() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.MONITOR, "/api/data"), 0)).isEqualTo(40);
    }

    // --- Path bonus ---

    @Test
    void compute_adminPathBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/admin/users"), 0)).isEqualTo(75);
    }

    @Test
    void compute_loginPathBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/login"), 0)).isEqualTo(75);
    }

    @Test
    void compute_normalPath_noBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/api/data"), 0)).isEqualTo(60);
    }

    // --- Repeat-offender bonus (A4) ---

    @Test
    void compute_repeatOffender_below5_noBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/api/data"), 4)).isEqualTo(60);
    }

    @Test
    void compute_repeatOffender_exactly5_addsBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/api/data"), 5)).isEqualTo(75);
    }

    @Test
    void compute_repeatOffender_above5_addsBonus() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/api/data"), 10)).isEqualTo(75);
    }

    // --- Stacking cap: max possible = CRITICAL(40) + DENY(20) + path(15) + repeat(15) = 90 ---

    @Test
    void compute_allBonuses_doesNotExceed90() {
        assertThat(engine.compute(buildRequest(Severity.CRITICAL, Action.DENY, "/admin"), 5)).isEqualTo(90);
    }

    private EventRequest buildRequest(Severity severity, Action action, String path) {
        RuleRequest rule = new RuleRequest(
                "950001", "TEST_RULE", "Test message", severity, RuleCategory.INJECTION);
        GeoLocationRequest geo = new GeoLocationRequest("US", "New York");

        EventRequest req = new EventRequest();
        req.setEventId("evt-test");
        req.setTimestamp(Instant.parse("2026-05-20T14:32:10Z"));
        req.setConfigId(1L);
        req.setPolicyId("pol_test");
        req.setClientIp("203.0.113.1");
        req.setHostname("www.example.com");
        req.setPath(path);
        req.setMethod("GET");
        req.setStatusCode(403);
        req.setUserAgent("TestAgent/1.0");
        req.setRule(rule);
        req.setAction(action);
        req.setGeoLocation(geo);
        req.setRequestSize(100);
        req.setResponseSize(50);
        return req;
    }
}
