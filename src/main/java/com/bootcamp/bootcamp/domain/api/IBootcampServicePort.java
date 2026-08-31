package com.bootcamp.bootcamp.domain.api;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import reactor.core.publisher.Mono;

public interface IBootcampServicePort {

    Mono<Bootcamp> registerBootcamp(Bootcamp bootcamp);

    Mono<PagedResult<BootcampListItem>> listBootcamps(BootcampPageQuery query);

    Mono<Void> deleteBootcamp(Long id);
}
