package com.akamai.miniwsa.samples;

import com.akamai.miniwsa.AbstractControllerIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SamplesControllerIT extends AbstractControllerIT {

    // configId 99998 is never used in any other test — ensures a zero-result query
    private static final long UNUSED_CONFIG_ID = 99998L;
    private static final long ISOLATED_CONFIG_ID = 77777L;
    private static final long PAGINATED_CONFIG_ID = 44444L;

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
    void pagination_returnsCorrectSlicesAndTotal() throws Exception {
        String batch = """
                [
                  {
                    "eventId": "p-001", "timestamp": "2026-05-20T10:00:00Z",
                    "configId": 44444, "policyId": "pol-p",
                    "clientIp": "10.0.0.1", "hostname": "h.example.com",
                    "path": "/p", "method": "GET", "statusCode": 200,
                    "userAgent": "curl/7.0",
                    "rule": { "id": "r-p", "name": "Bot Detection", "message": "bot",
                              "severity": "LOW", "category": "BOT" },
                    "action": "MONITOR",
                    "geoLocation": { "country": "US", "city": "NY" },
                    "requestSize": 10, "responseSize": 20
                  },
                  {
                    "eventId": "p-002", "timestamp": "2026-05-20T11:00:00Z",
                    "configId": 44444, "policyId": "pol-p",
                    "clientIp": "10.0.0.1", "hostname": "h.example.com",
                    "path": "/p", "method": "GET", "statusCode": 200,
                    "userAgent": "curl/7.0",
                    "rule": { "id": "r-p", "name": "Bot Detection", "message": "bot",
                              "severity": "LOW", "category": "BOT" },
                    "action": "MONITOR",
                    "geoLocation": { "country": "US", "city": "NY" },
                    "requestSize": 10, "responseSize": 20
                  },
                  {
                    "eventId": "p-003", "timestamp": "2026-05-20T12:00:00Z",
                    "configId": 44444, "policyId": "pol-p",
                    "clientIp": "10.0.0.1", "hostname": "h.example.com",
                    "path": "/p", "method": "GET", "statusCode": 200,
                    "userAgent": "curl/7.0",
                    "rule": { "id": "r-p", "name": "Bot Detection", "message": "bot",
                              "severity": "LOW", "category": "BOT" },
                    "action": "MONITOR",
                    "geoLocation": { "country": "US", "city": "NY" },
                    "requestSize": 10, "responseSize": 20
                  }
                ]
                """;

        mockMvc.perform(post("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batch))
                .andExpect(status().isCreated());

        // first page: limit=2, offset=0 → 2 events, newest first (DESC)
        mockMvc.perform(get("/v1/events/samples")
                .param("configId", String.valueOf(PAGINATED_CONFIG_ID))
                .param("limit", "2").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].eventId").value("p-003"));

        // second page: limit=2, offset=2 → 1 event remains
        mockMvc.perform(get("/v1/events/samples")
                .param("configId", String.valueOf(PAGINATED_CONFIG_ID))
                .param("limit", "2").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].eventId").value("p-001"));

        // past the end: offset=3 → 0 events
        mockMvc.perform(get("/v1/events/samples")
                .param("configId", String.valueOf(PAGINATED_CONFIG_ID))
                .param("limit", "2").param("offset", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void invalidEnumForCategory_returns400() throws Exception {
        // Query-param enum binding raises MethodArgumentTypeMismatchException → 400
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/events/samples")
                .param("category", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}
