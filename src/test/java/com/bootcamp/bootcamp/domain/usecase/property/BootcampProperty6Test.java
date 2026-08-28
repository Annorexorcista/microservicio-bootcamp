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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Feature: registrar-bootcamp, Property 6 — Validates: Requirements 6.1
 *
 * <p><b>Property 6: Capacidades repetidas se rechazan sin persistir.</b>
 * Para todo conjunto que contenga al menos un id repetido, {@code registerBootcamp}
 * emite {@link InvalidBootcampDataException} (capacidades duplicadas) y no invoca
 * {@code save} ni el gateway.
 */
class BootcampProperty6Test {

    @Property(tries = 200)
    void duplicateCapabilitiesRejected(@ForAll("listsWithDuplicate") List<Long> ids) {
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
        if (!(error instanceof InvalidBootcampDataException ibe)
                || ibe.getCode() != DomainErrorCode.CAPABILITIES_DUPLICATED) {
            throw new AssertionError("esperado CAPABILITIES_DUPLICATED, obtenido=" + error);
        }
        verify(persistencePort, never()).save(any());
        verify(gatewayPort, never()).findExistingCapabilityIds(anyCollection());
    }

    /**
     * Genera una lista de 2-4 ids distintos y duplica uno de ellos, de modo que
     * la lista siempre contenga al menos un repetido (y su tamaño total sea 3-5,
     * pero con distintos dentro del rango válido, aislando la regla de duplicados).
     */
    @Provide
    Arbitrary<List<Long>> listsWithDuplicate() {
        return Arbitraries.longs().between(1L, 1000L)
                .list().uniqueElements().ofMinSize(2).ofMaxSize(3)
                .map(distinct -> {
                    List<Long> withDup = new ArrayList<>(distinct);
                    withDup.add(distinct.get(0)); // repite el primero
                    return withDup;
                });
    }
}
