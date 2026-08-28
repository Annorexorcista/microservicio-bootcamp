package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository;

import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampCapabilityEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Repositorio reactivo R2DBC para la tabla puente {@link BootcampCapabilityEntity}.
 *
 * <p>La entidad no tiene un {@code @Id} de una sola columna (la clave primaria de
 * la tabla es compuesta), por lo que este repositorio se tipa con {@link Long}
 * como identificador nominal pero NO se usa para {@code save}/{@code findById}
 * basados en id. La inserción de las filas del join la realiza el adaptador de
 * persistencia con {@code R2dbcEntityTemplate#insert}. Aquí se expone únicamente
 * una derived query de solo lectura para recuperar las asociaciones de un bootcamp.
 */
public interface IBootcampCapabilityRepository
        extends ReactiveCrudRepository<BootcampCapabilityEntity, Long> {

    /**
     * Recupera todas las asociaciones (filas del join) de un bootcamp concreto.
     *
     * @param bootcampId identificador del bootcamp.
     * @return un {@link Flux} con las asociaciones encontradas.
     */
    Flux<BootcampCapabilityEntity> findByBootcampId(Long bootcampId);

    /**
     * Recupera las asociaciones cuyas capacidades están en la colección dada. Se
     * usa para detectar qué capacidades siguen referenciadas por algún bootcamp
     * tras un borrado (las que no aparezcan quedaron huérfanas).
     *
     * @param capabilityIds identificadores de capacidad a comprobar.
     * @return un {@link Flux} con las asociaciones que aún referencian esas capacidades.
     */
    Flux<BootcampCapabilityEntity> findByCapabilityIdIn(Collection<Long> capabilityIds);

    /**
     * Elimina todas las asociaciones de un bootcamp concreto.
     *
     * @param bootcampId identificador del bootcamp cuyas asociaciones se borran.
     * @return un {@link Mono} que completa cuando el borrado ha terminado.
     */
    Mono<Void> deleteByBootcampId(Long bootcampId);
}
