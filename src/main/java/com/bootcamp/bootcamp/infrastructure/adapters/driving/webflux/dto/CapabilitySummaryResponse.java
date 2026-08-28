package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.util.List;

/**
 * DTO de respuesta de una capacidad dentro del listado de bootcamps: id, nombre y
 * su listado de tecnologías (id y nombre).
 *
 * @param id           identificador de la capacidad.
 * @param name         nombre de la capacidad.
 * @param technologies tecnologías de la capacidad.
 */
public record CapabilitySummaryResponse(
        Long id,
        String name,
        List<TechnologySummaryResponse> technologies) {
}
