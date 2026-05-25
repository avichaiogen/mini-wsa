package com.akamai.miniwsa.ingestion.dto;

import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.validation.NoInjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input DTO for the WAF rule embedded in an incoming DLR.
 * Mirrors Rule (@Embeddable) but carries no JPA annotations.
 * Validated via @Valid cascade from EventRequest.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RuleRequest {

    @NotBlank @Size(max = 50) @NoInjection
    private String id;

    @NotBlank @Size(max = 200) @NoInjection
    private String name;

    @NotBlank @Size(max = 500) @NoInjection
    private String message;

    // Only CRITICAL/HIGH/MEDIUM/LOW accepted — unknown values → 400 via Jackson
    @NotNull
    private Severity severity;

    // Unknown category values → 400 via Jackson (InvalidFormatException → HttpMessageNotReadableException)
    @NotNull
    private RuleCategory category;
}
