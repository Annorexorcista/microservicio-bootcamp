package com.bootcamp.bootcamp.domain.exception;

/**
 * Excepción de dominio lanzada cuando el bootcamp solicitado no existe.
 * El handler global la traduce a 404 Not Found.
 */
public class BootcampNotFoundException extends RuntimeException {

    public BootcampNotFoundException(Long id) {
        super("El bootcamp con id " + id + " no existe");
    }
}
