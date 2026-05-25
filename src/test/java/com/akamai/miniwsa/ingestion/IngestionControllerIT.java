package com.akamai.miniwsa.ingestion;

/**
 * Integration tests for the ingestion endpoint (POST /v1/events/ingest).
 *
 * Suggested coverage:
 *   - POST single event → 201 Created, event persisted in DB
 *   - POST batch of events → 201 Created, all events persisted
 *   - POST with missing required field → 400 Bad Request with field error
 *   - POST with unknown enum value (severity/category/action) → 400 Bad Request
 *   - POST batch where one event is invalid → 400, nothing persisted
 *   - POST with negative requestSize → 400 Bad Request
 */
class IngestionControllerIT {
}
