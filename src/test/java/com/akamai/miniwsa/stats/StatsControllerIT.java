package com.akamai.miniwsa.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class StatsControllerIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // configId 99999 is never used in any other test — ensures an isolated zero-count query
    private static final long UNUSED_CONFIG_ID = 99999L;
    private static final long ISOLATED_CONFIG_ID = 88888L;

    private static final String EVENT_TEMPLATE = """
            [{
              "eventId": "stats-evt-001",
              "timestamp": "2026-05-20T14:32:10Z",
              "configId": %d,
              "policyId": "pol-001",
              "clientIp": "192.168.1.200",
              "hostname": "example.com",
              "path": "/api/data",
              "method": "POST",
              "statusCode": 403,
              "userAgent": "curl/7.0",
              "rule": {
                "id": "r-002",
                "name": "XSS Detection",
                "message": "Detected XSS attempt",
                "severity": "MEDIUM",
                "category": "XSS"
              },
              "action": "ALERT",
              "geoLocation": { "country": "DE", "city": "Berlin" },
              "requestSize": 256,
              "responseSize": 512
            }]
            """;

    @Test
    void noMatchingEvents_summaryHasZeroTotal() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/stats/summary")
                .param("configId", String.valueOf(UNUSED_CONFIG_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(0));
    }

    @Test
    void afterIngestingOneEvent_totalIsOneAndByCategoryHasEntry() throws Exception {
        String payload = String.format(EVENT_TEMPLATE, ISOLATED_CONFIG_ID);
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/stats/summary")
                .param("configId", String.valueOf(ISOLATED_CONFIG_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1))
                .andExpect(jsonPath("$.byCategory").isNotEmpty());
    }
}
