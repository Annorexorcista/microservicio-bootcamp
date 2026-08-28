package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
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
 * Feature: listar-bootcamps, Property 8 — Validates: Requirements 7.1
 *
 * <p>Para toda página no vacía en la que el gateway emite
 * {@link CapabilityValidationUnavailableException}, {@code listBootcamps} termina
 * con ese error sin producir un {@code PagedResult}.
 */
class BootcampListProperty8Test {

    @Property(tries = 200)
    void gatewayUnavailablePropagatesError(@ForAll("nonEmptyPages") List<Long> ids) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        List<Bootcamp> page = new ArrayList<>();
        for (Long id : ids) {
            page.add(new Bootcamp(id, "b" + id, "d", LocalDate.of(2026, 3, 1), 30, List.of(1L, 2L)));
        }
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just((long) page.size()));
        when(gatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.error(new CapabilityValidationUnavailableException(
                        new RuntimeException("down"))));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(0, 100,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        Throwable error = null;
        Object result = null;
        try {
            result = useCase.listBootcamps(q).block();
        } catch (Throwable t) {
            error = t;
        }
        if (result != null) {
            throw new AssertionError("no debió producir un PagedResult");
        }
        if (!(error instanceof CapabilityValidationUnavailableException)) {
            throw new AssertionError("esperado CapabilityValidationUnavailableException, obtenido=" + error);
        }
    }

    @Provide
    Arbitrary<List<Long>> nonEmptyPages() {
        return Arbitraries.longs().between(1L, 100_000L)
                .list().uniqueElements().ofMinSize(1).ofMaxSize(20);
    }
}
