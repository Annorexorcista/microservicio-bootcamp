package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.exception.DomainErrorCode;
import com.bootcamp.bootcamp.domain.exception.InvalidBootcampDataException;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Feature: registrar-bootcamp, Property 4 — Validates: Requirements 4.1, 4.2
 *
 * <p><b>Property 4: Fecha o duración inválidas se rechazan sin persistir.</b>
 * Para toda launchDate nula o durationInDays ≤ 0, {@code registerBootcamp} emite
 * {@link InvalidBootcampDataException} con el código correspondiente y no invoca
 * {@code save} ni el gateway.
 */
class BootcampProperty4Test {

    @Property(tries = 100)
    void nullLaunchDateRejected(@ForAll @IntRange(min = 1, max = 3650) int duration) {
        // launchDate nula con duración válida -> LAUNCH_DATE_REQUIRED
        assertRejected(null, duration, DomainErrorCode.LAUNCH_DATE_REQUIRED);
    }

    @Property(tries = 200)
    void nonPositiveDurationRejected(@ForAll @IntRange(min = 0, max = 100_000) int magnitude) {
        int duration = -magnitude; // <= 0
        assertRejected(LocalDate.of(2026, 3, 1), duration, DomainErrorCode.DURATION_INVALID);
    }

    private void assertRejected(LocalDate launchDate, int duration, DomainErrorCode expected) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);
        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);

        Bootcamp input = new Bootcamp(null, "Backend", "descripción válida",
                launchDate, duration, List.of(1L, 2L));

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
}
