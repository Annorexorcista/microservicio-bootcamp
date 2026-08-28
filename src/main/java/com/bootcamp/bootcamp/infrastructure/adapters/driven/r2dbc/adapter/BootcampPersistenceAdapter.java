package com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.BootcampSortBy;
import com.bootcamp.bootcamp.domain.model.BootcampSortDirection;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampCapabilityEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.mapper.BootcampEntityMapper;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampCapabilityRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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
    private final IBootcampCapabilityRepository bootcampCapabilityRepository;
    private final BootcampEntityMapper mapper;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcEntityTemplate entityTemplate;

    public BootcampPersistenceAdapter(IBootcampRepository bootcampRepository,
                                      IBootcampCapabilityRepository bootcampCapabilityRepository,
                                      BootcampEntityMapper mapper,
                                      TransactionalOperator transactionalOperator,
                                      R2dbcEntityTemplate entityTemplate) {
        this.bootcampRepository = bootcampRepository;
        this.bootcampCapabilityRepository = bootcampCapabilityRepository;
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

    /**
     * {@inheritDoc}
     *
     * <p>Delega en {@code count()} del repositorio reactivo.
     */
    @Override
    public Mono<Long> countAll() {
        return bootcampRepository.count();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ejecuta la consulta paginada y ordenada en la base de datos (LIMIT/OFFSET
     * y ORDER BY resueltos en SQL). Para {@code NAME} ordena por {@code b.name};
     * para {@code CAPABILITY_COUNT} calcula el conteo con {@code LEFT JOIN} sobre la
     * tabla puente, {@code GROUP BY} por bootcamp y {@code COUNT}. El
     * {@code sortBy}/{@code direction} se traducen desde los enums a fragmentos SQL
     * de una lista blanca, evitando inyección.
     *
     * <p>Tras obtener los bootcamps de la página (en el orden de la consulta),
     * resuelve los {@code capabilityIds} de cada uno con {@code findByBootcampId}
     * usando {@code concatMap} para preservar ese orden.
     */
    @Override
    public Flux<Bootcamp> findPage(BootcampPageQuery query) {
        long offset = (long) query.getPage() * query.getSize();
        String sql = buildPageSql(query.getSortBy(), query.getDirection());
        return entityTemplate.getDatabaseClient().sql(sql)
                .bind("size", query.getSize())
                .bind("offset", offset)
                .map((row, meta) -> new BootcampEntity(
                        row.get("id", Long.class),
                        row.get("name", String.class),
                        row.get("description", String.class),
                        row.get("launch_date", LocalDate.class),
                        row.get("duration_days", Integer.class)))
                .all()
                .concatMap(entity -> bootcampCapabilityRepository.findByBootcampId(entity.getId())
                        .map(BootcampCapabilityEntity::getCapabilityId)
                        .collectList()
                        .map(ids -> mapper.toDomain(entity, ids)));
    }

    /**
     * Construye el SQL de la página traduciendo {@code sortBy}/{@code direction}
     * a fragmentos fijos de una lista blanca (sin interpolar entrada de usuario).
     */
    private String buildPageSql(BootcampSortBy sortBy, BootcampSortDirection direction) {
        String dir = direction == BootcampSortDirection.DESC ? "DESC" : "ASC";
        if (sortBy == BootcampSortBy.CAPABILITY_COUNT) {
            return "SELECT b.id, b.name, b.description, b.launch_date, b.duration_days, "
                    + "COUNT(bc.capability_id) AS cap_count "
                    + "FROM bootcamp b "
                    + "LEFT JOIN bootcamp_capability bc ON bc.bootcamp_id = b.id "
                    + "GROUP BY b.id, b.name, b.description, b.launch_date, b.duration_days "
                    + "ORDER BY cap_count " + dir + ", b.id " + dir + " "
                    + "LIMIT :size OFFSET :offset";
        }
        return "SELECT b.id, b.name, b.description, b.launch_date, b.duration_days "
                + "FROM bootcamp b "
                + "ORDER BY b.name " + dir + ", b.id " + dir + " "
                + "LIMIT :size OFFSET :offset";
    }
}
