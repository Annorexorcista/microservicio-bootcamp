package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto;

import java.time.LocalDate;
import java.util.List;

public record BootcampRequest(
        String name,
        String description,
        LocalDate launchDate,
        Integer durationInDays,
        List<Long> capabilityIds) {
}
