package com.bootcamp.bootcamp.domain.spi;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida (spi) del dominio para la persistencia de bootcamps.
 *
 * <p>Abstracción implementada por el adaptador driven de R2DBC. El dominio
 * depende de este contrato y no de detalles de persistencia (MySQL, R2DBC,
 * transacciones), manteniendo así la inversión de dependencias hexagonal.
 */
public interface IBootcampPersistencePort {

    /**
     * Persiste el bootcamp y sus asociaciones con las capacidades de forma
     * atómica (transaccional) y no bloqueante.
     *
     * @param bootcamp bootcamp de dominio a persistir ({@code id == null}).
     * @return un {@link Mono} que emite el bootcamp persistido con su
     *         identificador asignado.
     */
    Mono<Bootcamp> save(Bootcamp bootcamp);

    /**
     * Recupera la página de bootcamps ya ordenada y paginada en la base de datos
     * según los parámetros de consulta (LIMIT/OFFSET y ORDER BY resueltos en SQL).
     * Cada {@link Bootcamp} emitido incluye sus {@code capabilityIds} resueltos; el
     * orden de emisión es el orden de la consulta.
     *
     * @param query parámetros de consulta (page, size, sortBy, direction).
     * @return un {@link Flux} con los bootcamps de la página solicitada.
     */
    Flux<Bootcamp> findPage(BootcampPageQuery query);

    /**
     * Cuenta el total de bootcamps existentes, para la metadata de paginación.
     *
     * @return un {@link Mono} que emite el total de bootcamps.
     */
    Mono<Long> countAll();
}
