package com.bootcamp.bootcamp.domain.usecase.property;

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
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: listar-bootcamps, Property 5 — Validates: Requirements 5.1, 5.2, 5.6
 *
 * <p>El gateway se invoca a lo sumo una vez: cero si la página es vacía, y
 * exactamente una vez con el conjunto de capabilityId distintos de la página.
 */
class BootcampListProperty5Test {

    @Property(tries = 200)
    void gatewayCalledAtMostOnceWithDistinctIds(@ForAll("pages") List<List<Long>> bootcampCapIds) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        List<Bootcamp> page = new ArrayList<>();
        long id = 1;
        for (List<Long> capIds : bootcampCapIds) {
            page.add(new Bootcamp(id++, "b", "d", LocalDate.of(2026, 3, 1), 30, new ArrayList<>(capIds)));
        }
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just((long) page.size()));
        when(gatewayPort.findCapabilitiesByIds(anyCollection())).thenReturn(Flux.empty());

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(0, 100,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        useCase.listBootcamps(q).block();

        if (page.isEmpty()) {
            verify(gatewayPort, never()).findCapabilitiesByIds(anyCollection());
            return;
        }
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(gatewayPort, times(1)).findCapabilitiesByIds(captor.capture());

        Set<Long> expected = new LinkedHashSet<>();
        bootcampCapIds.forEach(expected::addAll);
        Set<Long> actual = new LinkedHashSet<>(captor.getValue());
        if (actual.size() != captor.getValue().size() || !actual.equals(expected)) {
            throw new AssertionError("ids distintos esperados=" + expected + " obtenidos=" + captor.getValue());
        }
    }

    @Provide
    Arbitrary<List<List<Long>>> pages() {
        Arbitrary<List<Long>> capIdLists = Arbitraries.longs().between(1L, 50L)
                .list().uniqueElements().ofMinSize(1).ofMaxSize(4);
        return capIdLists.list().ofMinSize(0).ofMaxSize(10);
    }
}
