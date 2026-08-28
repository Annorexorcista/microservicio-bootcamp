package com.bootcamp.bootcamp.infrastructure.adapters.driven.http;

import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto.CapabilityDetailResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.http.dto.CapabilityGatewayResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Adaptador driven que implementa {@link ICapabilityGatewayPort} consultando al
 * microservicio de Capacidad de forma no bloqueante mediante {@link WebClient}.
 *
 * <p>Consume {@code GET /api/v1/capabilities?ids=1,2,3}, que devuelve
 * {@code [{id, name, description}]} únicamente de las capacidades existentes.
 * Esta consulta por ids es la que permite validar la existencia de un conjunto
 * concreto de identificadores (distinta del listado paginado del catálogo).
 *
 * <p>Es una clase plana (sin {@code @Component}); el cableado del bean se realiza
 * en {@code BeanConfiguration}, y el {@link WebClient} con su {@code baseUrl} se
 * configura en {@code WebClientConfig}.
 */
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
}
