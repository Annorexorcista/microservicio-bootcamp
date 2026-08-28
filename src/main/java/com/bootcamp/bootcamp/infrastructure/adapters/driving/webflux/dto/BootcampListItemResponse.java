package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta de un bootcamp dentro del listado paginado, incluyendo el
 * detalle de sus capacidades (id, nombre y sus tecnologías).
 *
 * @param id            identificador del bootcamp.
 * @param name          nombre del bootcamp.
 * @param description   descripción del bootcamp.
 * @param launchDate    fecha de lanzamiento.
 * @param durationInDays duración en días.
 * @param capabilities  capacidades asociadas, cada una con sus tecnologías.
 */
public record BootcampListItemResponse(
        Long id,
        String name,
        String description,
        LocalDate launchDate,
        int durationInDays,
        List<CapabilitySummaryResponse> capabilities) {
}
