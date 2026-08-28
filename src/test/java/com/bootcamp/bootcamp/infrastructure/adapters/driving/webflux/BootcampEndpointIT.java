package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux;

import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampCapabilityRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampRequest;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.ErrorResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración end-to-end del endpoint {@code POST /api/v1/bootcamps}
 * usando {@link WebTestClient} contra el contexto COMPLETO de Spring Boot, una
 * base de datos MySQL real levantada con Testcontainers y un {@link MockWebServer}
 * que simula el Capability_Service.
 *
 * <p>Ejercita el flujo reactivo completo de la arquitectura hexagonal sin mocks
 * de componentes internos: {@code BootcampRouter -> BootcampHandler ->
 * BootcampUseCase -> (BootcampPersistenceAdapter -> MySQL) +
 * (CapabilityGatewayAdapter -> MockWebServer)}, junto con la traducción de errores
 * del {@code GlobalErrorWebExceptionHandler}.
 *
 * <p>Casos cubiertos:
 * <ul>
 *   <li>POST válido -&gt; {@code 201 Created} con {@link BootcampResponse} (Req 1.3).</li>
 *   <li>Nombre vacío / descripción vacía -&gt; {@code 400} (Req 2.1, 3.1).</li>
 *   <li>Fecha nula / duración inválida -&gt; {@code 400} (Req 4.1, 4.2).</li>
 *   <li>Menos de 1 / más de 4 capacidades -&gt; {@code 400} (Req 5.1, 5.2).</li>
 *   <li>Capacidades repetidas -&gt; {@code 400} (Req 6.1).</li>
 *   <li>Capacidad inexistente -&gt; {@code 400} (Req 7.2).</li>
 *   <li>Capability_Service caído -&gt; {@code 502} (Req 7.4).</li>
 * </ul>
 *
 * <p><b>Requirements: 1.3, 2.1, 3.1, 4.1, 4.2, 5.1, 5.2, 6.1, 7.2, 7.4</b>
 *
 * <p>Se usa un {@code GenericContainer<>("mysql:8.0")} (NO {@code MySQLContainer}).
 * Requiere Docker en ejecución.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient(timeout = "PT30S")
class BootcampEndpointIT {

    private static final int MYSQL_PORT = 3306;
    private static final String DB_NAME = "bootcamp_db";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "test";

    private static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0")
            .withEnv("MYSQL_DATABASE", DB_NAME)
            .withEnv("MYSQL_USER", DB_USER)
            .withEnv("MYSQL_PASSWORD", DB_PASSWORD)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withExposedPorts(MYSQL_PORT)
            .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(180)));

    private static MockWebServer capabilityService;

    @BeforeAll
    static void startInfrastructure() throws IOException {
        MYSQL.start();
        capabilityService = new MockWebServer();
        capabilityService.start();
    }

    @AfterAll
    static void stopInfrastructure() throws IOException {
        if (capabilityService != null) {
            capabilityService.shutdown();
        }
        MYSQL.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:mysql://%s:%d/%s",
                MYSQL.getHost(), MYSQL.getMappedPort(MYSQL_PORT), DB_NAME));
        registry.add("spring.r2dbc.username", () -> DB_USER);
        registry.add("spring.r2dbc.password", () -> DB_PASSWORD);
        registry.add("capability.service.url", () -> capabilityService.url("/").toString());
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IBootcampRepository bootcampRepository;

    @Autowired
    private IBootcampCapabilityRepository bootcampCapabilityRepository;

    @BeforeEach
    void cleanState() {
        bootcampCapabilityRepository.deleteAll().block();
        bootcampRepository.deleteAll().block();
        capabilityService.setDispatcher(new QueueDispatcher());
    }

    /** Encola una respuesta que declara existentes exactamente los ids dados. */
    private void enqueueExistingCapabilities(List<Long> ids) {
        String body = ids.stream()
                .map(id -> String.format(
                        "{\"id\":%d,\"name\":\"Cap%d\",\"description\":\"d\"}", id, id))
                .collect(Collectors.joining(",", "[", "]"));
        capabilityService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    private BootcampRequest request(String name, String description,
                                    LocalDate launchDate, Integer durationInDays, List<Long> capIds) {
        return new BootcampRequest(name, description, launchDate, durationInDays, capIds);
    }

    // --- 1. POST válido -> 201 (Req 1.3) ---

    @Test
    @DisplayName("POST válido -> 201 con BootcampResponse")
    void post_withValidData_returns201() {
        List<Long> capIds = List.of(1L, 2L, 3L);
        enqueueExistingCapabilities(capIds);

        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend 2026", "Bootcamp backend",
                        LocalDate.of(2026, 3, 1), 84, capIds))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(BootcampResponse.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull().isPositive();
                    assertThat(response.name()).isEqualTo("Backend 2026");
                    assertThat(response.launchDate()).isEqualTo(LocalDate.of(2026, 3, 1));
                    assertThat(response.durationInDays()).isEqualTo(84);
                    assertThat(response.capabilityIds()).containsExactlyInAnyOrderElementsOf(capIds);
                });
    }

    // --- 2. Nombre / descripción inválidos -> 400 (Req 2.1, 3.1) ---

    @Test
    @DisplayName("Nombre vacío -> 400")
    void post_withEmptyName_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("   ", "desc", LocalDate.of(2026, 3, 1), 30, List.of(1L)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(err -> assertThat(err.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("Descripción vacía -> 400")
    void post_withEmptyDescription_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "   ", LocalDate.of(2026, 3, 1), 30, List.of(1L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- 3. Fecha / duración inválidas -> 400 (Req 4.1, 4.2) ---

    @Test
    @DisplayName("Fecha de lanzamiento nula -> 400")
    void post_withNullLaunchDate_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", null, 30, List.of(1L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Duración nula -> 400")
    void post_withNullDuration_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), null, List.of(1L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Duración 0 -> 400")
    void post_withZeroDuration_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 0, List.of(1L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- 4. Cantidad de capacidades fuera de rango -> 400 (Req 5.1, 5.2) ---

    @Test
    @DisplayName("0 capacidades -> 400")
    void post_withZeroCapabilities_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 30, List.of()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("5 capacidades -> 400")
    void post_withFiveCapabilities_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 30,
                        List.of(1L, 2L, 3L, 4L, 5L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- 5. Capacidades repetidas -> 400 (Req 6.1) ---

    @Test
    @DisplayName("Capacidades repetidas -> 400")
    void post_withDuplicatedCapabilities_returns400() {
        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 30,
                        List.of(1L, 2L, 2L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- 6. Capacidad inexistente -> 400 (Req 7.2) ---

    @Test
    @DisplayName("Capacidad inexistente -> 400")
    void post_withNonExistentCapability_returns400() {
        // Se solicitan 1,2,3 pero el service solo conoce 1 y 2.
        enqueueExistingCapabilities(List.of(1L, 2L));

        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 30,
                        List.of(1L, 2L, 3L)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- 7. Capability_Service caído -> 502 (Req 7.4) ---

    @Test
    @DisplayName("Capability_Service responde 500 -> 502")
    void post_whenCapabilityServiceReturns500_returns502() {
        capabilityService.enqueue(new MockResponse().setResponseCode(500));

        webTestClient.post()
                .uri("/api/v1/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("Backend", "desc", LocalDate.of(2026, 3, 1), 30, List.of(1L, 2L)))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(ErrorResponse.class)
                .value(err -> assertThat(err.status()).isEqualTo(HttpStatus.BAD_GATEWAY.value()));
    }
}
