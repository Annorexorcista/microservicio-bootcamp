package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.time.LocalDate;
import java.util.List;

public record BootcampListItemResponse(
        Long id,
        String name,
        String description,
        LocalDate launchDate,
        int durationInDays,
        List<CapabilitySummaryResponse> capabilities) {
}
