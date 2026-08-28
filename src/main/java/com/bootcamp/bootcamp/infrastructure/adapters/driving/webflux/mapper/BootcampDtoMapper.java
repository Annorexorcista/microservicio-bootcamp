package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper;

import com.bootcamp.bootcamp.domain.exception.InvalidPageQueryException;
import com.bootcamp.bootcamp.domain.exception.PageErrorCode;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampListItemResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampPageResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.CapabilitySummaryResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.TechnologySummaryResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.List;

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

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    /**
     * Construye un {@link BootcampPageQuery} de dominio a partir de los query
     * params de la solicitud, aplicando los defaults (page=0, size=10, sortBy=name,
     * sortDirection=asc) cuando faltan y traduciendo {@code sortBy}/
     * {@code sortDirection} a los enums de dominio contra una lista blanca.
     *
     * <p>Un {@code page}/{@code size} no numérico o un {@code sortBy}/
     * {@code sortDirection} fuera de la lista blanca produce una
     * {@link InvalidPageQueryException} (traducida a 400 por el handler global). El
     * rango de {@code page}/{@code size} lo valida el caso de uso.
     */
    public BootcampPageQuery toPageQuery(ServerRequest request) {
        int page = parseIntParam(request, "page", DEFAULT_PAGE);
        int size = parseIntParam(request, "size", DEFAULT_SIZE);
        BootcampSortBy sortBy = parseSortBy(request.queryParam("sortBy").orElse(null));
        BootcampSortDirection direction =
                parseSortDirection(request.queryParam("sortDirection").orElse(null));
        return new BootcampPageQuery(page, size, sortBy, direction);
    }

    private int parseIntParam(ServerRequest request, String name, int defaultValue) {
        String raw = request.queryParam(name).orElse(null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            PageErrorCode code = "size".equals(name)
                    ? PageErrorCode.SIZE_TOO_SMALL
                    : PageErrorCode.PAGE_NEGATIVE;
            throw new InvalidPageQueryException(code);
        }
    }

    private BootcampSortBy parseSortBy(String raw) {
        if (raw == null || raw.isBlank()) {
            return BootcampSortBy.NAME;
        }
        return switch (raw.trim()) {
            case "name" -> BootcampSortBy.NAME;
            case "capabilityCount" -> BootcampSortBy.CAPABILITY_COUNT;
            default -> throw new InvalidPageQueryException(PageErrorCode.SORT_BY_INVALID);
        };
    }

    private BootcampSortDirection parseSortDirection(String raw) {
        if (raw == null || raw.isBlank()) {
            return BootcampSortDirection.ASC;
        }
        return switch (raw.trim().toLowerCase()) {
            case "asc" -> BootcampSortDirection.ASC;
            case "desc" -> BootcampSortDirection.DESC;
            default -> throw new InvalidPageQueryException(PageErrorCode.SORT_DIRECTION_INVALID);
        };
    }

    /**
     * Convierte el {@link PagedResult} de dominio en el DTO de respuesta paginada,
     * conservando la metadata, el orden y la cantidad de items, y mapeando el árbol
     * capacidades→tecnologías de cada item.
     */
    public BootcampPageResponse toPageResponse(PagedResult<BootcampListItem> pagedResult) {
        List<BootcampListItemResponse> content = pagedResult.getContent().stream()
                .map(this::toListItemResponse)
                .toList();
        return new BootcampPageResponse(
                pagedResult.getPage(),
                pagedResult.getSize(),
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                content);
    }

    private BootcampListItemResponse toListItemResponse(BootcampListItem item) {
        List<CapabilitySummaryResponse> capabilities = item.getCapabilities().stream()
                .map(c -> new CapabilitySummaryResponse(
                        c.getId(), c.getName(),
                        c.getTechnologies().stream()
                                .map(t -> new TechnologySummaryResponse(t.getId(), t.getName()))
                                .toList()))
                .toList();
        return new BootcampListItemResponse(
                item.getId(), item.getName(), item.getDescription(),
                item.getLaunchDate(), item.getDurationInDays(), capabilities);
    }
}
