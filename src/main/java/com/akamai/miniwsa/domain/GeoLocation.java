package com.akamai.miniwsa.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GeoLocation {

    @NotBlank @Size(max = 100)
    private String country;

    @NotBlank @Size(max = 100)
    private String city;
}
