package com.akamai.miniwsa.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressValidatorTest {

    private final IpAddressValidator v = new IpAddressValidator();

    // --- Happy paths (IPv4) ---

    @Test void isValid_normalIpv4_returnsTrue()  { assertThat(v.isValid("192.168.1.1",     null)).isTrue(); }
    @Test void isValid_zeroIpv4_returnsTrue()    { assertThat(v.isValid("0.0.0.0",         null)).isTrue(); }
    @Test void isValid_maxIpv4_returnsTrue()     { assertThat(v.isValid("255.255.255.255",  null)).isTrue(); }

    // --- Happy paths (IPv6) ---

    @Test void isValid_ipv6Loopback_returnsTrue() { assertThat(v.isValid("::1",         null)).isTrue(); }
    @Test void isValid_ipv6Full_returnsTrue()      { assertThat(v.isValid("2001:db8::1", null)).isTrue(); }

    // --- Happy paths (edge) ---

    @Test void isValid_null_returnsTrue()  { assertThat(v.isValid(null, null)).isTrue(); }
    @Test void isValid_blank_returnsTrue() { assertThat(v.isValid("",   null)).isTrue(); }

    // --- Unhappy paths ---

    @Test void isValid_octetOver255_returnsFalse()      { assertThat(v.isValid("999.999.999.999", null)).isFalse(); }
    @Test void isValid_firstOctetOver255_returnsFalse() { assertThat(v.isValid("256.0.0.1",       null)).isFalse(); }
    @Test void isValid_onlyThreeOctets_returnsFalse()   { assertThat(v.isValid("192.168.1",       null)).isFalse(); }
    @Test void isValid_hostname_returnsFalse()           { assertThat(v.isValid("example.com",     null)).isFalse(); }
    @Test void isValid_randomText_returnsFalse()         { assertThat(v.isValid("not-an-ip",       null)).isFalse(); }
    // Hostnames that happen to contain ':' (e.g., host:port) must not be accepted via DNS resolution
    @Test void isValid_hostColonPort_returnsFalse()      { assertThat(v.isValid("example.com:80",  null)).isFalse(); }
}
