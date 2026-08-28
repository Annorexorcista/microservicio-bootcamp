package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper puro (sin I/O ni tipos reactivos) que convierte entre los DTOs de la
 * capa driving (WebFlux) y el modelo de dominio {@link Bootcamp}.
 *
 * <p>Las conversiones son transformaciones en memoria; se invocan dentro del
 * pipeline reactivo del handler (por ejemplo con {@code map}), por lo que este
 * componente no conoce Project Reactor ni detalles de HTTP. El modelo de dominio
 * permanece libre de anotaciones de framework.
 */
@Component
public class BootcampDtoMapper {

    /**
     * Convierte un DTO de solicitud en modelo de dominio.
     *
     * <p>El {@code id} se fija en {@code null} porque el bootcamp aún no ha sido
     * persistido. Un {@code durationInDays} nulo se traduce a {@code 0}, valor que
     * el dominio rechaza con {@code DURATION_INVALID}; así la validación de la
     * duración vive en el dominio y no en la deserialización. La normalización
     * (trim) y el resto de validaciones también se realizan en el dominio.
     *
     * @param request DTO recibido en la solicitud.
     * @return el modelo de dominio equivalente con {@code id} nulo.
     */
    public Bootcamp toDomain(BootcampRequest request) {
        int duration = request.durationInDays() == null ? 0 : request.durationInDays();
        return new Bootcamp(
                null,
                request.name(),
                request.description(),
                request.launchDate(),
                duration,
                request.capabilityIds());
    }

    /**
     * Convierte un modelo de dominio ya persistido en DTO de respuesta.
     *
     * @param bootcamp modelo de dominio a convertir.
     * @return el DTO de respuesta equivalente.
     */
    public BootcampResponse toResponse(Bootcamp bootcamp) {
        return new BootcampResponse(
                bootcamp.getId(),
                bootcamp.getName(),
                bootcamp.getDescription(),
                bootcamp.getLaunchDate(),
                bootcamp.getDurationInDays(),
                bootcamp.getCapabilityIds());
    }
}
