package com.akamai.miniwsa.generator;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // --- nextFileIndex ---

    @Test
    void nextFileIndex_emptyDir_returnsZero(@TempDir Path tempDir) {
        assertThat(DataGenerator.nextFileIndex(tempDir.toFile())).isEqualTo(0);
    }

    @Test
    void nextFileIndex_existingFiles_returnsNextIndex(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("events_000.json"), "[]");
        Files.writeString(tempDir.resolve("events_001.json"), "[]");
        Files.writeString(tempDir.resolve("events_002.json"), "[]");
        assertThat(DataGenerator.nextFileIndex(tempDir.toFile())).isEqualTo(3);
    }

    @Test
    void writeBatches_appendsToExistingFiles(@TempDir Path tempDir) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        // Pre-create events_000.json so the second run should start at events_001.json
        Files.writeString(tempDir.resolve("events_000.json"), "[]");

        List<EventRequest> events = DataGenerator.generate(5, 0, 0);
        List<File> written = DataGenerator.writeBatches(events, tempDir.toString(), 10, mapper);

        assertThat(written).hasSize(1);
        assertThat(written.get(0).getName()).isEqualTo("events_001.json");
        assertThat(new File(tempDir.toFile(), "events_000.json")).exists();
    }

    // --- ingestFiles: unhappy path ---

    @Test
    void ingestFiles_serverUnreachable_throwsIOException(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("events_000.json");
        Files.writeString(file, "[]");

        assertThatThrownBy(() -> DataGenerator.ingestFiles(List.of(file.toFile()), "http://localhost:19999"))
                .isInstanceOf(IOException.class);
    }

    // --- New P1 tests ---

    @Test
    void generate_eventIdsAreZeroPaddedSequential() {
        List<EventRequest> events = DataGenerator.generate(10, 0, 0);
        assertThat(events).hasSize(10);
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).getEventId()).isEqualTo(String.format("%06d", i + 1));
        }
    }

    @Test
    void generate_timestampsOnOrAfterEpoch2020() {
        Instant epoch2020 = Instant.parse("2020-01-01T00:00:00Z");
        List<EventRequest> events = DataGenerator.generate(50, 0, 0);
        events.forEach(e -> assertThat(e.getTimestamp()).isAfterOrEqualTo(epoch2020));
    }

    @Test
    void generate_methodsOnlyFromAllowedSet() {
        Set<String> allowed = Set.of("GET", "POST", "PUT", "DELETE");
        List<EventRequest> events = DataGenerator.generate(200, 0, 0);
        events.forEach(e -> assertThat(allowed).contains(e.getMethod()));
    }

    @Test
    void generate_largeSample_containsPutAndDelete() {
        // PUT+DELETE at 20% combined — with 200 events the probability of neither
        // appearing is astronomically small (< 10^-18)
        List<EventRequest> events = DataGenerator.generate(200, 0, 0);
        Set<String> methods = events.stream().map(EventRequest::getMethod).collect(Collectors.toSet());
        assertThat(methods).contains("PUT", "DELETE");
    }

    @Test
    void printStats_doesNotThrow_onNonEmptyList() {
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            DataGenerator.printStats(DataGenerator.generate(50, 2, 5));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void printStats_doesNotThrow_onEmptyList() {
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            DataGenerator.printStats(List.of());
        } finally {
            System.setOut(original);
        }
    }
}
