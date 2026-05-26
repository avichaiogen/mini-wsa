package com.akamai.miniwsa.repository;

import com.akamai.miniwsa.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityEventRepositoryIT {

    @Autowired
    private SecurityEventRepository repository;

    private SecurityEvent buildEvent(String clientIp, Instant receivedAt) {
        SecurityEvent e = new SecurityEvent();
        e.setEventId("evt-001");
        e.setTimestamp(Instant.parse("2026-05-20T14:32:10Z"));
        e.setConfigId(1L);
        e.setPolicyId("pol-001");
        e.setClientIp(clientIp);
        e.setHostname("example.com");
        e.setPath("/api/users");
        e.setMethod("GET");
        e.setStatusCode(200);
        e.setUserAgent("Mozilla/5.0");
        e.setRule(new Rule("r-001", "SQL Injection", "Detected SQL injection", Severity.HIGH, RuleCategory.INJECTION));
        e.setAction(Action.DENY);
        e.setGeoLocation(new GeoLocation("US", "New York"));
        e.setRequestSize(512);
        e.setResponseSize(1024);
        e.setAttackType("SQL/Command Injection");
        e.setThreatScore(60);
        e.setReceivedAt(receivedAt);
        return e;
    }

    @Test
    void saveAndFindById_scalarFieldsSurvivePersistence() {
        SecurityEvent saved = repository.save(buildEvent("10.0.0.1", Instant.now()));

        SecurityEvent found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getEventId()).isEqualTo("evt-001");
        assertThat(found.getConfigId()).isEqualTo(1L);
        assertThat(found.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(found.getThreatScore()).isEqualTo(60);
        assertThat(found.getAttackType()).isEqualTo("SQL/Command Injection");
    }

    @Test
    void embeddedRuleAndGeoLocation_survivePersistence() {
        SecurityEvent saved = repository.save(buildEvent("10.0.0.2", Instant.now()));

        SecurityEvent found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getRule().getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(found.getRule().getCategory()).isEqualTo(RuleCategory.INJECTION);
        assertThat(found.getGeoLocation().getCountry()).isEqualTo("US");
        assertThat(found.getGeoLocation().getCity()).isEqualTo("New York");
    }

    @Test
    void countByClientIpAndReceivedAt_withinWindowOnly() {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(600);

        repository.save(buildEvent("10.0.0.3", now));
        // event outside the 10-minute window
        repository.save(buildEvent("10.0.0.3", now.minusSeconds(660)));

        long withinWindow = repository.countByClientIpAndReceivedAtGreaterThanEqual("10.0.0.3", windowStart);
        assertThat(withinWindow).isEqualTo(1);

        long allTime = repository.countByClientIpAndReceivedAtGreaterThanEqual("10.0.0.3", now.minusSeconds(1200));
        assertThat(allTime).isEqualTo(2);
    }
}
