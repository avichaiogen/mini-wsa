package com.akamai.miniwsa.domain;

// Wildcard covers: @Entity, @Table, @Id, @GeneratedValue, @Column, @Embedded,
// @AttributeOverride(s), @Enumerated, EnumType — all standard JPA mapping annotations.
import jakarta.persistence.*;
// @Valid on embedded fields cascades Bean Validation into Rule and GeoLocation.
// Without it, @NotBlank/@NotNull inside those classes would be silently ignored.
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
// Lombok: @NoArgsConstructor required by JPA spec; @Getter/@Setter replace ~40 boilerplate methods.
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Instant maps to TIMESTAMPTZ in PostgreSQL — always UTC, no timezone ambiguity.
import java.time.Instant;

/**
 * Persisted security event (DLR) enriched with attackType, threatScore, and receivedAt.
 *
 * Rule and GeoLocation are @Embeddable — their fields are stored directly in this table
 * under prefixed column names (rule_id, rule_name, … geo_country, geo_city) via @AttributeOverrides.
 *
 * receivedAt is set server-side at ingestion time, never trusted from the client.
 */
@Entity
@Table(name = "security_events")
@Getter @Setter @NoArgsConstructor
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp;

    @NotNull
    @Column(name = "config_id", nullable = false)
    private Long configId;

    @NotBlank @Size(max = 100)
    @Column(name = "policy_id", nullable = false, length = 100)
    private String policyId;

    @NotBlank @Size(max = 45)
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @NotBlank @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String hostname;

    @NotBlank @Size(max = 2048)
    @Column(nullable = false, length = 2048)
    private String path;

    @NotBlank @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String method;

    @NotNull
    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @NotBlank @Size(max = 512)
    @Column(name = "user_agent", nullable = false, length = 512)
    private String userAgent;

    @Valid @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "id",       column = @Column(name = "rule_id",       nullable = false, length = 50)),
        @AttributeOverride(name = "name",     column = @Column(name = "rule_name",     nullable = false, length = 200)),
        @AttributeOverride(name = "message",  column = @Column(name = "rule_message",  nullable = false, length = 500)),
        @AttributeOverride(name = "severity", column = @Column(name = "rule_severity", nullable = false, length = 20)),
        @AttributeOverride(name = "category", column = @Column(name = "rule_category", nullable = false, length = 30))
    })
    private Rule rule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Action action;

    @Valid @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "country", column = @Column(name = "geo_country", nullable = false, length = 100)),
        @AttributeOverride(name = "city",    column = @Column(name = "geo_city",    nullable = false, length = 100))
    })
    private GeoLocation geoLocation;

    @NotNull @Min(0)
    @Column(name = "request_size", nullable = false)
    private Integer requestSize;

    @NotNull @Min(0)
    @Column(name = "response_size", nullable = false)
    private Integer responseSize;

    @NotBlank @Size(max = 100)
    @Column(name = "attack_type", nullable = false, length = 100)
    private String attackType;

    @NotNull @Min(0)
    @Column(name = "threat_score", nullable = false)
    private Integer threatScore;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
