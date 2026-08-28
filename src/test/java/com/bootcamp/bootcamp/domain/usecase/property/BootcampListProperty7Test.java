package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampListItemResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampPageResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.CapabilitySummaryResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper.BootcampDtoMapper;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Feature: listar-bootcamps, Property 7 — Validates: Requirements 5.4
 *
 * <p>El {@link BootcampPageResponse} mapeado conserva la metadata, el orden y la
 * cantidad de items, y para cada item su id/name/description/launchDate/
 * durationInDays y su árbol de capacidades→tecnologías (id+name).
 */
class BootcampListProperty7Test {

    private final BootcampDtoMapper mapper = new BootcampDtoMapper();

    @Property(tries = 200)
    void pageResponseKeepsNestedStructure(
            @ForAll @IntRange(min = 0, max = 10_000) int page,
            @ForAll @IntRange(min = 1, max = 100) int size,
            @ForAll @LongRange(min = 0, max = 1_000_000) long totalElements,
            @ForAll("items") List<BootcampListItem> items) {

        PagedResult<BootcampListItem> pagedResult = new PagedResult<>(page, size, totalElements, items);
        BootcampPageResponse response = mapper.toPageResponse(pagedResult);

        if (response.page() != page || response.size() != size
                || response.totalElements() != totalElements
                || response.totalPages() != pagedResult.getTotalPages()
                || response.content().size() != items.size()) {
            throw new AssertionError("metadata o cantidad de items alterada");
        }
        for (int i = 0; i < items.size(); i++) {
            BootcampListItem src = items.get(i);
            BootcampListItemResponse dst = response.content().get(i);
            if (!Objects.equals(src.getId(), dst.id())
                    || !Objects.equals(src.getName(), dst.name())
                    || !Objects.equals(src.getDescription(), dst.description())
                    || !Objects.equals(src.getLaunchDate(), dst.launchDate())
                    || src.getDurationInDays() != dst.durationInDays()
                    || src.getCapabilities().size() != dst.capabilities().size()) {
                throw new AssertionError("campos del item alterados en la posición " + i);
            }
            for (int j = 0; j < src.getCapabilities().size(); j++) {
                CapabilitySummary sc = src.getCapabilities().get(j);
                CapabilitySummaryResponse dc = dst.capabilities().get(j);
                if (!Objects.equals(sc.getId(), dc.id())
                        || !Objects.equals(sc.getName(), dc.name())
                        || sc.getTechnologies().size() != dc.technologies().size()) {
                    throw new AssertionError("capacidad alterada");
                }
            }
        }
    }

    @Provide
    Arbitrary<List<BootcampListItem>> items() {
        Arbitrary<TechnologySummary> tech = Combinators.combine(
                        Arbitraries.longs().between(1L, 1000L),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8))
                .as(TechnologySummary::new);
        Arbitrary<CapabilitySummary> capability = Combinators.combine(
                        Arbitraries.longs().between(1L, 1000L),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                        tech.list().ofMinSize(0).ofMaxSize(3))
                .as(CapabilitySummary::new);
        Arbitrary<BootcampListItem> item = Combinators.combine(
                        Arbitraries.longs().between(1L, 100_000L),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),
                        capability.list().ofMinSize(0).ofMaxSize(4))
                .as((id, name, desc, caps) -> new BootcampListItem(
                        id, name, desc, LocalDate.of(2026, 3, 1), 30, caps));
        return item.list().ofMinSize(0).ofMaxSize(10);
    }
}
