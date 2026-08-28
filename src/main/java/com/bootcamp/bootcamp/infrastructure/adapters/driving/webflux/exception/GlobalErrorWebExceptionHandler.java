package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception;

import com.bootcamp.bootcamp.domain.exception.BootcampNotFoundException;
import com.bootcamp.bootcamp.domain.exception.CapabilitiesNotFoundException;
import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.exception.InvalidBootcampDataException;
import com.bootcamp.bootcamp.domain.exception.InvalidPageQueryException;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.ErrorResponse;

import java.time.Instant;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

/**
 * Manejador global de errores reactivo para la capa driving (WebFlux).
 *
 * <p>Intercepta las excepciones que emergen del pipeline reactivo y las traduce a
 * respuestas HTTP con un cuerpo {@link ErrorResponse} uniforme, sin bloquear
 * (todo se compone con operadores de Project Reactor, retornando
 * {@link Mono Mono&lt;ServerResponse&gt;}).
 *
 * <ul>
 *   <li>{@link InvalidBootcampDataException} -&gt; 400 Bad Request</li>
 *   <li>{@link CapabilitiesNotFoundException} -&gt; 400 Bad Request</li>
 *   <li>{@link CapabilityValidationUnavailableException} -&gt; 502 Bad Gateway</li>
 *   <li>{@link ServerWebInputException} (JSON inválido / body faltante) -&gt; 400 Bad Request</li>
 *   <li>Cualquier otra excepción -&gt; 500 Internal Server Error</li>
 * </ul>
 *
 * <p>Se registra con {@code @Order(-2)} para tener precedencia sobre el
 * {@code DefaultErrorWebExceptionHandler} de Spring Boot.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final String ERROR_CODE_BAD_REQUEST = "BAD_REQUEST";
    private static final String ERROR_CODE_NOT_FOUND = "BOOTCAMP_NOT_FOUND";
    private static final String ERROR_CODE_CAPABILITIES_NOT_FOUND = "CAPABILITIES_NOT_FOUND";
    private static final String ERROR_CODE_BAD_GATEWAY = "CAPABILITY_VALIDATION_UNAVAILABLE";
    private static final String ERROR_CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String DEFAULT_ERROR_MESSAGE = "Ocurrió un error inesperado";

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties webProperties,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * Obtiene el error asociado a la petición desde los {@link ErrorAttributes},
     * lo traduce a un {@link ErrorResponse} con el código HTTP adecuado y construye
     * la {@link ServerResponse} JSON de forma no bloqueante.
     */
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        ErrorResponse errorResponse = toErrorResponse(error);

        return ServerResponse
                .status(errorResponse.status())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    private ErrorResponse toErrorResponse(Throwable error) {
        if (error instanceof InvalidBootcampDataException invalidData) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    invalidData.getCode().getCode(),
                    invalidData.getMessage());
        }
        if (error instanceof InvalidPageQueryException invalidPageQuery) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    invalidPageQuery.getCode().getCode(),
                    invalidPageQuery.getMessage());
        }
        if (error instanceof BootcampNotFoundException notFound) {
            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    ERROR_CODE_NOT_FOUND,
                    notFound.getMessage());
        }
        if (error instanceof CapabilitiesNotFoundException capabilitiesNotFound) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    ERROR_CODE_CAPABILITIES_NOT_FOUND,
                    capabilitiesNotFound.getMessage());
        }
        if (error instanceof CapabilityValidationUnavailableException unavailable) {
            return buildErrorResponse(
                    HttpStatus.BAD_GATEWAY,
                    ERROR_CODE_BAD_GATEWAY,
                    unavailable.getMessage());
        }
        if (error instanceof ServerWebInputException inputException) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    ERROR_CODE_BAD_REQUEST,
                    inputException.getReason() != null ? inputException.getReason() : "Entrada inválida");
        }
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ERROR_CODE_INTERNAL_ERROR,
                DEFAULT_ERROR_MESSAGE);
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String code, String message) {
        return new ErrorResponse(status.value(), code, message, Instant.now());
    }
}
