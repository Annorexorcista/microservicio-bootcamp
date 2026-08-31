package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception.InvalidPathVariableException;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.exception.RequestErrorCode;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper.BootcampDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class BootcampHandler {

    private final IBootcampServicePort servicePort;
    private final BootcampDtoMapper dtoMapper;

    public BootcampHandler(IBootcampServicePort servicePort, BootcampDtoMapper dtoMapper) {
        this.servicePort = servicePort;
        this.dtoMapper = dtoMapper;
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(BootcampRequest.class)
                .map(dtoMapper::toDomain)
                .flatMap(servicePort::registerBootcamp)
                .map(dtoMapper::toResponse)
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        return Mono.fromCallable(() -> dtoMapper.toPageQuery(request))
                .flatMap(servicePort::listBootcamps)
                .map(dtoMapper::toPageResponse)
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.fromCallable(() -> parseBootcampId(request.pathVariable("id")))
                .flatMap(servicePort::deleteBootcamp)
                .then(ServerResponse.noContent().build());
    }

    private Long parseBootcampId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new InvalidPathVariableException(RequestErrorCode.ID_REQUIRED);
        }
        String cleanId = rawId.trim();
        if (!cleanId.matches("[0-9]+")) {
            throw new InvalidPathVariableException(RequestErrorCode.ID_NOT_NUMERIC, cleanId);
        }

        try {
            long id = Long.parseLong(cleanId);
            if (id <= 0) {
                throw new InvalidPathVariableException(RequestErrorCode.ID_NOT_POSITIVE, cleanId);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new InvalidPathVariableException(
                    RequestErrorCode.ID_OUT_OF_RANGE, exception, cleanId);
        }
    }
}
