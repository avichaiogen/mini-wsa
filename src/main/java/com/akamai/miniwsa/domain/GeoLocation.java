package com.akamai.miniwsa.domain;

// @Embeddable: no own DB table — columns are inlined into the owning entity's table (security_events).
// Column names are overridden via @AttributeOverrides in SecurityEvent.
import jakarta.persistence.Embeddable;
// Bean Validation: enforces A6 (country and city are required, non-blank).
import jakarta.validation.constraints.NotBlank;
// Caps string length — OWASP: prevents oversized payload attacks at the persistence boundary.
import jakarta.validation.constraints.Size;
// Lombok: generates getters, setters, no-arg constructor (required by JPA), and all-arg constructor
// used in test/service code (e.g. new GeoLocation("CN", "Beijing")).
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Country and city extracted from the incoming DLR. Both fields are required (A6). */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GeoLocation {

    @NotBlank @Size(max = 100)
    private String country;

    @NotBlank @Size(max = 100)
    private String city;
}
