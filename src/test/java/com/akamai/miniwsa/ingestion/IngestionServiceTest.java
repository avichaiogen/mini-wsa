package com.akamai.miniwsa.ingestion;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.SecurityEvent;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.enrichment.AttackClassifier;
import com.akamai.miniwsa.enrichment.ThreatScoreEngine;
import com.akamai.miniwsa.ingestion.dto.EventRequest;
import com.akamai.miniwsa.ingestion.dto.GeoLocationRequest;
import com.akamai.miniwsa.ingestion.dto.RuleRequest;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// No Spring context — pure Mockito unit test. No DB, no network.
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @Mock
    private AttackClassifier attackClassifier;

    @Mock
    private ThreatScoreEngine threatScoreEngine;

    // Injects the three mocks above into IngestionService via constructor injection
    @InjectMocks
    private IngestionService service;

    @Test
    void ingest_singleEvent_callsSaveAll() {
        when(attackClassifier.classify(any())).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(60);

        service.ingest(List.of(buildRequest()));

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void ingest_batchOfTwo_savesBothEntities() {
        when(attackClassifier.classify(any())).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(60);

        service.ingest(List.of(buildRequest(), buildRequest()));

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void ingest_setsReceivedAtServerSide() {
        when(attackClassifier.classify(any())).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(60);
        Instant before = Instant.now();

        service.ingest(List.of(buildRequest()));

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        // receivedAt must be set by the service, not copied from the DTO
        assertThat(captor.getValue().get(0).getReceivedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void ingest_populatesAttackTypeAndThreatScoreFromEnrichmentServices() {
        when(attackClassifier.classify(RuleCategory.INJECTION)).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(75);

        service.ingest(List.of(buildRequest()));

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        SecurityEvent saved = captor.getValue().get(0);
        assertThat(saved.getAttackType()).isEqualTo("SQL/Command Injection");
        assertThat(saved.getThreatScore()).isEqualTo(75);
    }

    @Test
    void ingest_mapsAllClientFieldsToEntity() {
        when(attackClassifier.classify(any())).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(60);
        EventRequest req = buildRequest();

        service.ingest(List.of(req));

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        SecurityEvent saved = captor.getValue().get(0);

        assertThat(saved.getEventId()).isEqualTo(req.getEventId());
        assertThat(saved.getClientIp()).isEqualTo(req.getClientIp());
        assertThat(saved.getConfigId()).isEqualTo(req.getConfigId());
        assertThat(saved.getRule().getSeverity()).isEqualTo(req.getRule().getSeverity());
        assertThat(saved.getRule().getCategory()).isEqualTo(req.getRule().getCategory());
        assertThat(saved.getGeoLocation().getCountry()).isEqualTo(req.getGeoLocation().getCountry());
        assertThat(saved.getGeoLocation().getCity()).isEqualTo(req.getGeoLocation().getCity());
        assertThat(saved.getAction()).isEqualTo(req.getAction());
        assertThat(saved.getRequestSize()).isEqualTo(req.getRequestSize());
        assertThat(saved.getResponseSize()).isEqualTo(req.getResponseSize());
    }

    // --- Unhappy path ---

    @Test
    void ingest_emptyList_callsSaveAllWithEmptyList() {
        service.ingest(List.of());

        ArgumentCaptor<List<SecurityEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void ingest_repositoryThrows_propagatesException() {
        when(attackClassifier.classify(any())).thenReturn("SQL/Command Injection");
        when(repository.countByClientIpAndReceivedAtGreaterThanEqual(anyString(), any(Instant.class))).thenReturn(0L);
        when(threatScoreEngine.compute(any(), anyLong())).thenReturn(60);
        when(repository.saveAll(any())).thenThrow(new DataIntegrityViolationException("DB error"));

        assertThatThrownBy(() -> service.ingest(List.of(buildRequest())))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void ingest_attackClassifierThrows_propagatesException() {
        // classify() is called before the repo count query; when it throws saveAll is never reached
        when(attackClassifier.classify(any())).thenThrow(new RuntimeException("classifier failure"));

        assertThatThrownBy(() -> service.ingest(List.of(buildRequest())))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("classifier failure");

        verify(repository, never()).saveAll(any());
    }

    private EventRequest buildRequest() {
        RuleRequest rule = new RuleRequest(
                "950001", "SQL_INJECTION", "SQL Injection Attack Detected",
                Severity.CRITICAL, RuleCategory.INJECTION);
        GeoLocationRequest geo = new GeoLocationRequest("CN", "Beijing");

        EventRequest req = new EventRequest();
        req.setEventId("evt-001");
        req.setTimestamp(Instant.parse("2026-05-20T14:32:10Z"));
        req.setConfigId(14227L);
        req.setPolicyId("pol_web1");
        req.setClientIp("203.0.113.42");
        req.setHostname("www.example.com");
        req.setPath("/api/v1/login");
        req.setMethod("POST");
        req.setStatusCode(403);
        req.setUserAgent("Mozilla/5.0");
        req.setRule(rule);
        req.setAction(Action.DENY);
        req.setGeoLocation(geo);
        req.setRequestSize(1024);
        req.setResponseSize(256);
        return req;
    }
}
