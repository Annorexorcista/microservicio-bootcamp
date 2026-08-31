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
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public Mono<Long> countAll() {
        return bootcampRepository.count();
    }

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

    @Override
    public Mono<Boolean> existsById(Long id) {
        return bootcampRepository.existsById(id);
    }

    @Override
    public Mono<List<Long>> deleteByIdReturningOrphanCapabilityIds(Long id) {
        Mono<List<Long>> pipeline = bootcampCapabilityRepository.findByBootcampId(id)
                .map(BootcampCapabilityEntity::getCapabilityId)
                .distinct()
                .collectList()
                .flatMap(candidates ->
                        bootcampCapabilityRepository.deleteByBootcampId(id)
                                .then(bootcampRepository.deleteById(id))
                                .then(referencedAmong(candidates))
                                .map(stillReferenced -> candidates.stream()
                                        .filter(capId -> !stillReferenced.contains(capId))
                                        .toList()));

        return pipeline.as(transactionalOperator::transactional);
    }

    private Mono<Set<Long>> referencedAmong(List<Long> candidateCapabilityIds) {
        if (candidateCapabilityIds.isEmpty()) {
            return Mono.just(Set.of());
        }
        return bootcampCapabilityRepository.findByCapabilityIdIn(candidateCapabilityIds)
                .map(BootcampCapabilityEntity::getCapabilityId)
                .collect(Collectors.toSet());
    }

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
