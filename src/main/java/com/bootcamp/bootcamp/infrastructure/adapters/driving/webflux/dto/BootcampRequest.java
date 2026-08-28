package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de solicitud para el registro de un bootcamp en la capa driving (WebFlux).
 *
 * <p>Record inmutable. {@code durationInDays} es {@link Integer} (nulo permitido)
 * para que la validación de duración la realice el dominio, no la deserialización.
 *
 * @param name          nombre del bootcamp.
 * @param description   descripción del bootcamp.
 * @param launchDate    fecha de lanzamiento (formato ISO {@code yyyy-MM-dd}).
 * @param durationInDays duración en días.
 * @param capabilityIds identificadores de las capacidades asociadas.
 */
public record BootcampRequest(
        String name,
        String description,
        LocalDate launchDate,
        Integer durationInDays,
        List<Long> capabilityIds) {
}
