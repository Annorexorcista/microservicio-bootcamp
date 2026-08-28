package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.exception.DomainErrorCode;
import com.bootcamp.bootcamp.domain.exception.InvalidBootcampDataException;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Feature: registrar-bootcamp, Property 5 — Validates: Requirements 5.1, 5.2
 *
 * <p><b>Property 5: Cantidad de capacidades fuera de rango se rechaza sin persistir.</b>
 * Para todo conjunto de capacidades con menos de 1 o más de 4 ids distintos,
 * {@code registerBootcamp} emite {@link InvalidBootcampDataException} y no invoca
 * {@code save} ni el gateway.
 */
class BootcampProperty5Test {

    @Property(tries = 100)
    void emptyCapabilitiesRejected() {
        assertRejected(List.of(), DomainErrorCode.CAPABILITIES_TOO_FEW);
    }

    @Property(tries = 200)
    void tooManyCapabilitiesRejected(@ForAll("tooManyDistinctIds") List<Long> ids) {
        assertRejected(ids, DomainErrorCode.CAPABILITIES_TOO_MANY);
    }

    private void assertRejected(List<Long> ids, DomainErrorCode expected) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);
        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);

        Bootcamp input = new Bootcamp(null, "Backend", "descripción válida",
                LocalDate.of(2026, 3, 1), 30, ids);

        Throwable error = null;
        try {
            useCase.registerBootcamp(input).block();
        } catch (Throwable t) {
            error = t;
        }
        if (!(error instanceof InvalidBootcampDataException ibe) || ibe.getCode() != expected) {
            throw new AssertionError("esperado " + expected + ", obtenido=" + error);
        }
        verify(persistencePort, never()).save(any());
        verify(gatewayPort, never()).findExistingCapabilityIds(anyCollection());
    }

    @Provide
    Arbitrary<List<Long>> tooManyDistinctIds() {
        // 5 o más ids distintos.
        return Arbitraries.longs().between(1L, 1_000_000L)
                .list().uniqueElements().ofMinSize(5).ofMaxSize(12);
    }
}
