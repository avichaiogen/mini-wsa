package com.akamai.miniwsa.stats;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import com.akamai.miniwsa.stats.dto.ActionRow;
import com.akamai.miniwsa.stats.dto.AttackerRow;
import com.akamai.miniwsa.stats.dto.CategoryRow;
import com.akamai.miniwsa.stats.dto.PathRow;
import com.akamai.miniwsa.stats.dto.StatsSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getSummary_withConfigIdAndTimeRange_returnsCorrectTotals() {
        Long configId = 14227L;
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to   = Instant.parse("2026-05-31T23:59:59Z");

        when(repository.countFiltered(configId, from, to)).thenReturn(1523L);
        when(repository.countByCategory(eq(configId), eq(from), eq(to))).thenReturn(List.of(
                new CategoryRow(RuleCategory.INJECTION, 450, 72.3)));
        when(repository.countByAction(eq(configId), eq(from), eq(to))).thenReturn(List.of(
                new ActionRow(Action.DENY, 890)));
        when(repository.topAttackers(eq(configId), eq(from), eq(to), any(Pageable.class))).thenReturn(List.of(
                new AttackerRow("203.0.113.42", 87, 81.2)));
        when(repository.topTargetedPaths(eq(configId), eq(from), eq(to), any(Pageable.class))).thenReturn(List.of(
                new PathRow("/api/v1/login", 234)));

        StatsSummaryResponse result = statsService.getSummary(configId, from, to);

        assertThat(result.totalEvents()).isEqualTo(1523L);
        assertThat(result.configId()).isEqualTo(configId);
        assertThat(result.timeRange().from()).isEqualTo(from);
        assertThat(result.timeRange().to()).isEqualTo(to);
    }

    @Test
    void getSummary_noConfigId_passesNullToRepository() {
        stubEmptyResults(null, null, null);

        statsService.getSummary(null, null, null);

        verify(repository).countFiltered(null, null, null);
        verify(repository).countByCategory(null, null, null);
    }

    @Test
    void getSummary_noTimeRange_passesNullFromAndTo() {
        Long configId = 1L;
        stubEmptyResults(configId, null, null);

        statsService.getSummary(configId, null, null);

        verify(repository).countFiltered(eq(configId), eq(null), eq(null));
    }

    @Test
    void getSummary_emptyResults_returnsZerosAndEmptyCollections() {
        stubEmptyResults(null, null, null);

        StatsSummaryResponse result = statsService.getSummary(null, null, null);

        assertThat(result.totalEvents()).isZero();
        assertThat(result.byCategory()).isEmpty();
        assertThat(result.byAction()).isEmpty();
        assertThat(result.topAttackers()).isEmpty();
        assertThat(result.topTargetedPaths()).isEmpty();
    }

    @Test
    void getSummary_byCategoryMappedCorrectly() {
        when(repository.countFiltered(any(), any(), any())).thenReturn(5L);
        when(repository.countByCategory(any(), any(), any())).thenReturn(List.of(
                new CategoryRow(RuleCategory.INJECTION, 3, 70.0),
                new CategoryRow(RuleCategory.BOT, 2, 45.0)));
        when(repository.countByAction(any(), any(), any())).thenReturn(List.of());
        when(repository.topAttackers(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.topTargetedPaths(any(), any(), any(), any())).thenReturn(List.of());

        StatsSummaryResponse result = statsService.getSummary(null, null, null);

        assertThat(result.byCategory()).containsKey(RuleCategory.INJECTION);
        assertThat(result.byCategory().get(RuleCategory.INJECTION).count()).isEqualTo(3);
        assertThat(result.byCategory().get(RuleCategory.INJECTION).avgThreatScore()).isEqualTo(70.0);
        assertThat(result.byCategory()).containsKey(RuleCategory.BOT);
    }

    @Test
    void getSummary_byActionMappedCorrectly() {
        when(repository.countFiltered(any(), any(), any())).thenReturn(3L);
        when(repository.countByCategory(any(), any(), any())).thenReturn(List.of());
        when(repository.countByAction(any(), any(), any())).thenReturn(List.of(
                new ActionRow(Action.DENY, 2),
                new ActionRow(Action.ALERT, 1)));
        when(repository.topAttackers(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.topTargetedPaths(any(), any(), any(), any())).thenReturn(List.of());

        StatsSummaryResponse result = statsService.getSummary(null, null, null);

        assertThat(result.byAction()).containsEntry(Action.DENY, 2L);
        assertThat(result.byAction()).containsEntry(Action.ALERT, 1L);
    }

    // --- Unhappy path ---

    @Test
    void getSummary_repositoryThrows_propagatesException() {
        when(repository.countFiltered(any(), any(), any()))
                .thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> statsService.getSummary(null, null, null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void getSummary_fromAfterTo_returnsZeroTotals() {
        // Service has no cross-field validation; passes inverted range to repo which returns 0
        Instant from = Instant.parse("2026-12-31T00:00:00Z");
        Instant to   = Instant.parse("2026-01-01T00:00:00Z");
        stubEmptyResults(null, from, to);

        StatsSummaryResponse result = statsService.getSummary(null, from, to);

        assertThat(result.totalEvents()).isZero();
        assertThat(result.byCategory()).isEmpty();
        assertThat(result.topAttackers()).isEmpty();
    }

    @Test
    void getSummary_exactly10Attackers_allReturned() {
        List<AttackerRow> ten = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new AttackerRow("10.0.0." + i, i, 50.0))
                .toList();
        when(repository.countFiltered(any(), any(), any())).thenReturn(10L);
        when(repository.countByCategory(any(), any(), any())).thenReturn(List.of());
        when(repository.countByAction(any(), any(), any())).thenReturn(List.of());
        when(repository.topAttackers(any(), any(), any(), any())).thenReturn(ten);
        when(repository.topTargetedPaths(any(), any(), any(), any())).thenReturn(List.of());

        StatsSummaryResponse result = statsService.getSummary(null, null, null);

        assertThat(result.topAttackers()).hasSize(10);
    }

    private void stubEmptyResults(Long configId, Instant from, Instant to) {
        when(repository.countFiltered(configId, from, to)).thenReturn(0L);
        when(repository.countByCategory(eq(configId), eq(from), eq(to))).thenReturn(List.of());
        when(repository.countByAction(eq(configId), eq(from), eq(to))).thenReturn(List.of());
        when(repository.topAttackers(eq(configId), eq(from), eq(to), any(Pageable.class))).thenReturn(List.of());
        when(repository.topTargetedPaths(eq(configId), eq(from), eq(to), any(Pageable.class))).thenReturn(List.of());
    }
}
