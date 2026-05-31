package com.akamai.miniwsa.samples;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.GeoLocation;
import com.akamai.miniwsa.domain.Rule;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.SecurityEvent;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import com.akamai.miniwsa.samples.dto.SamplesResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamplesServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @InjectMocks
    private SamplesService samplesService;

    // --- Happy paths ---

    @Test
    void getSamples_noFilters_returnsMatchingResponse() {
        List<SecurityEvent> entities = List.of(buildEvent("1"), buildEvent("2"), buildEvent("3"));
        when(repository.countSamples(null, null, null, null, null)).thenReturn(3L);
        when(repository.findSamples(eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(entities);

        SamplesResponse result = samplesService.getSamples(null, null, null, null, null, 20, 0);

        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.events()).hasSize(3);
    }

    @Test
    void getSamples_withCategoryFilter_propagatesToRepository() {
        when(repository.countSamples(any(), any(), any(), eq(RuleCategory.INJECTION), any())).thenReturn(1L);
        when(repository.findSamples(any(), any(), any(), eq(RuleCategory.INJECTION), any(), any(Pageable.class)))
                .thenReturn(List.of(buildEvent("x")));

        samplesService.getSamples(null, null, null, RuleCategory.INJECTION, null, 20, 0);

        verify(repository).countSamples(null, null, null, RuleCategory.INJECTION, null);
        verify(repository).findSamples(eq(null), eq(null), eq(null),
                eq(RuleCategory.INJECTION), eq(null), any(Pageable.class));
    }

    @Test
    void getSamples_entityMappedToResponseCorrectly() {
        Instant ts         = Instant.parse("2026-05-20T14:32:10Z");
        Instant receivedAt = Instant.parse("2026-05-20T14:32:11Z");
        SecurityEvent e = buildEvent("evt-00132");
        e.setTimestamp(ts);
        e.setReceivedAt(receivedAt);
        e.setConfigId(14227L);
        e.setClientIp("203.0.113.42");
        e.setPath("/api/v1/login");
        e.setRule(new Rule("950001", "SQL_INJECTION", "SQL Injection Attack Detected",
                Severity.CRITICAL, RuleCategory.INJECTION));
        e.setAction(Action.DENY);
        e.setGeoLocation(new GeoLocation("CN", "Beijing"));
        e.setRequestSize(1024);
        e.setResponseSize(256);
        e.setAttackType("SQL/Command Injection");
        e.setThreatScore(75);

        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(1L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(e));

        SamplesResponse result = samplesService.getSamples(null, null, null, null, null, 20, 0);

        var event = result.events().get(0);
        assertThat(event.eventId()).isEqualTo("evt-00132");
        assertThat(event.timestamp()).isEqualTo(ts);
        assertThat(event.receivedAt()).isEqualTo(receivedAt);
        assertThat(event.configId()).isEqualTo(14227L);
        assertThat(event.clientIp()).isEqualTo("203.0.113.42");
        assertThat(event.rule().id()).isEqualTo("950001");
        assertThat(event.rule().severity()).isEqualTo(Severity.CRITICAL);
        assertThat(event.rule().category()).isEqualTo(RuleCategory.INJECTION);
        assertThat(event.action()).isEqualTo(Action.DENY);
        assertThat(event.geoLocation().country()).isEqualTo("CN");
        assertThat(event.geoLocation().city()).isEqualTo("Beijing");
        assertThat(event.requestSize()).isEqualTo(1024);
        assertThat(event.responseSize()).isEqualTo(256);
        assertThat(event.attackType()).isEqualTo("SQL/Command Injection");
        assertThat(event.threatScore()).isEqualTo(75);
    }

    // --- Unhappy paths ---

    @Test
    void getSamples_limitCappedAt100_whenExceeded() {
        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(0L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        samplesService.getSamples(null, null, null, null, null, 500, 0);

        verify(repository).findSamples(any(), any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(SamplesService.MAX_LIMIT);
    }

    @Test
    void getSamples_limitClampedToOne_whenNegative() {
        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(0L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        samplesService.getSamples(null, null, null, null, null, -1, 0);

        verify(repository).findSamples(any(), any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getSamples_offsetClampedToZero_whenNegative() {
        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(0L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        samplesService.getSamples(null, null, null, null, null, 20, -5);

        verify(repository).findSamples(any(), any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(0L);
    }

    @Test
    void getSamples_nonMultipleOffset_isPreserved() {
        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(0L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        samplesService.getSamples(null, null, null, null, null, 20, 15);

        verify(repository).findSamples(any(), any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(15L);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getSamples_emptyResult_returnsTotalZeroAndEmptyList() {
        when(repository.countSamples(any(), any(), any(), any(), any())).thenReturn(0L);
        when(repository.findSamples(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        SamplesResponse result = samplesService.getSamples(null, null, null, null, null, 20, 0);

        assertThat(result.total()).isZero();
        assertThat(result.events()).isEmpty();
    }

    private SecurityEvent buildEvent(String eventId) {
        SecurityEvent e = new SecurityEvent();
        e.setEventId(eventId);
        e.setTimestamp(Instant.now());
        e.setReceivedAt(Instant.now());
        e.setConfigId(1L);
        e.setPolicyId("pol_test");
        e.setClientIp("1.2.3.4");
        e.setHostname("test.example.com");
        e.setPath("/test");
        e.setMethod("GET");
        e.setStatusCode(403);
        e.setUserAgent("TestAgent/1.0");
        e.setRule(new Rule("r001", "TEST_RULE", "Test rule", Severity.HIGH, RuleCategory.BOT));
        e.setAction(Action.ALERT);
        e.setGeoLocation(new GeoLocation("US", "New York"));
        e.setRequestSize(100);
        e.setResponseSize(50);
        e.setAttackType("Bot Activity");
        e.setThreatScore(40);
        return e;
    }
}
