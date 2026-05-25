package com.akamai.miniwsa.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusCodeValidatorTest {

    private final StatusCodeValidator v = new StatusCodeValidator();

    // --- Happy paths ---

    @Test void isValid_200_returnsTrue()  { assertThat(v.isValid(200,  null)).isTrue(); }
    @Test void isValid_100_returnsTrue()  { assertThat(v.isValid(100,  null)).isTrue(); }
    @Test void isValid_599_returnsTrue()  { assertThat(v.isValid(599,  null)).isTrue(); }
    @Test void isValid_404_returnsTrue()  { assertThat(v.isValid(404,  null)).isTrue(); }
    @Test void isValid_500_returnsTrue()  { assertThat(v.isValid(500,  null)).isTrue(); }
    @Test void isValid_null_returnsTrue() { assertThat(v.isValid(null, null)).isTrue(); }

    // --- Unhappy paths ---

    @Test void isValid_99_returnsFalse()   { assertThat(v.isValid(99,   null)).isFalse(); }
    @Test void isValid_600_returnsFalse()  { assertThat(v.isValid(600,  null)).isFalse(); }
    @Test void isValid_neg1_returnsFalse() { assertThat(v.isValid(-1,   null)).isFalse(); }
    @Test void isValid_0_returnsFalse()    { assertThat(v.isValid(0,    null)).isFalse(); }
    @Test void isValid_9999_returnsFalse() { assertThat(v.isValid(9999, null)).isFalse(); }
}
