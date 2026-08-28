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
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: registrar-bootcamp, Property 1 — Validates: Requirements 1.1, 1.2, 1.3
 *
 * <p><b>Property 1: Registro válido conserva datos normalizados y persiste.</b>
 * Para todo nombre válido (1-50 tras trim), descripción válida (1-90 tras trim),
 * launchDate no nula, durationInDays > 0 y conjunto de 1-4 ids distintos
 * existentes, {@code registerBootcamp} emite un {@link Bootcamp} con
 * name/description normalizados (trim), la misma launchDate/durationInDays y los
 * mismos ids, e invoca {@code save} exactamente una vez.
 */
class BootcampProperty1Test {

    private static final long ASSIGNED_ID = 999L;

    @Property(tries = 200)
    void validRegistrationNormalizesDataAndPersists(
            @ForAll("paddedNames") String rawName,
            @ForAll("paddedDescriptions") String rawDescription,
            @ForAll @IntRange(min = 1, max = 3650) int durationInDays,
            @ForAll("distinctIdSets") List<Long> ids) {

        IBootcampPersistencePort persistencePort = mock(IBootcampPersistencePort.class);
        ICapabilityGatewayPort gatewayPort = mock(ICapabilityGatewayPort.class);

        when(gatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.fromIterable(ids));
        when(persistencePort.save(any(Bootcamp.class))).thenAnswer(invocation -> {
            Bootcamp toSave = invocation.getArgument(0);
            return Mono.just(new Bootcamp(ASSIGNED_ID, toSave.getName(), toSave.getDescription(),
                    toSave.getLaunchDate(), toSave.getDurationInDays(), toSave.getCapabilityIds()));
        });

        BootcampUseCase useCase = new BootcampUseCase(persistencePort, gatewayPort);

        String expectedName = rawName.trim();
        String expectedDescription = rawDescription.trim();
        LocalDate launch = LocalDate.of(2026, 3, 1);

        Bootcamp input = new Bootcamp(null, rawName, rawDescription, launch,
                durationInDays, new ArrayList<>(ids));

        Bootcamp result = useCase.registerBootcamp(input).block();

        if (result == null) {
            throw new AssertionError("resultado nulo para un registro válido");
        }
        if (!expectedName.equals(result.getName())) {
            throw new AssertionError("name no normalizado: '" + result.getName() + "'");
        }
        if (!expectedDescription.equals(result.getDescription())) {
            throw new AssertionError("description no normalizada: '" + result.getDescription() + "'");
        }
        if (!launch.equals(result.getLaunchDate()) || result.getDurationInDays() != durationInDays) {
            throw new AssertionError("launchDate/durationInDays alterados");
        }
        if (!Set.copyOf(ids).equals(Set.copyOf(result.getCapabilityIds()))
                || result.getCapabilityIds().size() != ids.size()) {
            throw new AssertionError("capabilityIds distintos: " + result.getCapabilityIds());
        }

        verify(persistencePort, times(1)).save(any(Bootcamp.class));
    }

    @Provide
    Arbitrary<String> paddedNames() {
        return trimmedTextWithPadding(1, 50);
    }

    @Provide
    Arbitrary<String> paddedDescriptions() {
        return trimmedTextWithPadding(1, 90);
    }

    private Arbitrary<String> trimmedTextWithPadding(int min, int max) {
        Arbitrary<String> core = Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                .ofMinLength(min).ofMaxLength(max);
        Arbitrary<String> leftPad = Arbitraries.strings().withChars(' ').ofMaxLength(3);
        Arbitrary<String> rightPad = Arbitraries.strings().withChars(' ').ofMaxLength(3);
        return Combinators.combine(leftPad, core, rightPad).as((l, c, r) -> l + c + r);
    }

    @Provide
    Arbitrary<List<Long>> distinctIdSets() {
        return Arbitraries.longs().between(1L, 1_000_000L)
                .list().uniqueElements().ofMinSize(1).ofMaxSize(4);
    }
}
