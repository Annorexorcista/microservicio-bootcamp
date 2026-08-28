package com.bootcamp.bootcamp.domain.usecase;

import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.exception.InvalidPageQueryException;
import com.bootcamp.bootcamp.domain.exception.PageErrorCode;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
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
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del método de listado {@link BootcampUseCase#listBootcamps}.
 *
 * <p>Mockea ambos puertos SPI y verifica los flujos reactivos con
 * {@link StepVerifier}. Cubre límites de page/size, página vacía sin gateway,
 * enriquecimiento con ids faltantes, invariante N+1, preservación de orden y
 * propagación del error del gateway; ante rechazo de validación, ni persistencia
 * ni gateway se invocan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BootcampListUseCaseTest {

    @Mock
    private IBootcampPersistencePort persistencePort;

    @Mock
    private ICapabilityGatewayPort capabilityGatewayPort;

    private BootcampUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BootcampUseCase(persistencePort, capabilityGatewayPort);
    }

    private static BootcampPageQuery query(int page, int size) {
        return new BootcampPageQuery(page, size, BootcampSortBy.NAME, BootcampSortDirection.ASC);
    }

    private static Bootcamp bootcamp(long id, String name, List<Long> capIds) {
        return new Bootcamp(id, name, "desc " + id, LocalDate.of(2026, 3, 1), 30, capIds);
    }

    private static CapabilitySummary cap(long id, String name) {
        return new CapabilitySummary(id, name, List.of(new TechnologySummary(id * 10, "T" + id)));
    }

    // --- validación de rango ---

    @Test
    @DisplayName("page < 0 -> PAGE_NEGATIVE sin consultar persistencia ni gateway")
    void negativePage_rejected() {
        StepVerifier.create(useCase.listBootcamps(query(-1, 10)))
                .expectErrorSatisfies(err -> assertThat(((InvalidPageQueryException) err).getCode())
                        .isEqualTo(PageErrorCode.PAGE_NEGATIVE))
                .verify();
        verify(persistencePort, never()).findPage(any());
        verify(persistencePort, never()).countAll();
        verify(capabilityGatewayPort, never()).findCapabilitiesByIds(anyCollection());
    }

    @Test
    @DisplayName("size < 1 -> SIZE_TOO_SMALL")
    void tooSmallSize_rejected() {
        StepVerifier.create(useCase.listBootcamps(query(0, 0)))
                .expectErrorSatisfies(err -> assertThat(((InvalidPageQueryException) err).getCode())
                        .isEqualTo(PageErrorCode.SIZE_TOO_SMALL))
                .verify();
        verify(persistencePort, never()).findPage(any());
    }

    @Test
    @DisplayName("size > 100 -> SIZE_TOO_LARGE")
    void tooLargeSize_rejected() {
        StepVerifier.create(useCase.listBootcamps(query(0, 101)))
                .expectErrorSatisfies(err -> assertThat(((InvalidPageQueryException) err).getCode())
                        .isEqualTo(PageErrorCode.SIZE_TOO_LARGE))
                .verify();
        verify(persistencePort, never()).findPage(any());
    }

    // --- página vacía sin gateway ---

    @Test
    @DisplayName("Página vacía -> PagedResult vacío con metadata coherente y sin gateway")
    void emptyPage_returnsEmptyResultWithoutGateway() {
        when(persistencePort.findPage(any())).thenReturn(Flux.empty());
        when(persistencePort.countAll()).thenReturn(Mono.just(23L));

        StepVerifier.create(useCase.listBootcamps(query(5, 10)))
                .assertNext(result -> {
                    assertThat(result.getContent()).isEmpty();
                    assertThat(result.getTotalElements()).isEqualTo(23L);
                    assertThat(result.getTotalPages()).isEqualTo(3);
                })
                .verifyComplete();

        verify(capabilityGatewayPort, never()).findCapabilitiesByIds(anyCollection());
    }

    // --- enriquecimiento ---

    @Test
    @DisplayName("Enriquecimiento: una sola llamada al gateway con los ids distintos")
    void enrichment_callsGatewayOnceWithDistinctIds() {
        List<Bootcamp> page = List.of(
                bootcamp(1L, "A", List.of(10L, 11L)),
                bootcamp(2L, "B", List.of(11L, 12L)));
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just(2L));
        when(capabilityGatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(cap(10L, "C10"), cap(11L, "C11"), cap(12L, "C12")));

        StepVerifier.create(useCase.listBootcamps(query(0, 10)))
                .expectNextCount(1)
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(capabilityGatewayPort, times(1)).findCapabilitiesByIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(10L, 11L, 12L);
    }

    @Test
    @DisplayName("Cada bootcamp recibe sus capacidades (con tecnologías) e ignora las no resueltas")
    void enrichment_mapsResolvedCapabilitiesAndOmitsMissing() {
        List<Bootcamp> page = List.of(
                bootcamp(1L, "A", List.of(10L, 11L)),
                bootcamp(2L, "B", List.of(12L, 99L))); // 99 no resuelto
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just(2L));
        when(capabilityGatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(cap(10L, "C10"), cap(11L, "C11"), cap(12L, "C12")));

        StepVerifier.create(useCase.listBootcamps(query(0, 10)))
                .assertNext(result -> {
                    List<BootcampListItem> items = result.getContent();
                    assertThat(items.get(0).getCapabilities())
                            .extracting(CapabilitySummary::getId).containsExactly(10L, 11L);
                    assertThat(items.get(0).getCapabilities().get(0).getTechnologies())
                            .extracting(TechnologySummary::getName).containsExactly("T10");
                    assertThat(items.get(1).getCapabilities())
                            .extracting(CapabilitySummary::getId).containsExactly(12L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("El listado preserva el orden emitido por findPage")
    void listing_preservesFindPageOrder() {
        List<Bootcamp> page = List.of(
                bootcamp(3L, "C", List.of(10L)),
                bootcamp(1L, "A", List.of(10L)),
                bootcamp(2L, "B", List.of(10L)));
        when(persistencePort.findPage(any())).thenReturn(Flux.fromIterable(page));
        when(persistencePort.countAll()).thenReturn(Mono.just(3L));
        when(capabilityGatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.just(cap(10L, "C10")));

        StepVerifier.create(useCase.listBootcamps(query(0, 10)))
                .assertNext(result -> assertThat(result.getContent())
                        .extracting(BootcampListItem::getId).containsExactly(3L, 1L, 2L))
                .verifyComplete();
    }

    @Test
    @DisplayName("findPage recibe los mismos parámetros del query")
    void findPage_receivesQueryParameters() {
        BootcampPageQuery q = new BootcampPageQuery(2, 25,
                BootcampSortBy.CAPABILITY_COUNT, BootcampSortDirection.DESC);
        when(persistencePort.findPage(any())).thenReturn(Flux.empty());
        when(persistencePort.countAll()).thenReturn(Mono.just(0L));

        StepVerifier.create(useCase.listBootcamps(q)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<BootcampPageQuery> captor = ArgumentCaptor.forClass(BootcampPageQuery.class);
        verify(persistencePort).findPage(captor.capture());
        assertThat(captor.getValue().getSortBy()).isEqualTo(BootcampSortBy.CAPABILITY_COUNT);
        assertThat(captor.getValue().getDirection()).isEqualTo(BootcampSortDirection.DESC);
    }

    // --- error del gateway ---

    @Test
    @DisplayName("Error del gateway durante el enriquecimiento propaga el error")
    void gatewayError_propagates() {
        when(persistencePort.findPage(any()))
                .thenReturn(Flux.just(bootcamp(1L, "A", List.of(10L))));
        when(persistencePort.countAll()).thenReturn(Mono.just(1L));
        when(capabilityGatewayPort.findCapabilitiesByIds(anyCollection()))
                .thenReturn(Flux.error(new CapabilityValidationUnavailableException(
                        new RuntimeException("down"))));

        StepVerifier.create(useCase.listBootcamps(query(0, 10)))
                .expectError(CapabilityValidationUnavailableException.class)
                .verify();
    }
}
