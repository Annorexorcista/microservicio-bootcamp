package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: listar-bootcamps, Property 6 — Validates: Requirements 5.3, 5.5
 *
 * <p>Las capacidades de cada {@link BootcampListItem} son la intersección de los
 * capabilityId del bootcamp con los resueltos por el gateway (en orden del
 * bootcamp), cada una con su id, name y tecnologías; los ids no resueltos se omiten.
 */
class BootcampListProperty6Test {

    @Property(tries = 200)
    void enrichmentIsIntersectionWithResolved(
            @ForAll("capIdLists") List<Long> capIds,
            @ForAll("resolvedFraction") int resolvedCount) {

        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        int n = Math.min(resolvedCount, capIds.size());
        List<Long> resolvedIds = capIds.subList(0, n);
        List<CapabilitySummary> resolved = resolvedIds.stream()
                .map(id -> new CapabilitySummary(id, "C" + id,
                        List.of(new TechnologySummary(id * 10, "T" + id)))).toList();

        Bootcamp b = new Bootcamp(1L, "b", "d", LocalDate.of(2026, 3, 1), 30, new ArrayList<>(capIds));
        when(persistencePort.findPage(any())).thenReturn(Flux.just(b));
        when(persistencePort.countAll()).thenReturn(Mono.just(1L));
        when(gatewayPort.findCapabilitiesByIds(anyCollection())).thenReturn(Flux.fromIterable(resolved));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        BootcampPageQuery q = new BootcampPageQuery(0, 100,
                BootcampSortBy.NAME, BootcampSortDirection.ASC);

        PagedResult<BootcampListItem> result = useCase.listBootcamps(q).block();
        if (result == null || result.getContent().size() != 1) {
            throw new AssertionError("resultado inesperado");
        }
        BootcampListItem item = result.getContent().get(0);

        Set<Long> resolvedSet = new LinkedHashSet<>(resolvedIds);
        List<Long> expected = capIds.stream().filter(resolvedSet::contains).toList();
        List<Long> actual = item.getCapabilities().stream().map(CapabilitySummary::getId).toList();
        if (!actual.equals(expected)) {
            throw new AssertionError("intersección esperada=" + expected + " obtenida=" + actual);
        }
        for (CapabilitySummary c : item.getCapabilities()) {
            if (!("C" + c.getId()).equals(c.getName()) || c.getTechnologies().size() != 1) {
                throw new AssertionError("capacidad mal enriquecida id=" + c.getId());
            }
        }
    }

    @Provide
    Arbitrary<List<Long>> capIdLists() {
        return Arbitraries.longs().between(1L, 100L)
                .list().uniqueElements().ofMinSize(1).ofMaxSize(4);
    }

    @Provide
    Arbitrary<Integer> resolvedFraction() {
        return Arbitraries.integers().between(0, 4);
    }
}
