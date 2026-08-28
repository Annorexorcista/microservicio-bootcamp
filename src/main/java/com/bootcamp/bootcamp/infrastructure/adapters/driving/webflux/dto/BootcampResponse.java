package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para un bootcamp ya persistido en la capa driving (WebFlux).
 *
 * <p>Record inmutable que representa el bootcamp creado, incluyendo su
 * identificador generado y el listado de identificadores de capacidad asociados.
 *
 * @param id            identificador del bootcamp asignado por la base de datos.
 * @param name          nombre del bootcamp.
 * @param description   descripción del bootcamp.
 * @param launchDate    fecha de lanzamiento.
 * @param durationInDays duración en días.
 * @param capabilityIds identificadores de las capacidades asociadas.
 */
public record BootcampResponse(
        Long id,
        String name,
        String description,
        LocalDate launchDate,
        int durationInDays,
        List<Long> capabilityIds) {
}
