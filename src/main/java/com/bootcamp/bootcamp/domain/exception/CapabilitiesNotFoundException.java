package com.bootcamp.bootcamp.domain.exception;

import java.util.List;

/**
 * Excepción de dominio lanzada cuando al menos una de las capacidades asociadas
 * a un bootcamp no existe en el microservicio de Capacidad.
 *
 * <p>Es una excepción pura, sin dependencias de HTTP; porta la lista de
 * identificadores faltantes, que el handler global traduce al código de estado
 * correspondiente (400 Bad Request) junto con un mensaje descriptivo.
 */
public class CapabilitiesNotFoundException extends RuntimeException {

    private final List<Long> missingIds;

    public CapabilitiesNotFoundException(List<Long> missingIds) {
        super("Las siguientes capacidades no existen: " + missingIds);
        this.missingIds = List.copyOf(missingIds);
    }

    /**
     * @return los identificadores de capacidad que no fueron encontrados.
     */
    public List<Long> getMissingIds() {
        return missingIds;
    }
}
