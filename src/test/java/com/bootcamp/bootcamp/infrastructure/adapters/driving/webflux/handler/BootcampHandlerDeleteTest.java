package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception.InvalidPathVariableException;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception.RequestErrorCode;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper.BootcampDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampHandlerDeleteTest {

    @Mock
    private IBootcampServicePort servicePort;

    @Mock
    private ServerRequest request;

    private BootcampHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BootcampHandler(servicePort, new BootcampDtoMapper());
    }

    @Test
    void delete_withValidId_callsUseCaseAndReturnsNoContent() {
        when(request.pathVariable("id")).thenReturn("7");
        when(servicePort.deleteBootcamp(7L)).thenReturn(Mono.empty());

        StepVerifier.create(handler.delete(request))
                .assertNext(response -> assertThat(response.statusCode())
                        .isEqualTo(HttpStatus.NO_CONTENT))
                .verifyComplete();

        verify(servicePort).deleteBootcamp(7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "0", "9223372036854775808"})
    void delete_withInvalidId_rejectsRequestBeforeCallingUseCase(String rawId) {
        when(request.pathVariable("id")).thenReturn(rawId);

        StepVerifier.create(handler.delete(request))
                .expectError(InvalidPathVariableException.class)
                .verify();

        verify(servicePort, never()).deleteBootcamp(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void delete_withNonNumericId_usesStableErrorCode() {
        when(request.pathVariable("id")).thenReturn("abc");

        StepVerifier.create(handler.delete(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InvalidPathVariableException.class);
                    InvalidPathVariableException exception = (InvalidPathVariableException) error;
                    assertThat(exception.getCode()).isEqualTo(RequestErrorCode.ID_NOT_NUMERIC);
                })
                .verify();
    }
}
