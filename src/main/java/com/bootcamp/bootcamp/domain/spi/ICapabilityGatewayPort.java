package com.bootcamp.bootcamp.domain.spi;

import com.bootcamp.bootcamp.domain.model.CapabilitySummary;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * Puerto de salida (spi) del dominio para consultar capacidades en el
 * microservicio de Capacidad.
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

    /**
     * Consulta al microservicio de Capacidad los datos completos (id, nombre y sus
     * tecnologías con id y nombre) de las capacidades correspondientes a los
     * identificadores proporcionados, en una única llamada por lotes (evita N+1).
     *
     * @param ids identificadores de capacidad distintos a resolver.
     * @return un {@link Flux} que emite un {@link CapabilitySummary} por cada
     *         capacidad existente devuelta por el service (0..N).
     */
    Flux<CapabilitySummary> findCapabilitiesByIds(Collection<Long> ids);
}
