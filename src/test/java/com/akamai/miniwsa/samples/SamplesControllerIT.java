package com.akamai.miniwsa.samples;

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
class SamplesControllerIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // configId 99998 is never used in any other test — ensures a zero-result query
    private static final long UNUSED_CONFIG_ID = 99998L;
    private static final long ISOLATED_CONFIG_ID = 77777L;

    private static final String EVENT_TEMPLATE = """
            [{
              "eventId": "samples-evt-001",
              "timestamp": "2026-05-20T14:32:10Z",
              "configId": %d,
              "policyId": "pol-001",
              "clientIp": "192.168.1.201",
              "hostname": "example.com",
              "path": "/api/items",
              "method": "GET",
              "statusCode": 200,
              "userAgent": "curl/7.0",
              "rule": {
                "id": "r-003",
                "name": "Bot Detection",
                "message": "Detected bot activity",
                "severity": "LOW",
                "category": "BOT"
              },
              "action": "MONITOR",
              "geoLocation": { "country": "FR", "city": "Paris" },
              "requestSize": 128,
              "responseSize": 256
            }]
            """;

    @Test
    void noMatchingEvents_returns200WithZeroTotal() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/events/samples")
                .param("configId", String.valueOf(UNUSED_CONFIG_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void afterIngestingOneEvent_returnsItInResults() throws Exception {
        String payload = String.format(EVENT_TEMPLATE, ISOLATED_CONFIG_ID);
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/events/samples")
                .param("configId", String.valueOf(ISOLATED_CONFIG_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.events").isNotEmpty());
    }

    @Test
    void invalidEnumForCategory_returns400() throws Exception {
        // Query-param enum binding raises MethodArgumentTypeMismatchException → 400
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/events/samples")
                .param("category", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}
