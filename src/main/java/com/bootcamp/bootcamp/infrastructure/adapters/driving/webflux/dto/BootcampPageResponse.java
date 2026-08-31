package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.util.List;

public record BootcampPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<BootcampListItemResponse> content) {
}
