package com.akamai.miniwsa.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpMethodValidatorTest {

    private final HttpMethodValidator v = new HttpMethodValidator();

    // --- Happy paths ---

    @Test void isValid_get_returnsTrue()     { assertThat(v.isValid("GET",     null)).isTrue(); }
    @Test void isValid_post_returnsTrue()    { assertThat(v.isValid("POST",    null)).isTrue(); }
    @Test void isValid_put_returnsTrue()     { assertThat(v.isValid("PUT",     null)).isTrue(); }
    @Test void isValid_delete_returnsTrue()  { assertThat(v.isValid("DELETE",  null)).isTrue(); }
    @Test void isValid_patch_returnsTrue()   { assertThat(v.isValid("PATCH",   null)).isTrue(); }
    @Test void isValid_head_returnsTrue()    { assertThat(v.isValid("HEAD",    null)).isTrue(); }
    @Test void isValid_options_returnsTrue() { assertThat(v.isValid("OPTIONS", null)).isTrue(); }
    @Test void isValid_lowercase_returnsTrue()  { assertThat(v.isValid("get",  null)).isTrue(); }
    @Test void isValid_mixedCase_returnsTrue()  { assertThat(v.isValid("Post", null)).isTrue(); }
    @Test void isValid_null_returnsTrue()       { assertThat(v.isValid(null,   null)).isTrue(); }

    // --- Unhappy paths ---

    @Test void isValid_hack_returnsFalse()      { assertThat(v.isValid("HACK",     null)).isFalse(); }
    @Test void isValid_foobar_returnsFalse()    { assertThat(v.isValid("FOOBAR",   null)).isFalse(); }
    @Test void isValid_scriptTag_returnsFalse() { assertThat(v.isValid("<script>", null)).isFalse(); }
    @Test void isValid_empty_returnsFalse()     { assertThat(v.isValid("",         null)).isFalse(); }
    @Test void isValid_connect_returnsFalse()   { assertThat(v.isValid("CONNECT",  null)).isFalse(); }
}
