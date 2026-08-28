package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
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
 * Feature: listar-bootcamps, Property 3 — Validates: Requirements 2.1, 2.2, 3.1, 3.2
 *
 * <p>El {@code content} preserva exactamente el orden posicional emitido por
 * {@code findPage}, sin reordenar en memoria.
 */
class BootcampListProperty3Test {

    @Property(tries = 200)
    void listingPreservesDatabaseOrder(@ForAll("idSequences") List<Long> ids) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        List<Bootcamp> page = new ArrayList<>();
        for (Long id : ids) {
            page.add(new Bootcamp(id, "b" + id, "d", LocalDate.of(2026, 3, 1), 30, List.of(1L)));
        }
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just((long) page.size()));
        when(gatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(new CapabilitySummary(1L, "C", List.of())));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(0, 100,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        PagedResult<BootcampListItem> result = useCase.listBootcamps(q).block();
        if (result == null) {
            throw new AssertionError("resultado nulo");
        }
        List<Long> resultIds = result.getContent().stream().map(BootcampListItem::getId).toList();
        if (!resultIds.equals(ids)) {
            throw new AssertionError("orden alterado: esperado=" + ids + " obtenido=" + resultIds);
        }
    }

    @Provide
    Arbitrary<List<Long>> idSequences() {
        return Arbitraries.longs().between(1L, 100_000L)
                .list().uniqueElements().ofMinSize(0).ofMaxSize(50);
    }
}
