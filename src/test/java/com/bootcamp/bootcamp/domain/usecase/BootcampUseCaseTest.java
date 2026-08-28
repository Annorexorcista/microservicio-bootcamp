package com.bootcamp.bootcamp.domain.usecase;

import com.bootcamp.bootcamp.domain.exception.CapabilitiesNotFoundException;
import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.exception.DomainErrorCode;
import com.bootcamp.bootcamp.domain.exception.InvalidBootcampDataException;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del caso de uso {@link BootcampUseCase}.
 *
 * <p>Mockea ambos puertos SPI ({@link IBootcampPersistencePort} e
 * {@link ICapabilityGatewayPort}) y verifica los flujos reactivos con
 * {@link StepVerifier}. Cubre ejemplos y bordes de todas las reglas de negocio
 * (Req 1-7) y asegura que ante cualquier rechazo {@code save} nunca se invoca.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BootcampUseCaseTest {

    private static final LocalDate LAUNCH = LocalDate.of(2026, 3, 1);
    private static final int DURATION = 84;

    @Mock
    private IBootcampPersistencePort persistencePort;

    @Mock
    private ICapabilityGatewayPort capabilityGatewayPort;

    private BootcampUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BootcampUseCase(persistencePort, capabilityGatewayPort);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static List<Long> ids(long... values) {
        return LongStream.of(values).boxed().collect(Collectors.toList());
    }

    private static List<Long> idRange(int count) {
        return LongStream.rangeClosed(1, count).boxed().collect(Collectors.toList());
    }

    private static String repeat(char c, int times) {
        return Stream.generate(() -> String.valueOf(c)).limit(times).collect(Collectors.joining());
    }

    private static Bootcamp input(String name, String description,
                                  LocalDate launchDate, int durationInDays, List<Long> capabilityIds) {
        return new Bootcamp(null, name, description, launchDate, durationInDays, capabilityIds);
    }

    /** Stubs para el camino feliz: todas las capacidades existen. */
    private void stubHappyPath(List<Long> existingIds) {
        when(capabilityGatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.fromIterable(existingIds));
        when(persistencePort.save(any(Bootcamp.class)))
                .thenAnswer(invocation -> {
                    Bootcamp b = invocation.getArgument(0);
                    return Mono.just(new Bootcamp(99L, b.getName(), b.getDescription(),
                            b.getLaunchDate(), b.getDurationInDays(), b.getCapabilityIds()));
                });
    }

    // ---------------------------------------------------------------------
    // Requirement 1 / 7.3 - Registro válido
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Registro válido: emite bootcamp persistido y llama save exactamente una vez")
    void validRegistration_persistsAndReturnsBootcamp() {
        List<Long> capIds = idRange(3);
        stubHappyPath(capIds);

        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend 2026", "Bootcamp backend", LAUNCH, DURATION, capIds)))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo(99L);
                    assertThat(saved.getName()).isEqualTo("Backend 2026");
                    assertThat(saved.getDescription()).isEqualTo("Bootcamp backend");
                    assertThat(saved.getLaunchDate()).isEqualTo(LAUNCH);
                    assertThat(saved.getDurationInDays()).isEqualTo(DURATION);
                    assertThat(saved.getCapabilityIds()).containsExactlyElementsOf(capIds);
                })
                .verifyComplete();

        verify(persistencePort, times(1)).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("Registro válido: nombre y descripción se normalizan con trim")
    void validRegistration_trimsNameAndDescription() {
        List<Long> capIds = idRange(2);
        stubHappyPath(capIds);

        StepVerifier.create(useCase.registerBootcamp(
                        input("  Backend  ", "  Desc  ", LAUNCH, DURATION, capIds)))
                .assertNext(saved -> {
                    assertThat(saved.getName()).isEqualTo("Backend");
                    assertThat(saved.getDescription()).isEqualTo("Desc");
                })
                .verifyComplete();

        ArgumentCaptor<Bootcamp> captor = ArgumentCaptor.forClass(Bootcamp.class);
        verify(persistencePort).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("Backend");
    }

    // ---------------------------------------------------------------------
    // Requirement 2 - Nombre
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Nombre nulo -> NAME_REQUIRED y save nunca invocado")
    void nullName_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input(null, "desc", LAUNCH, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(err)
                        .isInstanceOf(InvalidBootcampDataException.class)
                        .extracting(e -> ((InvalidBootcampDataException) e).getCode())
                        .isEqualTo(DomainErrorCode.NAME_REQUIRED))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Nombre en blanco -> NAME_REQUIRED")
    void blankName_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("   ", "desc", LAUNCH, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.NAME_REQUIRED))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Nombre de 50 caracteres -> válido")
    void nameLength50_isValid() {
        List<Long> capIds = idRange(2);
        stubHappyPath(capIds);
        StepVerifier.create(useCase.registerBootcamp(
                        input(repeat('a', 50), "desc", LAUNCH, DURATION, capIds)))
                .assertNext(saved -> assertThat(saved.getName()).hasSize(50))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("Nombre de 51 caracteres -> NAME_TOO_LONG y save nunca invocado")
    void nameLength51_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input(repeat('a', 51), "desc", LAUNCH, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.NAME_TOO_LONG))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Requirement 3 - Descripción
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Descripción nula -> DESCRIPTION_REQUIRED y save nunca invocado")
    void nullDescription_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", null, LAUNCH, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.DESCRIPTION_REQUIRED))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Descripción de 90 caracteres -> válida")
    void descriptionLength90_isValid() {
        List<Long> capIds = idRange(2);
        stubHappyPath(capIds);
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", repeat('d', 90), LAUNCH, DURATION, capIds)))
                .assertNext(saved -> assertThat(saved.getDescription()).hasSize(90))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("Descripción de 91 caracteres -> DESCRIPTION_TOO_LONG y save nunca invocado")
    void descriptionLength91_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", repeat('d', 91), LAUNCH, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.DESCRIPTION_TOO_LONG))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Requirement 4 - Fecha y duración
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Fecha de lanzamiento nula -> LAUNCH_DATE_REQUIRED y save nunca invocado")
    void nullLaunchDate_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", null, DURATION, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.LAUNCH_DATE_REQUIRED))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Duración 0 -> DURATION_INVALID y save nunca invocado")
    void zeroDuration_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, 0, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.DURATION_INVALID))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Duración negativa -> DURATION_INVALID y save nunca invocado")
    void negativeDuration_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, -5, idRange(2))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.DURATION_INVALID))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Duración 1 (límite inferior) -> válida")
    void durationOne_isValid() {
        List<Long> capIds = idRange(1);
        stubHappyPath(capIds);
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, 1, capIds)))
                .assertNext(saved -> assertThat(saved.getDurationInDays()).isEqualTo(1))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }

    // ---------------------------------------------------------------------
    // Requirement 5 - Cantidad de capacidades
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("0 capacidades -> CAPABILITIES_TOO_FEW y save nunca invocado")
    void zeroCapabilities_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, List.of())))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.CAPABILITIES_TOO_FEW))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("1 capacidad (límite inferior) -> válido")
    void oneCapability_isValid() {
        List<Long> capIds = idRange(1);
        stubHappyPath(capIds);
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, capIds)))
                .assertNext(saved -> assertThat(saved.getCapabilityIds()).hasSize(1))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("4 capacidades (límite superior) -> válido")
    void fourCapabilities_isValid() {
        List<Long> capIds = idRange(4);
        stubHappyPath(capIds);
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, capIds)))
                .assertNext(saved -> assertThat(saved.getCapabilityIds()).hasSize(4))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("5 capacidades -> CAPABILITIES_TOO_MANY y save nunca invocado")
    void fiveCapabilities_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, idRange(5))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.CAPABILITIES_TOO_MANY))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Requirement 6 - Capacidades repetidas
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Ids de capacidad duplicados -> CAPABILITIES_DUPLICATED y save nunca invocado")
    void duplicateCapabilityIds_rejected() {
        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, ids(1L, 2L, 2L))))
                .expectErrorSatisfies(err -> assertThat(((InvalidBootcampDataException) err).getCode())
                        .isEqualTo(DomainErrorCode.CAPABILITIES_DUPLICATED))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Requirement 7 - Existencia de capacidades
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Alguna capacidad inexistente -> CapabilitiesNotFoundException y save nunca invocado")
    void missingCapability_rejected() {
        List<Long> requested = idRange(3);
        when(capabilityGatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.just(1L, 2L)); // falta el 3

        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, requested)))
                .expectErrorSatisfies(err -> assertThat(err)
                        .isInstanceOf(CapabilitiesNotFoundException.class)
                        .extracting(e -> ((CapabilitiesNotFoundException) e).getMissingIds())
                        .isEqualTo(List.of(3L)))
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Error del gateway propaga CapabilityValidationUnavailableException y save nunca invocado")
    void gatewayError_propagates() {
        when(capabilityGatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.error(new CapabilityValidationUnavailableException(
                        new RuntimeException("service down"))));

        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, idRange(2))))
                .expectError(CapabilityValidationUnavailableException.class)
                .verify();
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Todas las capacidades existen (en desorden) -> registro válido")
    void allCapabilitiesExistOutOfOrder_isValid() {
        List<Long> requested = idRange(3);
        when(capabilityGatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.just(3L, 1L, 2L));
        when(persistencePort.save(any(Bootcamp.class)))
                .thenAnswer(invocation -> {
                    Bootcamp b = invocation.getArgument(0);
                    return Mono.just(new Bootcamp(1L, b.getName(), b.getDescription(),
                            b.getLaunchDate(), b.getDurationInDays(), b.getCapabilityIds()));
                });

        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, requested)))
                .expectNextCount(1)
                .verifyComplete();
        verify(persistencePort, times(1)).save(any(Bootcamp.class));
    }

    @Test
    @DisplayName("Ids nulos en la lista se descartan; con 1 distinto válido -> registro exitoso")
    void nullIdsAreDiscarded_isValid() {
        List<Long> withNulls = new ArrayList<>(List.of(1L, 2L));
        withNulls.add(null);
        when(capabilityGatewayPort.findExistingCapabilityIds(anyCollection()))
                .thenReturn(Flux.just(1L, 2L));
        when(persistencePort.save(any(Bootcamp.class)))
                .thenAnswer(invocation -> {
                    Bootcamp b = invocation.getArgument(0);
                    return Mono.just(new Bootcamp(1L, b.getName(), b.getDescription(),
                            b.getLaunchDate(), b.getDurationInDays(), b.getCapabilityIds()));
                });

        StepVerifier.create(useCase.registerBootcamp(
                        input("Backend", "desc", LAUNCH, DURATION, withNulls)))
                .assertNext(saved -> assertThat(saved.getCapabilityIds()).containsExactly(1L, 2L))
                .verifyComplete();
        verify(persistencePort).save(any(Bootcamp.class));
    }
}
