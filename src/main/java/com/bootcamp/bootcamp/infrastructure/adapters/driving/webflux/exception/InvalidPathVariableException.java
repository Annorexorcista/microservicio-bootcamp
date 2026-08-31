package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception;

public class InvalidPathVariableException extends RuntimeException {

    private final RequestErrorCode code;

    public InvalidPathVariableException(RequestErrorCode code, Object... arguments) {
        super(code.formatMessage(arguments));
        this.code = code;
    }

    public InvalidPathVariableException(RequestErrorCode code, Throwable cause, Object... arguments) {
        super(code.formatMessage(arguments), cause);
        this.code = code;
    }

    public RequestErrorCode getCode() {
        return code;
    }
}
