package com.akamai.miniwsa.repository;

import com.akamai.miniwsa.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityEventRepositoryIT {

    @Autowired
    SecurityEventRepository repository;

    @Test
    void save_andFindById_roundtrip() {
        SecurityEvent event = buildEvent();

        SecurityEvent saved = repository.saveAndFlush(event);

        Optional<SecurityEvent> found = repository.findById(saved.getId());
        assertThat(found).isPresent();

        SecurityEvent e = found.get();
        assertThat(e.getEventId()).isEqualTo("evt-00132");
        assertThat(e.getClientIp()).isEqualTo("203.0.113.42");
        assertThat(e.getConfigId()).isEqualTo(14227L);
        assertThat(e.getRule().getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(e.getRule().getCategory()).isEqualTo(RuleCategory.INJECTION);
        assertThat(e.getAction()).isEqualTo(Action.DENY);
        assertThat(e.getGeoLocation().getCountry()).isEqualTo("CN");
        assertThat(e.getGeoLocation().getCity()).isEqualTo("Beijing");
        assertThat(e.getAttackType()).isEqualTo("SQL/Command Injection");
        assertThat(e.getThreatScore()).isEqualTo(75);
        assertThat(e.getRequestSize()).isEqualTo(1024);
        assertThat(e.getResponseSize()).isEqualTo(256);
    }

    @Test
    void save_persistsAllFields_withoutDataLoss() {
        SecurityEvent event = buildEvent();
        event.setThreatScore(100);

        SecurityEvent saved = repository.saveAndFlush(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReceivedAt()).isNotNull();
        assertThat(saved.getThreatScore()).isEqualTo(100);
        assertThat(saved.getRule().getId()).isEqualTo("950001");
        assertThat(saved.getRule().getName()).isEqualTo("SQL_INJECTION");
        assertThat(saved.getPolicyId()).isEqualTo("pol_web1");
        assertThat(saved.getHostname()).isEqualTo("www.example.com");
        assertThat(saved.getStatusCode()).isEqualTo(403);
    }

    private SecurityEvent buildEvent() {
        SecurityEvent e = new SecurityEvent();
        e.setEventId("evt-00132");
        e.setTimestamp(Instant.parse("2026-05-20T14:32:10Z"));
        e.setConfigId(14227L);
        e.setPolicyId("pol_web1");
        e.setClientIp("203.0.113.42");
        e.setHostname("www.example.com");
        e.setPath("/api/v1/login");
        e.setMethod("POST");
        e.setStatusCode(403);
        e.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        e.setRule(new Rule("950001", "SQL_INJECTION", "SQL Injection Attack Detected",
                Severity.CRITICAL, RuleCategory.INJECTION));
        e.setAction(Action.DENY);
        e.setGeoLocation(new GeoLocation("CN", "Beijing"));
        e.setRequestSize(1024);
        e.setResponseSize(256);
        e.setAttackType("SQL/Command Injection");
        e.setThreatScore(75);
        e.setReceivedAt(Instant.now());
        return e;
    }
}
