package com.akamai.miniwsa.exception;

public class InvalidActionException extends MiniWsaException {
    public InvalidActionException(String field, String safeMessage) {
        super(field, safeMessage);
    }
}
