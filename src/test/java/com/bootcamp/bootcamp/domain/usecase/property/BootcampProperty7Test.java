package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.exception.CapabilitiesNotFoundException;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: registrar-bootcamp, Property 7 — Validates: Requirements 7.2
 *
 * <p><b>Property 7: Una capacidad inexistente rechaza el registro sin persistir.</b>
 * Para todo conjunto válido (1-4 distintos) en el que el gateway devuelve un
 * subconjunto estricto de los ids solicitados, {@code registerBootcamp} emite
 * {@link CapabilitiesNotFoundException} con los ids faltantes y no invoca
 * {@code save}.
 */
class BootcampProperty7Test {

    @Property(tries = 200)
    void missingCapabilityRejected(@ForAll("distinctIdSetsMin2") List<Long> ids) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        // El gateway devuelve todos menos el último -> hay al menos un faltante.
        List<Long> existing = ids.subList(0, ids.size() - 1);
        List<Long> expectedMissing = new ArrayList<>(ids.subList(ids.size() - 1, ids.size()));
        when(gatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.fromIterable(existing));

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        Bootcamp input = new Bootcamp(null, "Backend", "descripción válida",
                LocalDate.of(2026, 3, 1), 30, new ArrayList<>(ids));

        Throwable error = null;
        try {
            useCase.registerBootcamp(input).block();
        } catch (Throwable t) {
            error = t;
        }
        if (!(error instanceof CapabilitiesNotFoundException cnf)) {
            throw new AssertionError("esperado CapabilitiesNotFoundException, obtenido=" + error);
        }
        if (!cnf.getMissingIds().equals(expectedMissing)) {
            throw new AssertionError("missing esperado=" + expectedMissing
                    + " obtenido=" + cnf.getMissingIds());
        }
        verify(persistencePort, never()).save(any());
    }

    @Provide
    Arbitrary<List<Long>> distinctIdSetsMin2() {
        return Arbitraries.longs().between(1L, 1_000_000L)
                .list().uniqueElements().ofMinSize(2).ofMaxSize(4);
    }
}
