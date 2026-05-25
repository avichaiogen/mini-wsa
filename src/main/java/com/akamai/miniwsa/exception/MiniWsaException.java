package com.akamai.miniwsa.exception;

public abstract class MiniWsaException extends RuntimeException {

    private final String field;
    private final String safeMessage;

    protected MiniWsaException(String field, String safeMessage) {
        super(safeMessage);
        this.field = field;
        this.safeMessage = safeMessage;
    }

    protected MiniWsaException(String field, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.field = field;
        this.safeMessage = safeMessage;
    }

    public String getField() { return field; }
    public String getSafeMessage() { return safeMessage; }
}
