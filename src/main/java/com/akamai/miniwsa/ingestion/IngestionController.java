package com.akamai.miniwsa.ingestion;

import com.akamai.miniwsa.ingestion.dto.EventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the event ingestion endpoint.
 *
 * Accepts both a single event object and a batch array, because Jackson is configured
 * with accept-single-value-as-array=true (application.yml).
 *
 * @Valid triggers Bean Validation on each EventRequest before the service is called.
 * Validation failures are handled by GlobalExceptionHandler → 400 Bad Request.
 */
@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public void ingest(@Valid @RequestBody List<EventRequest> events) {
        ingestionService.ingest(events);
    }
}
