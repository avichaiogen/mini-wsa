-- V1: initial schema for security_events.
--
-- rule_* and geo_* columns are JPA @Embeddable fields inlined from Rule and GeoLocation.
-- attack_type and threat_score are enrichment fields populated at ingestion time (Phase 4).
-- received_at is the server-side ingestion timestamp, never sourced from the client.
--
-- CHECK constraints enforce OWASP numeric bounds at the DB layer as a second line of defence
-- (Bean Validation in the service layer is the first).
CREATE TABLE security_events (
    id            BIGSERIAL       PRIMARY KEY,
    event_id      VARCHAR(100)    NOT NULL,
    timestamp     TIMESTAMPTZ     NOT NULL,
    config_id     BIGINT          NOT NULL,
    policy_id     VARCHAR(100)    NOT NULL,
    client_ip     VARCHAR(45)     NOT NULL,
    hostname      VARCHAR(255)    NOT NULL,
    path          VARCHAR(2048)   NOT NULL,
    method        VARCHAR(10)     NOT NULL,
    status_code   INTEGER         NOT NULL,
    user_agent    VARCHAR(512)    NOT NULL,
    rule_id       VARCHAR(50)     NOT NULL,
    rule_name     VARCHAR(200)    NOT NULL,
    rule_message  VARCHAR(500)    NOT NULL,
    rule_severity VARCHAR(20)     NOT NULL,
    rule_category VARCHAR(30)     NOT NULL,
    action        VARCHAR(10)     NOT NULL,
    geo_country   VARCHAR(100)    NOT NULL,
    geo_city      VARCHAR(100)    NOT NULL,
    request_size  INTEGER         NOT NULL,
    response_size INTEGER         NOT NULL,
    attack_type   VARCHAR(100)    NOT NULL,
    threat_score  INTEGER         NOT NULL,
    received_at   TIMESTAMPTZ     NOT NULL,

    CONSTRAINT chk_request_size  CHECK (request_size  >= 0),
    CONSTRAINT chk_response_size CHECK (response_size >= 0),
    CONSTRAINT chk_threat_score  CHECK (threat_score  BETWEEN 0 AND 100)
);

-- repeat-offender window query: WHERE client_ip = ? AND received_at >= ?
CREATE INDEX idx_se_client_ip_received_at ON security_events (client_ip, received_at DESC);

-- stats aggregation: WHERE config_id = ? AND received_at BETWEEN ? AND ?
CREATE INDEX idx_se_config_id_received_at ON security_events (config_id, received_at DESC);

-- samples API sort: ORDER BY timestamp DESC
CREATE INDEX idx_se_timestamp ON security_events (timestamp DESC);
