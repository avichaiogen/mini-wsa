package com.akamai.miniwsa.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoInjectionValidatorTest {

    private final NoInjectionValidator v = new NoInjectionValidator();

    // --- Happy paths ---

    @Test void isValid_normalText_returnsTrue()   { assertThat(v.isValid("hello world",       null)).isTrue(); }
    @Test void isValid_apiPath_returnsTrue()       { assertThat(v.isValid("/api/v1/login",     null)).isTrue(); }
    @Test void isValid_userAgent_returnsTrue()     { assertThat(v.isValid("Mozilla/5.0",       null)).isTrue(); }
    @Test void isValid_cityName_returnsTrue()      { assertThat(v.isValid("New York",          null)).isTrue(); }
    @Test void isValid_sqlmapAgent_returnsTrue()   { assertThat(v.isValid("sqlmap/1.7.8#stable (https://sqlmap.org)", null)).isTrue(); }
    @Test void isValid_null_returnsTrue()          { assertThat(v.isValid(null,                null)).isTrue(); }

    // --- Unhappy paths (one per injection category) ---

    @Test void isValid_nullByte_returnsFalse()         { assertThat(v.isValid("abc\0def",                    null)).isFalse(); }
    @Test void isValid_scriptOpenTag_returnsFalse()    { assertThat(v.isValid("<script>alert(1)</script>",   null)).isFalse(); }
    @Test void isValid_javascriptScheme_returnsFalse() { assertThat(v.isValid("javascript:alert(1)",         null)).isFalse(); }
    @Test void isValid_eventHandler_returnsFalse()     { assertThat(v.isValid("onclick=evil()",              null)).isFalse(); }
    @Test void isValid_pathTraversal_returnsFalse()    { assertThat(v.isValid("../../etc/passwd",            null)).isFalse(); }
    @Test void isValid_unionSelect_returnsFalse()      { assertThat(v.isValid("x UNION SELECT * FROM users", null)).isFalse(); }
    @Test void isValid_dropTable_returnsFalse()        { assertThat(v.isValid("'; DROP TABLE events; --",    null)).isFalse(); }
    @Test void isValid_sqlComment_returnsFalse()       { assertThat(v.isValid("admin'-- ",                   null)).isFalse(); }
}
