package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper.BootcampDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler de la capa driving (WebFlux funcional) para el registro de bootcamps.
 *
 * <p>Compone un pipeline reactivo de extremo a extremo, sin llamadas bloqueantes
 * ({@code .block()}): deserializa el cuerpo de la solicitud a {@link BootcampRequest},
 * lo mapea al modelo de dominio, delega en el puerto de entrada
 * {@link IBootcampServicePort}, mapea el resultado a DTO de respuesta y construye
 * la respuesta {@code 201 Created}.
 *
 * <p>El manejo de errores no ocurre aquí: cualquier {@code Mono.error} emitido por
 * el caso de uso (validación, existencia de capacidades) o por la deserialización
 * fluye por el pipeline y lo traduce el handler global de errores.
 *
 * <p>Se construye como bean en {@code BeanConfiguration}, por lo que la clase no
 * lleva la anotación {@code @Component}. Las dependencias se inyectan por constructor.
 */
public class BootcampHandler {

    private final IBootcampServicePort servicePort;
    private final BootcampDtoMapper dtoMapper;

    public BootcampHandler(IBootcampServicePort servicePort, BootcampDtoMapper dtoMapper) {
        this.servicePort = servicePort;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Registra un bootcamp a partir de la solicitud HTTP.
     *
     * <p>Pipeline reactivo: {@code bodyToMono -> map(toDomain) ->
     * flatMap(registerBootcamp) -> map(toResponse) -> flatMap(ServerResponse 201)}.
     * Los errores se propagan hacia el handler global; aquí no se capturan.
     *
     * @param request la solicitud del servidor con el cuerpo {@link BootcampRequest}.
     * @return un {@link Mono} que emite la respuesta {@code 201 Created} con el DTO
     *         del bootcamp creado, o propaga el error correspondiente.
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(BootcampRequest.class)
                .map(dtoMapper::toDomain)
                .flatMap(servicePort::registerBootcamp)
                .map(dtoMapper::toResponse)
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    /**
     * Lista los bootcamps de forma paginada y ordenada a partir de los query
     * params de la solicitud.
     *
     * <p>Pipeline reactivo sin bloqueos: {@code fromCallable(toPageQuery) ->
     * flatMap(listBootcamps) -> map(toPageResponse) -> flatMap(ServerResponse 200)}.
     * El parseo de los query params se envuelve en {@code Mono.fromCallable} para
     * que un valor inválido emerja como {@code Mono.error} y lo traduzca el handler
     * global a 400. Los errores del dominio y del gateway se propagan igualmente.
     *
     * @param request la solicitud del servidor con los query params page, size,
     *                sortBy y sortDirection.
     * @return un {@link Mono} que emite la respuesta {@code 200 OK} con la página.
     */
    public Mono<ServerResponse> list(ServerRequest request) {
        return Mono.fromCallable(() -> dtoMapper.toPageQuery(request))
                .flatMap(servicePort::listBootcamps)
                .map(dtoMapper::toPageResponse)
                .flatMap(response -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    /**
     * Elimina el bootcamp indicado por la variable de ruta {@code id} y, en
     * cascada, las capacidades y tecnologías que queden huérfanas. Responde
     * {@code 204 No Content}; sin bloqueos. Un bootcamp inexistente se traduce a
     * 404 y un fallo del Capability_Service a 502 por el handler global. Un
     * {@code id} no numérico emerge como error y se traduce a 400.
     *
     * @param request la solicitud del servidor con la variable de ruta {@code id}.
     * @return un {@link Mono} que emite la respuesta {@code 204 No Content}.
     */
    public Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.fromCallable(() -> Long.parseLong(request.pathVariable("id")))
                .flatMap(servicePort::deleteBootcamp)
                .then(ServerResponse.noContent().build());
    }
}
