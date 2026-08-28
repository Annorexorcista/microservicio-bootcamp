package com.bootcamp.bootcamp.domain.exception;

/**
 * Excepción de dominio lanzada cuando no fue posible validar la existencia de
 * las capacidades porque el Capability_Service no está disponible o respondió
 * con un error.
 *
 * <p>Es una excepción pura, sin dependencias de HTTP; el handler global la
 * traduce al código de estado correspondiente (502 Bad Gateway).
 */
public class CapabilityValidationUnavailableException extends RuntimeException {

    public CapabilityValidationUnavailableException(Throwable cause) {
        super("No fue posible validar las capacidades", cause);
    }
}
