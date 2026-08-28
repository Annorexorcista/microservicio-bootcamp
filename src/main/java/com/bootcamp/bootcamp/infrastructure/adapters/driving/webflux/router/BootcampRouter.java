package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.ErrorResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.handler.BootcampHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Router de la capa driving (WebFlux funcional) que declara las rutas del recurso
 * {@code bootcamps} y las asocia al {@link BootcampHandler}.
 *
 * <p>Los endpoints funcionales ({@code RouterFunction}) no exponen su contrato
 * automáticamente a springdoc como lo hacen los {@code @RestController}. Por ello
 * la documentación OpenAPI del endpoint se declara de forma explícita con las
 * anotaciones {@link RouterOperations}/{@link RouterOperation} sobre el método
 * que produce el bean {@code RouterFunction}, describiendo el esquema de la
 * solicitud, el de la respuesta {@code 201} y los errores {@code 400}/{@code 502}
 * (Requerimiento 9.1).
 */
@Configuration
public class BootcampRouter {

    private static final String BOOTCAMPS_PATH = "/api/v1/bootcamps";

    /**
     * Declara la ruta {@code POST /api/v1/bootcamps} (que acepta
     * {@code application/json}) y la delega en {@link BootcampHandler#register}.
     *
     * @param handler handler que procesa el registro de bootcamps.
     * @return la {@link RouterFunction} con la ruta de registro configurada.
     */
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = BOOTCAMPS_PATH,
                    method = RequestMethod.POST,
                    beanClass = IBootcampServicePort.class,
                    beanMethod = "registerBootcamp",
                    operation = @Operation(
                            operationId = "registerBootcamp",
                            summary = "Registra un nuevo bootcamp",
                            description = "Valida obligatoriedad y longitudes (nombre 1-50, "
                                    + "descripción 1-90), la fecha de lanzamiento, la duración en "
                                    + "días (entero positivo), la cantidad (1-4) y no repetición de "
                                    + "las capacidades asociadas, y la existencia de las capacidades "
                                    + "en el Capability_Service, y persiste el bootcamp.",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = BootcampRequest.class))),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Bootcamp registrado correctamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = BootcampResponse.class))),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos inválidos (nombre/descripción "
                                                    + "obligatorios o exceden la longitud máxima, "
                                                    + "fecha o duración inválidas, cantidad de "
                                                    + "capacidades fuera de rango, capacidades "
                                                    + "repetidas o inexistentes)",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(
                                            responseCode = "502",
                                            description = "No fue posible validar las capacidades "
                                                    + "porque el Capability_Service no está disponible",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = ErrorResponse.class)))
                            }))
    })
    public RouterFunction<ServerResponse> bootcampRoutes(BootcampHandler handler) {
        return RouterFunctions.route()
                .POST(BOOTCAMPS_PATH, accept(MediaType.APPLICATION_JSON), handler::register)
                .build();
    }
}
