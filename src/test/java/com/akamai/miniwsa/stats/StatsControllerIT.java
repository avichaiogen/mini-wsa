package com.akamai.miniwsa.stats;

/**
 * Integration tests for the stats endpoint (GET /v1/stats/summary).
 *
 * Suggested coverage:
 *   - No events → totalEvents 0, all collections empty
 *   - Filter by configId → only matching events counted
 *   - Filter by time range → only events within range counted
 *   - byCategory counts correct for mixed categories
 *   - topAttackers capped at 10 even when more distinct IPs exist
 */
class StatsControllerIT {
}
