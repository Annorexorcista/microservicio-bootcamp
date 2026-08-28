package com.bootcamp.bootcamp.domain.exception;

/**
 * Excepción de dominio lanzada cuando los parámetros de paginación u ordenamiento
 * del listado de bootcamps no son válidos (page/size fuera de rango, o
 * sortBy/sortDirection con un valor no permitido).
 *
 * <p>Es una excepción pura, sin dependencias de HTTP; porta un
 * {@link PageErrorCode} que el handler global traduce al código de estado
 * correspondiente (400 Bad Request).
 */
public class InvalidPageQueryException extends RuntimeException {

    private final PageErrorCode code;

    public InvalidPageQueryException(PageErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public PageErrorCode getCode() {
        return code;
    }
}
