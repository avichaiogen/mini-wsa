package com.akamai.miniwsa.generator;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DataGeneratorTest {

    // --- Happy paths ---

    @Test
    void generate_countMatchesRequested() {
        assertThat(DataGenerator.generate(50, 2, 5)).hasSize(50);
    }

    @Test
    void generate_waveEventsShareSameIp() {
        // 1 wave of 10 → at least one IP must appear ≥ 10 times
        List<EventRequest> events = DataGenerator.generate(50, 1, 10);
        Map<String, Long> ipCounts = events.stream()
                .collect(Collectors.groupingBy(EventRequest::getClientIp, Collectors.counting()));
        assertThat(ipCounts.values()).anyMatch(count -> count >= 10);
    }

    @Test
    void generate_allRequiredFieldsNonNull() {
        List<EventRequest> events = DataGenerator.generate(20, 0, 0);
        for (EventRequest e : events) {
            assertThat(e.getEventId()).isNotBlank();
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(e.getConfigId()).isNotNull();
            assertThat(e.getClientIp()).isNotBlank();
            assertThat(e.getHostname()).isNotBlank();
            assertThat(e.getPath()).isNotBlank();
            assertThat(e.getMethod()).isNotBlank();
            assertThat(e.getRule()).isNotNull();
            assertThat(e.getRule().getCategory()).isNotNull();
            assertThat(e.getRule().getSeverity()).isNotNull();
            assertThat(e.getAction()).isNotNull();
            assertThat(e.getGeoLocation()).isNotNull();
            assertThat(e.getGeoLocation().getCountry()).isNotBlank();
            assertThat(e.getGeoLocation().getCity()).isNotBlank();
            assertThat(e.getRequestSize()).isGreaterThanOrEqualTo(0);
            assertThat(e.getResponseSize()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void generate_timestampsAreInThePast() {
        Instant before = Instant.now();
        List<EventRequest> events = DataGenerator.generate(20, 0, 0);
        events.forEach(e -> assertThat(e.getTimestamp()).isBefore(before));
    }

    // --- Unhappy paths ---

    @Test
    void generate_countZero_returnsEmptyList() {
        assertThat(DataGenerator.generate(0, 0, 0)).isEmpty();
    }

    @Test
    void generate_negativeCount_returnsEmptyList() {
        assertThat(DataGenerator.generate(-1, 0, 0)).isEmpty();
    }

    @Test
    void generate_wavesTotalExceedsCount_doesNotExceedCount() {
        // waves=10, waveSize=20 would produce 200 wave events, but count cap is 50
        assertThat(DataGenerator.generate(50, 10, 20)).hasSize(50);
    }

    // --- writeBatches: file output ---

    @Test
    void writeBatches_createsCorrectNumberOfFiles(@TempDir Path tempDir) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        List<EventRequest> events = DataGenerator.generate(25, 0, 0);

        DataGenerator.writeBatches(events, tempDir.toString(), 10, mapper);

        File[] files = tempDir.toFile().listFiles((d, name) -> name.endsWith(".json"));
        // 25 events / 10 per file = 3 files (10 + 10 + 5)
        assertThat(files).hasSize(3);
    }

    @Test
    void writeBatches_filesContainValidJson(@TempDir Path tempDir) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        List<EventRequest> events = DataGenerator.generate(5, 0, 0);

        DataGenerator.writeBatches(events, tempDir.toString(), 10, mapper);

        File file = new File(tempDir.toFile(), "events_000.json");
        assertThat(file).exists();
        // Verify it deserializes back to a list of the correct size
        EventRequest[] parsed = mapper.readValue(file, EventRequest[].class);
        assertThat(parsed).hasSize(5);
    }
}
