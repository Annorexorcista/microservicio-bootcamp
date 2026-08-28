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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Feature: registrar-bootcamp, Property 3 — Validates: Requirements 3.1, 3.2
 *
 * <p><b>Property 3: Descripción inválida se rechaza sin persistir.</b>
 * Para toda descripción nula/vacía/solo espacios, o de longitud > 90 tras trim,
 * {@code registerBootcamp} emite {@link InvalidBootcampDataException} con el
 * código correspondiente y no invoca {@code save}.
 */
class BootcampProperty3Test {

    @Property(tries = 200)
    void blankDescriptionRejected(@ForAll("blankDescriptions") String description) {
        assertRejected(description, DomainErrorCode.DESCRIPTION_REQUIRED);
    }

    @Property(tries = 200)
    void tooLongDescriptionRejected(@ForAll("tooLongDescriptions") String description) {
        assertRejected(description, DomainErrorCode.DESCRIPTION_TOO_LONG);
    }

    private void assertRejected(String description, DomainErrorCode expected) {
        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);
        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);

        // Nombre válido para aislar la validación de descripción.
        Bootcamp input = new Bootcamp(null, "Backend", description,
                LocalDate.of(2026, 3, 1), 30, List.of(1L, 2L));

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
    }

    @Provide
    Arbitrary<String> blankDescriptions() {
        return Arbitraries.oneOf(
                Arbitraries.just((String) null),
                Arbitraries.just(""),
                Arbitraries.strings().withChars(' ', '\t', '\n').ofMinLength(1).ofMaxLength(6));
    }

    @Provide
    Arbitrary<String> tooLongDescriptions() {
        return Arbitraries.strings().withChars('a', 'b', 'c').ofMinLength(91).ofMaxLength(160);
    }
}
