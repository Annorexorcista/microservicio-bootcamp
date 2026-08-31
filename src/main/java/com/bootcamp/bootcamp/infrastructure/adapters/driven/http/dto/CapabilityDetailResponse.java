package com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto;

import java.util.List;

public record CapabilityDetailResponse(
        Long id,
        String name,
        String description,
        List<TechnologyItem> technologies) {

    public record TechnologyItem(Long id, String name) {
    }
}
