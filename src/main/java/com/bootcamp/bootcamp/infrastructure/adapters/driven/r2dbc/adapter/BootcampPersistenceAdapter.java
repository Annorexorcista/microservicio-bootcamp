package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampCapabilityEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.mapper.BootcampEntityMapper;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adaptador driven que implementa el puerto de salida {@link IBootcampPersistencePort}
 * usando Spring Data R2DBC.
 *
 * <p>Persiste en dos tablas dentro de una transacción reactiva: primero
 * {@code bootcamp} (obteniendo el id autogenerado por MySQL) y luego las filas de
 * la tabla puente {@code bootcamp_capability} (una por cada capacidad asociada).
 * Ambas escrituras se envuelven en un {@link TransactionalOperator} mediante
 * {@code .as(transactionalOperator::transactional)}, de forma que fallen o
 * confirmen de manera atómica (Req 1.2).
 *
 * <p>La entidad {@link BootcampCapabilityEntity} tiene clave primaria compuesta y
 * no un {@code @Id} de una sola columna, por lo que el guardado de sus filas se
 * hace con {@link R2dbcEntityTemplate#insert(Object)} (que siempre ejecuta un
 * INSERT) en lugar de {@code saveAll}.
 *
 * <p>No se anota con {@code @Component}: el wiring hexagonal se realiza en
 * {@code BeanConfiguration}. Es un flujo totalmente reactivo, sin llamadas
 * bloqueantes ({@code .block()}).
 */
public class BootcampPersistenceAdapter implements IBootcampPersistencePort {

    private final IBootcampRepository bootcampRepository;
    private final BootcampEntityMapper mapper;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcEntityTemplate entityTemplate;

    public BootcampPersistenceAdapter(IBootcampRepository bootcampRepository,
                                      BootcampEntityMapper mapper,
                                      TransactionalOperator transactionalOperator,
                                      R2dbcEntityTemplate entityTemplate) {
        this.bootcampRepository = bootcampRepository;
        this.mapper = mapper;
        this.transactionalOperator = transactionalOperator;
        this.entityTemplate = entityTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Guarda la entidad {@code bootcamp} para obtener el id generado, inserta
     * una fila en {@code bootcamp_capability} por cada capacidad asociada y
     * reconstruye el modelo de dominio con el id y los mismos identificadores de
     * capacidad. Todo dentro de una transacción reactiva.
     */
    @Override
    public Mono<Bootcamp> save(Bootcamp bootcamp) {
        Mono<Bootcamp> pipeline = bootcampRepository
                .save(mapper.toEntity(bootcamp))
                .flatMap(saved -> {
                    List<Long> capabilityIds = bootcamp.getCapabilityIds();
                    return Flux.fromIterable(capabilityIds)
                            .concatMap(capId -> entityTemplate.insert(
                                    new BootcampCapabilityEntity(saved.getId(), capId)))
                            .then(Mono.just(mapper.toDomain(saved, capabilityIds)));
                });

        return pipeline.as(transactionalOperator::transactional);
    }
}
