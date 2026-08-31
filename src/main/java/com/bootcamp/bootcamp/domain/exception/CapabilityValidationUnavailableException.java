package com.bootcamp.bootcamp.domain.exception;

public class CapabilityValidationUnavailableException extends RuntimeException {

    public CapabilityValidationUnavailableException(Throwable cause) {
        super("No fue posible validar las capacidades", cause);
    }
}
