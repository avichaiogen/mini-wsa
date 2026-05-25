package com.akamai.miniwsa.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input DTO for geographic location embedded in an incoming DLR.
 * Both country and city are required.
 * Validated via @Valid cascade from EventRequest.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GeoLocationRequest {

    @NotBlank @Size(max = 100)
    private String country;

    @NotBlank @Size(max = 100)
    private String city;
}
