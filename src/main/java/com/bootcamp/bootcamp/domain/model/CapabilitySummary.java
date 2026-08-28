package com.bootcamp.bootcamp.domain.model;

import java.util.List;

/**
 * Modelo de dominio reducido de una capacidad para el listado de bootcamps:
 * identificador, nombre y su listado de tecnologías (id y nombre).
 *
 * <p>Se obtiene del Capability_Service mediante la consulta por ids, que ya
 * devuelve cada capacidad con sus tecnologías resueltas.
 */
public final class CapabilitySummary {

    private final Long id;
    private final String name;
    private final List<TechnologySummary> technologies;

    public CapabilitySummary(Long id, String name, List<TechnologySummary> technologies) {
        this.id = id;
        this.name = name;
        this.technologies = List.copyOf(technologies);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TechnologySummary> getTechnologies() {
        return technologies;
    }
}
