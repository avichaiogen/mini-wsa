package com.akamai.miniwsa.exception;

public class InvalidGeoLocationException extends MiniWsaException {
    public InvalidGeoLocationException(String field, String safeMessage) {
        super(field, safeMessage);
    }
}
