package com.bootcamp.bootcamp.domain.api;

import com.bootcamp.bootcamp.domain.model.Bootcamp;
import com.bootcamp.bootcamp.domain.model.BootcampListItem;
import com.bootcamp.bootcamp.domain.model.BootcampPageQuery;
import com.bootcamp.bootcamp.domain.model.PagedResult;
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

    /**
     * Lista los bootcamps de forma paginada y ordenada según los parámetros de
     * consulta, enriqueciendo cada bootcamp con sus capacidades (id y nombre) y,
     * dentro de cada capacidad, sus tecnologías (id y nombre), mediante una única
     * llamada por lotes al Capability_Service.
     *
     * @param query parámetros de consulta ya tipados (page, size, sortBy, direction).
     * @return un {@link Mono} que emite el {@link PagedResult} con la metadata de
     *         paginación y el contenido de la página, o un error de dominio si los
     *         parámetros son inválidos o el Capability_Service no está disponible.
     */
    Mono<PagedResult<BootcampListItem>> listBootcamps(BootcampPageQuery query);
}
