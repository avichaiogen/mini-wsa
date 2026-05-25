package com.akamai.miniwsa.domain;

// @Embeddable: no own DB table — columns inlined into security_events, prefixed via @AttributeOverrides.
import jakarta.persistence.Embeddable;
// EnumType.STRING: store enum name ("CRITICAL") not ordinal index (0).
// Using ordinal is dangerous — reordering enum values would silently corrupt all existing rows.
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
// @NotNull used on enum fields because @NotBlank only applies to strings.
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** WAF rule that triggered the security event. Stored inline in security_events (5 columns). */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Rule {

    @NotBlank @Size(max = 50)
    private String id;

    @NotBlank @Size(max = 200)
    private String name;

    @NotBlank @Size(max = 500)
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RuleCategory category;
}
