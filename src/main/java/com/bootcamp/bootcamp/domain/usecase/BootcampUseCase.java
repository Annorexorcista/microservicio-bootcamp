package com.bootcamp.bootcamp.domain.usecase;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.domain.exception.BootcampNotFoundException;
import com.bootcamp.bootcamp.domain.exception.CapabilitiesNotFoundException;
import com.bootcamp.bootcamp.domain.exception.DomainErrorCode;
import com.bootcamp.bootcamp.domain.exception.InvalidBootcampDataException;
import com.bootcamp.bootcamp.domain.exception.InvalidPageQueryException;
import com.bootcamp.bootcamp.domain.exception.PageErrorCode;
import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import com.bootcamp.bootcamp.domain.model.PagedResult;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BootcampUseCase implements IBootcampServicePort {

    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 90;
    private static final int MIN_CAPABILITIES = 1;
    private static final int MAX_CAPABILITIES = 4;
    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final IBootcampPersistencePort persistencePort;
    private final ICapabilityGatewayPort capabilityGatewayPort;

    public BootcampUseCase(IBootcampPersistencePort persistencePort,
                           ICapabilityGatewayPort capabilityGatewayPort) {
        this.persistencePort = persistencePort;
        this.capabilityGatewayPort = capabilityGatewayPort;
    }

    @Override
    public Mono<Bootcamp> registerBootcamp(Bootcamp bootcamp) {
        return validate(bootcamp)
                .flatMap(this::ensureCapabilitiesExist)
                .flatMap(persistencePort::save);
    }

    @Override
    public Mono<PagedResult<BootcampListItem>> listBootcamps(BootcampPageQuery query) {
        return validateQuery(query)
                .flatMap(validQuery -> Mono.zip(
                                persistencePort.findPage(validQuery).collectList(),
                                persistencePort.countAll())
                        .flatMap(tuple -> {
                            List<Bootcamp> pageContent = tuple.getT1();
                            long totalElements = tuple.getT2();
                            if (pageContent.isEmpty()) {
                                return Mono.just(new PagedResult<BootcampListItem>(
                                        validQuery.getPage(), validQuery.getSize(),
                                        totalElements, List.of()));
                            }
                            return enrichWithCapabilities(pageContent)
                                    .map(items -> new PagedResult<>(
                                            validQuery.getPage(), validQuery.getSize(),
                                            totalElements, items));
                        }));
    }

    @Override
    public Mono<Void> deleteBootcamp(Long id) {
        return persistencePort.existsById(id)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? persistencePort.deleteByIdReturningOrphanCapabilityIds(id)
                        : Mono.error(new BootcampNotFoundException(id)))
                .flatMap(orphanCapabilityIds -> orphanCapabilityIds.isEmpty()
                        ? Mono.empty()
                        : capabilityGatewayPort.deleteCapabilitiesByIds(orphanCapabilityIds));
    }

    private Mono<BootcampPageQuery> validateQuery(BootcampPageQuery query) {
        return Mono.defer(() -> {
            if (query.getPage() < MIN_PAGE) {
                return Mono.error(new InvalidPageQueryException(PageErrorCode.PAGE_NEGATIVE));
            }
            if (query.getSize() < MIN_SIZE) {
                return Mono.error(new InvalidPageQueryException(PageErrorCode.SIZE_TOO_SMALL));
            }
            if (query.getSize() > MAX_SIZE) {
                return Mono.error(new InvalidPageQueryException(PageErrorCode.SIZE_TOO_LARGE));
            }
            return Mono.just(query);
        });
    }

    private Mono<List<BootcampListItem>> enrichWithCapabilities(List<Bootcamp> page) {
        Set<Long> distinctIds = page.stream()
                .flatMap(b -> b.getCapabilityIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return capabilityGatewayPort.findCapabilitiesByIds(distinctIds)
                .collectMap(CapabilitySummary::getId, Function.identity())
                .map(byId -> page.stream()
                        .map(b -> new BootcampListItem(
                                b.getId(), b.getName(), b.getDescription(),
                                b.getLaunchDate(), b.getDurationInDays(),
                                b.getCapabilityIds().stream()
                                        .map(byId::get)
                                        .filter(Objects::nonNull)
                                        .toList()))
                        .toList());
    }

    private Mono<Bootcamp> validate(Bootcamp bootcamp) {
        return Mono.defer(() -> {
            String name = bootcamp.getName() == null ? null : bootcamp.getName().trim();
            String description =
                    bootcamp.getDescription() == null ? null : bootcamp.getDescription().trim();

            if (name == null || name.isEmpty()) {
                return Mono.error(new InvalidBootcampDataException(DomainErrorCode.NAME_REQUIRED));
            }
            if (name.length() > NAME_MAX_LENGTH) {
                return Mono.error(new InvalidBootcampDataException(DomainErrorCode.NAME_TOO_LONG));
            }

            if (description == null || description.isEmpty()) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.DESCRIPTION_REQUIRED));
            }
            if (description.length() > DESCRIPTION_MAX_LENGTH) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.DESCRIPTION_TOO_LONG));
            }

            if (bootcamp.getLaunchDate() == null) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.LAUNCH_DATE_REQUIRED));
            }
            if (bootcamp.getDurationInDays() <= 0) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.DURATION_INVALID));
            }

            List<Long> rawIds = bootcamp.getCapabilityIds();
            List<Long> nonNullIds = rawIds == null
                    ? List.of()
                    : rawIds.stream().filter(Objects::nonNull).toList();

            List<Long> distinctIds = nonNullIds.stream().distinct().toList();

            if (distinctIds.size() != nonNullIds.size()) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.CAPABILITIES_DUPLICATED));
            }

            if (distinctIds.size() < MIN_CAPABILITIES) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.CAPABILITIES_TOO_FEW));
            }
            if (distinctIds.size() > MAX_CAPABILITIES) {
                return Mono.error(
                        new InvalidBootcampDataException(DomainErrorCode.CAPABILITIES_TOO_MANY));
            }

            return Mono.just(new Bootcamp(
                    null,
                    name,
                    description,
                    bootcamp.getLaunchDate(),
                    bootcamp.getDurationInDays(),
                    distinctIds));
        });
    }

    private Mono<Bootcamp> ensureCapabilitiesExist(Bootcamp bootcamp) {
        List<Long> requested = bootcamp.getCapabilityIds();
        return capabilityGatewayPort.findExistingCapabilityIds(requested)
                .collectList()
                .flatMap(existing -> {
                    Set<Long> existingSet = new HashSet<>(existing);
                    List<Long> missing = requested.stream()
                            .filter(id -> !existingSet.contains(id))
                            .toList();
                    return missing.isEmpty()
                            ? Mono.just(bootcamp)
                            : Mono.error(new CapabilitiesNotFoundException(missing));
                });
    }
}
