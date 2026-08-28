package com.bootcamp.bootcamp.domain.spi;

import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * Puerto de salida (spi) del dominio para la validación de existencia de
 * capacidades en el microservicio de Capacidad.
 *
 * <p>Abstracción implementada por el adaptador driven basado en {@code WebClient}.
 * El dominio depende de este contrato y no del transporte HTTP, cumpliendo la
 * inversión de dependencias hexagonal.
 */
public interface ICapabilityGatewayPort {

    /**
     * Consulta al microservicio de Capacidad cuáles de los identificadores
     * proporcionados corresponden a capacidades existentes.
     *
     * @param ids identificadores de capacidad a validar.
     * @return un {@link Flux} que emite únicamente los identificadores existentes
     *         (un subconjunto de los solicitados; 0..N elementos).
     */
    Flux<Long> findExistingCapabilityIds(Collection<Long> ids);
}
