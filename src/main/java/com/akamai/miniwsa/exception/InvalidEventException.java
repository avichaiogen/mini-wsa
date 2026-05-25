package com.akamai.miniwsa.exception;

public class InvalidEventException extends MiniWsaException {
    public InvalidEventException(String field, String safeMessage) {
        super(field, safeMessage);
    }
}
