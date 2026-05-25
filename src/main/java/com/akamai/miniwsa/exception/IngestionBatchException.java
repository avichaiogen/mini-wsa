package com.akamai.miniwsa.exception;

public class IngestionBatchException extends MiniWsaException {
    public IngestionBatchException(String safeMessage, Throwable cause) {
        super(null, safeMessage, cause);
    }
}
