package com.akamai.miniwsa.stats;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.GeoLocation;
import com.akamai.miniwsa.domain.Rule;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.SecurityEvent;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full Spring context with H2 in PostgreSQL mode; Flyway disabled; ddl-auto=create-drop (application-test.yml)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class StatsControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SecurityEventRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void getSummary_noEvents_returnsZeroes() throws Exception {
        mockMvc.perform(get("/v1/stats/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(0))
                .andExpect(jsonPath("$.byCategory").isEmpty())
                .andExpect(jsonPath("$.byAction").isEmpty())
                .andExpect(jsonPath("$.topAttackers").isEmpty())
                .andExpect(jsonPath("$.topTargetedPaths").isEmpty());
    }

    @Test
    void getSummary_filteredByConfigId_onlyMatchingEventsIncluded() throws Exception {
        repository.save(buildEvent(1L, "1.1.1.1", RuleCategory.INJECTION, Action.DENY, "/login", Instant.now()));
        repository.save(buildEvent(2L, "2.2.2.2", RuleCategory.BOT, Action.ALERT, "/home", Instant.now()));

        mockMvc.perform(get("/v1/stats/summary").param("configId", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1))
                .andExpect(jsonPath("$.configId").value(1));
    }

    @Test
    void getSummary_filteredByTimeRange_onlyMatchingEventsIncluded() throws Exception {
        Instant old = Instant.parse("2026-01-01T00:00:00Z");
        Instant recent = Instant.parse("2026-05-20T00:00:00Z");

        SecurityEvent oldEvent = buildEvent(1L, "1.1.1.1", RuleCategory.BOT, Action.DENY, "/foo", old);
        oldEvent.setReceivedAt(old);
        SecurityEvent recentEvent = buildEvent(1L, "2.2.2.2", RuleCategory.INJECTION, Action.DENY, "/bar", recent);
        recentEvent.setReceivedAt(recent);
        repository.saveAll(List.of(oldEvent, recentEvent));

        mockMvc.perform(get("/v1/stats/summary")
                        .param("from", "2026-05-01T00:00:00Z")
                        .param("to", "2026-05-31T23:59:59Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1));
    }

    @Test
    void getSummary_byCategory_countsCorrect() throws Exception {
        for (int i = 0; i < 3; i++) {
            repository.save(buildEvent(1L, "1.1.1." + i, RuleCategory.INJECTION, Action.DENY, "/a", Instant.now()));
        }
        for (int i = 0; i < 2; i++) {
            repository.save(buildEvent(1L, "2.2.2." + i, RuleCategory.BOT, Action.ALERT, "/b", Instant.now()));
        }

        mockMvc.perform(get("/v1/stats/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.INJECTION.count").value(3))
                .andExpect(jsonPath("$.byCategory.BOT.count").value(2));
    }

    @Test
    void getSummary_topAttackers_top10Max() throws Exception {
        List<SecurityEvent> events = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            events.add(buildEvent(1L, "10.0.0." + i, RuleCategory.INJECTION, Action.DENY, "/api", Instant.now()));
        }
        repository.saveAll(events);

        mockMvc.perform(get("/v1/stats/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topAttackers", hasSize(10)));
    }

    private SecurityEvent buildEvent(Long configId, String clientIp, RuleCategory category,
                                     Action action, String path, Instant receivedAt) {
        SecurityEvent e = new SecurityEvent();
        e.setEventId("evt-" + System.nanoTime());
        e.setTimestamp(Instant.now());
        e.setConfigId(configId);
        e.setPolicyId("pol_test");
        e.setClientIp(clientIp);
        e.setHostname("test.example.com");
        e.setPath(path);
        e.setMethod("GET");
        e.setStatusCode(403);
        e.setUserAgent("TestAgent/1.0");
        e.setRule(new Rule("r001", "TEST_RULE", "Test", Severity.HIGH, category));
        e.setAction(action);
        e.setGeoLocation(new GeoLocation("US", "New York"));
        e.setRequestSize(512);
        e.setResponseSize(128);
        e.setAttackType("Test Attack");
        e.setThreatScore(50);
        e.setReceivedAt(receivedAt);
        return e;
    }
}
