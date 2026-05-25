package com.akamai.miniwsa.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
