package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository;

import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Repositorio reactivo R2DBC para la entidad {@link BootcampEntity}.
 *
 * <p>Extiende {@link ReactiveCrudRepository} para heredar las operaciones CRUD
 * no bloqueantes (por ejemplo, {@code save} que retorna {@code Mono<BootcampEntity>}
 * con el id autogenerado).
 */
public interface IBootcampRepository extends ReactiveCrudRepository<BootcampEntity, Long> {
}
