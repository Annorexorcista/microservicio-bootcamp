package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: listar-bootcamps, Property 2 — Validates: Requirements 1.2, 1.4
 *
 * <p>{@code totalPages} es el techo de {@code totalElements/size}; una página
 * fuera de rango deja {@code content} vacío conservando la metadata.
 */
class BootcampListProperty2Test {

    @Property(tries = 200)
    void paginationMetadataInvariant(
            @ForAll @LongRange(min = 0, max = 100_000) long totalElements,
            @ForAll @IntRange(min = 1, max = 100) int size,
            @ForAll @IntRange(min = 0, max = 10_000) int page) {

        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        long offset = (long) page * size;
        boolean outOfRange = offset >= totalElements;
        List<Bootcamp> pageContent = new ArrayList<>();
        if (!outOfRange) {
            long remaining = totalElements - offset;
            int count = (int) Math.min(size, remaining);
            for (int i = 0; i < count; i++) {
                pageContent.add(new Bootcamp((long) (offset + i), "b", "d",
                        LocalDate.of(2026, 3, 1), 30, List.of(1L)));
            }
        }

        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(pageContent));
        when(persistencePort.countAll()).thenReturn(Mono.just(totalElements));
        when(gatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(new CapabilitySummary(1L, "C", List.of())));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(page, size,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        PagedResult<?> result = useCase.listBootcamps(q).block();
        if (result == null) {
            throw new AssertionError("resultado nulo");
        }
        int expectedTotalPages = (int) Math.ceil((double) totalElements / size);
        if (result.getTotalPages() != expectedTotalPages
                || result.getTotalElements() != totalElements) {
            throw new AssertionError("metadata incorrecta");
        }
        if (outOfRange && !result.getContent().isEmpty()) {
            throw new AssertionError("página fuera de rango debe tener content vacío");
        }
    }
}
