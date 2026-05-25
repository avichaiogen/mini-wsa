package com.akamai.miniwsa.repository;

/**
 * Integration tests for SecurityEventRepository.
 *
 * Suggested coverage:
 *   - save and findById round-trip (all fields persisted correctly)
 *   - embedded Rule and GeoLocation fields survive persistence
 *   - enum columns stored as strings (Severity, RuleCategory, Action)
 *   - CHECK constraints enforced (threatScore 0–100, non-negative sizes)
 */
class SecurityEventRepositoryIT {
}
