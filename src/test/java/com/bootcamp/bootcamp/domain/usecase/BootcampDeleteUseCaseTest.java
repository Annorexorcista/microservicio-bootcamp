package com.bootcamp.bootcamp.domain.usecase;

import com.bootcamp.bootcamp.domain.exception.BootcampNotFoundException;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del método clave de la HU 6: {@link BootcampUseCase#deleteBootcamp}.
 *
 * <p>Verifica los tres comportamientos esenciales de la eliminación en cascada:
 * <ul>
 *   <li>bootcamp inexistente -&gt; error 404 sin borrar ni delegar;</li>
 *   <li>bootcamp con capacidades huérfanas -&gt; se borra y se delega el borrado de
 *       esas capacidades al gateway (que a su vez cascada a tecnología);</li>
 *   <li>bootcamp sin huérfanas (sus capacidades las usan otros bootcamps) -&gt; se
 *       borra pero NO se delega nada al gateway.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BootcampDeleteUseCaseTest {

    @Mock
    private IBootcampPersistencePort persistencePort;

    @Mock
    private ICapabilityGatewayPort capabilityGatewayPort;

    private BootcampUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BootcampUseCase(persistencePort, capabilityGatewayPort);
    }

    @Test
    @DisplayName("Bootcamp inexistente -> BootcampNotFoundException, sin borrar ni delegar")
    void nonExistentBootcamp_rejected() {
        when(persistencePort.existsById(99L)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.deleteBootcamp(99L))
                .expectError(BootcampNotFoundException.class)
                .verify();

        verify(persistencePort, never()).deleteByIdReturningOrphanCapabilityIds(any());
        verify(capabilityGatewayPort, never()).deleteCapabilitiesByIds(anyCollection());
    }

    @Test
    @DisplayName("Con capacidades huérfanas -> borra y delega su borrado al gateway")
    void withOrphanCapabilities_deletesAndDelegates() {
        when(persistencePort.existsById(1L)).thenReturn(Mono.just(true));
        when(persistencePort.deleteByIdReturningOrphanCapabilityIds(1L))
                .thenReturn(Mono.just(List.of(10L, 11L)));
        when(capabilityGatewayPort.deleteCapabilitiesByIds(anyCollection()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.deleteBootcamp(1L))
                .verifyComplete();

        verify(capabilityGatewayPort, times(1)).deleteCapabilitiesByIds(List.of(10L, 11L));
    }

    @Test
    @DisplayName("Sin capacidades huérfanas -> borra pero no delega nada al gateway")
    void withoutOrphanCapabilities_deletesWithoutDelegating() {
        when(persistencePort.existsById(1L)).thenReturn(Mono.just(true));
        when(persistencePort.deleteByIdReturningOrphanCapabilityIds(1L))
                .thenReturn(Mono.just(List.of()));

        StepVerifier.create(useCase.deleteBootcamp(1L))
                .verifyComplete();

        verify(persistencePort, times(1)).deleteByIdReturningOrphanCapabilityIds(eq(1L));
        verify(capabilityGatewayPort, never()).deleteCapabilitiesByIds(anyCollection());
    }
}
