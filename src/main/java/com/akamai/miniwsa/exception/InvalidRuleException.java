package com.akamai.miniwsa.exception;

public class InvalidRuleException extends MiniWsaException {
    public InvalidRuleException(String field, String safeMessage) {
        super(field, safeMessage);
    }
}
