package com.bootcamp.bootcamp.domain.spi;

import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ICapabilityGatewayPort {

    Flux<Long> findExistingCapabilityIds(Collection<Long> ids);

    Flux<CapabilitySummary> findCapabilitiesByIds(Collection<Long> ids);

    Mono<Void> deleteCapabilitiesByIds(Collection<Long> ids);
}
