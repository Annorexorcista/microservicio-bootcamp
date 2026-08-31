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

@Component
public class BootcampDtoMapper {

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
