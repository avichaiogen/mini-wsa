package com.akamai.miniwsa.ingestion;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class IngestionControllerIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private static final String VALID_EVENT = """
            {
              "eventId": "evt-001",
              "timestamp": "2026-05-20T14:32:10Z",
              "configId": 1,
              "policyId": "pol-001",
              "clientIp": "192.168.1.100",
              "hostname": "example.com",
              "path": "/api/users",
              "method": "GET",
              "statusCode": 200,
              "userAgent": "Mozilla/5.0",
              "rule": {
                "id": "r-001",
                "name": "SQL Injection",
                "message": "Detected SQL injection attempt",
                "severity": "HIGH",
                "category": "INJECTION"
              },
              "action": "DENY",
              "geoLocation": { "country": "US", "city": "New York" },
              "requestSize": 512,
              "responseSize": 1024
            }
            """;

    @Test
    void postSingleEvent_returns201() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + VALID_EVENT + "]"))
                .andExpect(status().isCreated());
    }

    @Test
    void postBatchOfTwoEvents_returns201() throws Exception {
        String batch = "[" + VALID_EVENT + "," + VALID_EVENT.replace("\"evt-001\"", "\"evt-002\"") + "]";
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batch))
                .andExpect(status().isCreated());
    }

    @Test
    void postMissingRequiredField_returns400() throws Exception {
        String invalid = VALID_EVENT.replace("\"clientIp\": \"192.168.1.100\",", "");
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + invalid + "]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postUnknownEnumValue_returns400() throws Exception {
        // "EXTREME" is not a valid Severity — Jackson throws HttpMessageNotReadableException → 400
        String invalid = VALID_EVENT.replace("\"severity\": \"HIGH\"", "\"severity\": \"EXTREME\"");
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + invalid + "]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postBatchWithOneInvalidEvent_returns400() throws Exception {
        // First event valid, second missing clientIp — all-or-nothing → 400
        String invalidEvent = VALID_EVENT
                .replace("\"evt-001\"", "\"evt-003\"")
                .replace("\"clientIp\": \"192.168.1.100\",", "");
        String batch = "[" + VALID_EVENT.replace("\"evt-001\"", "\"evt-004\"") + "," + invalidEvent + "]";
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batch))
                .andExpect(status().isBadRequest());
    }
}
