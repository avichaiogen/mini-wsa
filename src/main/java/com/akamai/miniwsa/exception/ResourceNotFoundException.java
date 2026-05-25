package com.akamai.miniwsa.exception;

public class ResourceNotFoundException extends MiniWsaException {
    public ResourceNotFoundException(String safeMessage) {
        super(null, safeMessage);
    }
}
