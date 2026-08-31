package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.util.List;

public record CapabilitySummaryResponse(
        Long id,
        String name,
        List<TechnologySummaryResponse> technologies) {
}
