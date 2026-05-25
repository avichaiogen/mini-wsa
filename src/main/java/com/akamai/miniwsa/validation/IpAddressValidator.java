package com.akamai.miniwsa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    private static final Pattern IPV4 = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\." +
        "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank handles this
        if (IPV4.matcher(value).matches()) return true;
        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }
}
