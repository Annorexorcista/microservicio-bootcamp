package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.util.List;

/**
 * DTO de respuesta paginada del listado de bootcamps, con la metadata de
 * paginación y el contenido de la página.
 *
 * @param page          número de página (base cero).
 * @param size          tamaño de página solicitado.
 * @param totalElements total de bootcamps existentes.
 * @param totalPages    total de páginas para el tamaño solicitado.
 * @param content       bootcamps de la página, enriquecidos con sus capacidades.
 */
public record BootcampPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<BootcampListItemResponse> content) {
}
