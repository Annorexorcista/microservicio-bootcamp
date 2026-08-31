package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository;

import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface IBootcampRepository extends ReactiveCrudRepository<BootcampEntity, Long> {
}
