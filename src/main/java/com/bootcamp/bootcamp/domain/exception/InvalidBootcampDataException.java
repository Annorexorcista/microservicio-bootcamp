package com.bootcamp.bootcamp.domain.exception;

public class InvalidBootcampDataException extends RuntimeException {

    private final DomainErrorCode code;

    public InvalidBootcampDataException(DomainErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public DomainErrorCode getCode() {
        return code;
    }
}
