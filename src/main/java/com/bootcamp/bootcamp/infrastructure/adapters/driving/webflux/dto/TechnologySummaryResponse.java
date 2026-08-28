package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

/**
 * DTO de respuesta de una tecnología en el listado de bootcamps (id y nombre).
 *
 * @param id   identificador de la tecnología.
 * @param name nombre de la tecnología.
 */
public record TechnologySummaryResponse(Long id, String name) {
}
