package com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto;

import java.util.List;

/**
 * DTO de respuesta del Capability_Service para la consulta por identificadores
 * {@code GET /api/v1/capabilities?ids=1,2,3} usada en el listado de bootcamps.
 *
 * <p>El endpoint devuelve cada capacidad con su id, nombre, descripción y su
 * listado de tecnologías (id y nombre) ya resuelto. Esta forma completa es la que
 * el listado de bootcamps necesita para enriquecer el árbol capacidad→tecnologías.
 *
 * @param id           identificador de la capacidad.
 * @param name         nombre de la capacidad.
 * @param description  descripción de la capacidad.
 * @param technologies tecnologías de la capacidad (id y nombre).
 */
public record CapabilityDetailResponse(
        Long id,
        String name,
        String description,
        List<TechnologyItem> technologies) {

    /**
     * Tecnología dentro de la respuesta del Capability_Service (id y nombre).
     */
    public record TechnologyItem(Long id, String name) {
    }
}
