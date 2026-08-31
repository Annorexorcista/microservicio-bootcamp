package com.bootcamp.bootcamp.domain.spi;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IBootcampPersistencePort {

    Mono<Bootcamp> save(Bootcamp bootcamp);

    Flux<Bootcamp> findPage(BootcampPageQuery query);

    Mono<Long> countAll();

    Mono<Boolean> existsById(Long id);

    Mono<List<Long>> deleteByIdReturningOrphanCapabilityIds(Long id);
}
