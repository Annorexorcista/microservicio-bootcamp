package com.bootcamp.bootcamp.domain.model;

/**
 * Modelo de dominio reducido de una tecnología, compuesto por su identificador y
 * su nombre. Se usa dentro de {@link CapabilitySummary} para el listado de
 * bootcamps enriquecido.
 */
public final class TechnologySummary {

    private final Long id;
    private final String name;

    public TechnologySummary(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
