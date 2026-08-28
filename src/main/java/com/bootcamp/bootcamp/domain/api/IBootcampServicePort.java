package com.bootcamp.bootcamp.domain.api;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import reactor.core.publisher.Mono;

/**
 * Puerto de entrada (api) del dominio para el registro de bootcamps.
 *
 * <p>Define el contrato que la capa driving (WebFlux) consume para orquestar el
 * caso de uso de registro. Es una abstracción pura: no conoce detalles de HTTP,
 * Spring ni persistencia. El caso de uso {@code BootcampUseCase} lo implementa.
 */
public interface IBootcampServicePort {

    /**
     * Registra un bootcamp aplicando las reglas de negocio (normalización,
     * validaciones de obligatoriedad/longitud, fecha y duración, cantidad y no
     * repetición de capacidades, y existencia de las capacidades asociadas).
     *
     * @param bootcamp bootcamp de dominio a registrar ({@code id == null}).
     * @return un {@link Mono} que emite el bootcamp persistido con su identificador
     *         asignado, o un error de dominio si alguna validación falla.
     */
    Mono<Bootcamp> registerBootcamp(Bootcamp bootcamp);
}
