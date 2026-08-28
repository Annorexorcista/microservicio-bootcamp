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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: listar-bootcamps, Property 1 — Validates: Requirements 1.1, 4.6
 *
 * <p>Un listado válido invoca {@code findPage} con los mismos page/size/sortBy/
 * direction del query, y el {@code content} no excede {@code size}.
 */
class BootcampListProperty1Test {

    @Property(tries = 200)
    void validQueryUsesSameParametersAndRespectsSize(
            @ForAll @IntRange(min = 0, max = 10_000) int page,
            @ForAll @IntRange(min = 1, max = 100) int size,
            @ForAll("pageContents") List<Bootcamp> content) {

        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        List<Bootcamp> limited = content.size() > size ? content.subList(0, size) : content;
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(limited));
        when(persistencePort.countAll()).thenReturn(Mono.just((long) limited.size()));
        when(gatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(new CapabilitySummary(1L, "C", List.of())));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(page, size,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        PagedResult<?> result = useCase.listBootcamps(q).block();

        if (result == null || result.getContent().size() > size) {
            throw new AssertionError("content excede size o resultado nulo");
        }
        ArgumentCaptor<BootcampPageQuery> captor = ArgumentCaptor.forClass(BootcampPageQuery.class);
        verify(persistencePort).findPage(captor.capture());
        BootcampPageQuery used = captor.getValue();
        if (used.getPage() != page || used.getSize() != size
                || used.getSortBy() != BootcampSortBy.NAME
                || used.getDirection() != BootcampSortDirection.ASC) {
            throw new AssertionError("findPage no recibió los parámetros del query");
        }
    }

    @Provide
    Arbitrary<List<Bootcamp>> pageContents() {
        Arbitrary<Bootcamp> bootcamp = Arbitraries.longs().between(1L, 100_000L)
                .map(id -> new Bootcamp(id, "b" + id, "d",
                        LocalDate.of(2026, 3, 1), 30, List.of(1L)));
        return bootcamp.list().ofMinSize(0).ofMaxSize(100);
    }
}
