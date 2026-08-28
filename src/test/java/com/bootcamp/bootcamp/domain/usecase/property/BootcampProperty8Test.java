package com.bootcamp.bootcamp.domain.usecase.property;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: registrar-bootcamp, Property 8 — Validates: Requirements 2.4, 3.1, 4.1, 5.1, 6.1, 7.2
 *
 * <p><b>Property 8: Invariante de no persistencia ante error.</b>
 * Para toda entrada que falle cualquier validación (nombre, descripción,
 * fecha/duración, cantidad, repetición o existencia), {@code save} no se invoca
 * jamás.
 */
class BootcampProperty8Test {

    private enum FailureKind { BAD_NAME, BAD_DESCRIPTION, NULL_DATE, BAD_DURATION, EMPTY_CAPS, DUP_CAPS, MISSING_CAP }

    @Property(tries = 300)
    void noPersistenceOnAnyError(@ForAll("failures") FailureKind kind) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        // Para el caso MISSING_CAP, el gateway devuelve vacío -> todos faltan.
        when(gatewayPort.findExistingCapabilityIds(any())).thenReturn(Flux.empty());

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);
        Bootcamp input = buildInvalid(kind);

        try {
            useCase.registerBootcamp(input).block();
        } catch (Throwable ignored) {
            // se espera un error de dominio; lo relevante es que save nunca ocurra
        }
        verify(persistencePort, never()).save(any());
    }

    private Bootcamp buildInvalid(FailureKind kind) {
        String name = "Backend";
        String description = "descripción válida";
        LocalDate date = LocalDate.of(2026, 3, 1);
        int duration = 30;
        List<Long> caps = new ArrayList<>(List.of(1L, 2L));

        switch (kind) {
            case BAD_NAME -> name = "   ";
            case BAD_DESCRIPTION -> description = "   ";
            case NULL_DATE -> date = null;
            case BAD_DURATION -> duration = 0;
            case EMPTY_CAPS -> caps = new ArrayList<>();
            case DUP_CAPS -> caps = new ArrayList<>(List.of(1L, 1L));
            case MISSING_CAP -> { /* datos válidos; el gateway vacío provoca faltantes */ }
        }
        return new Bootcamp(null, name, description, date, duration, caps);
    }

    @Provide
    Arbitrary<FailureKind> failures() {
        return Arbitraries.of(FailureKind.class);
    }
}
