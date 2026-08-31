package com.bootcamp.bootcamp.domain.exception;

public class BootcampNotFoundException extends RuntimeException {

    public BootcampNotFoundException(Long id) {
        super("El bootcamp con id " + id + " no existe");
    }
}
