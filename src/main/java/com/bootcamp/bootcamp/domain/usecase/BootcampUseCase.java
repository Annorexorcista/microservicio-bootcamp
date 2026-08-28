package com.bootcamp.bootcamp.domain.usecase;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
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

/**
 * Caso de uso del dominio para el registro de bootcamps.
 *
 * <p>Implementa {@link IBootcampServicePort} y concentra las reglas de negocio
 * como un pipeline reactivo. Cada paso transforma/valida produciendo un
 * {@link Mono} de éxito o un {@code Mono.error(...)} de fallo, de modo que un
 * error corta el pipeline sin persistir nada. No usa {@code .block()}.
 *
 * <p>Depende de dos puertos de salida: {@link IBootcampPersistencePort} para la
 * persistencia y {@link ICapabilityGatewayPort} para validar la existencia de las
 * capacidades asociadas.
 */
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

    /**
     * Lista los bootcamps de forma paginada y ordenada, componiendo un único
     * pipeline reactivo sin llamadas bloqueantes:
     * <ol>
     *   <li>valida el rango de {@code page}/{@code size} (Req 4.3-4.6);</li>
     *   <li>obtiene, en paralelo, la página desde la BD (ya ordenada y paginada) y
     *       el conteo total (Req 1.1, 1.2);</li>
     *   <li>si la página está vacía, omite el gateway y retorna un
     *       {@link PagedResult} vacío con la metadata coherente (Req 1.4, 5.6);</li>
     *   <li>en caso contrario, enriquece cada bootcamp con sus capacidades y
     *       tecnologías mediante una única llamada por lotes (Req 5).</li>
     * </ol>
     *
     * <p>El orden emitido por {@code findPage} se preserva. El error de
     * indisponibilidad del Capability_Service se propaga sin capturarse (Req 7.1).
     */
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

    /**
     * Valida el rango de los parámetros de paginación en memoria (sin I/O):
     * {@code page < 0} -> {@code PAGE_NEGATIVE}; {@code size < 1} ->
     * {@code SIZE_TOO_SMALL}; {@code size > 100} -> {@code SIZE_TOO_LARGE}. Los
     * valores de {@code sortBy}/{@code direction} ya llegan resueltos a enum (o al
     * default) desde la capa driving.
     */
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

    /**
     * Enriquece los bootcamps de la página con sus capacidades y tecnologías,
     * evitando el problema N+1: recolecta todos los {@code capabilityId} distintos
     * de la página (preservando el orden de aparición) y hace una única llamada por
     * lotes al gateway; con el mapa {@code id -> CapabilitySummary} resultante,
     * asocia a cada bootcamp únicamente las capacidades resueltas (omitiendo las no
     * devueltas por el service, Req 5.5), preservando el orden de la página.
     */
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

    /**
     * Normaliza y valida sintácticamente el bootcamp en memoria (sin I/O):
     * <ol>
     *   <li>trim de nombre y descripción;</li>
     *   <li>nombre obligatorio y de longitud ≤ 50 (Req 2.1, 2.2);</li>
     *   <li>descripción obligatoria y de longitud ≤ 90 (Req 3.1, 3.2);</li>
     *   <li>fecha de lanzamiento obligatoria (Req 4.1);</li>
     *   <li>duración en días > 0 (Req 4.2);</li>
     *   <li>normaliza los ids de capacidad descartando nulls y detecta repetidos
     *       comparando el tamaño de la lista con el de los distintos (Req 6.1);</li>
     *   <li>valida la cantidad de distintos: mínimo 1 y máximo 4 (Req 5.1, 5.2);</li>
     *   <li>emite un {@link Bootcamp} normalizado (trim + ids distintos, {@code id == null}).</li>
     * </ol>
     *
     * @param bootcamp bootcamp de entrada tal como llega desde la capa driving.
     * @return un {@link Mono} que emite el bootcamp normalizado o un
     *         {@link InvalidBootcampDataException} si alguna regla falla.
     */
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

    /**
     * Valida que todas las capacidades asociadas existan en el Capability_Service,
     * consultando el gateway (Req 7.1). Calcula los identificadores faltantes
     * comparando los solicitados contra el conjunto de existentes devuelto por el
     * gateway; si hay al menos uno faltante, rechaza el registro con
     * {@link CapabilitiesNotFoundException} sin persistir (Req 7.2). Si todas
     * existen, continúa el proceso (Req 7.3).
     *
     * <p>La indisponibilidad del Capability_Service se traduce a
     * {@code CapabilityValidationUnavailableException} en el adaptador del gateway
     * (vía {@code onErrorMap}); aquí simplemente se propaga el error sin capturarlo.
     *
     * @param bootcamp bootcamp ya normalizado y validado sintácticamente.
     * @return un {@link Mono} que emite el bootcamp si todas las capacidades
     *         existen, o un error que corta el pipeline.
     */
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
