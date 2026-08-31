package com.bootcamp.bootcamp.infrastructure.adapters.driven.http;

import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto.CapabilityDetailResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto.CapabilityGatewayResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

public class CapabilityGatewayAdapter implements ICapabilityGatewayPort {

    private final WebClient webClient;

    public CapabilityGatewayAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<Long> findExistingCapabilityIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return webClient.get()
                .uri(uri -> uri.path("/api/v1/capabilities").queryParam("ids", csv).build())
                .retrieve()
                .bodyToFlux(CapabilityGatewayResponse.class)
                .map(CapabilityGatewayResponse::id)
                .onErrorMap(ex -> new CapabilityValidationUnavailableException(ex));
    }

    @Override
    public Flux<CapabilitySummary> findCapabilitiesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return webClient.get()
                .uri(uri -> uri.path("/api/v1/capabilities").queryParam("ids", csv).build())
                .retrieve()
                .bodyToFlux(CapabilityDetailResponse.class)
                .map(r -> new CapabilitySummary(r.id(), r.name(),
                        r.technologies().stream()
                                .map(t -> new TechnologySummary(t.id(), t.name()))
                                .toList()))
                .onErrorMap(ex -> new CapabilityValidationUnavailableException(ex));
    }

    @Override
    public Mono<Void> deleteCapabilitiesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Mono.empty();
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return webClient.delete()
                .uri(uri -> uri.path("/api/v1/capabilities").queryParam("ids", csv).build())
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new CapabilityValidationUnavailableException(ex));
    }
}
