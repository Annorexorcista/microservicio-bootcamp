package com.bootcamp.bootcamp.domain.spi;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
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
}
