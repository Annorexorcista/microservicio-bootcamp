package com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux;

import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampCapabilityEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.entity.BootcampEntity;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampCapabilityRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.BootcampPageResponse;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.dto.ErrorResponse;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración end-to-end del endpoint {@code GET /api/v1/bootcamps} con
 * {@link WebTestClient}, MySQL real (Testcontainers) y {@link MockWebServer}
 * simulando el Capability_Service.
 *
 * <p>Casos: GET válido -> 200 con árbol bootcamp->capacidades->tecnologías; orden
 * por name y capabilityCount asc/desc; página fuera de rango -> 200 vacío; params
 * inválidos -> 400; Capability_Service caído -> 502.
 *
 * <p><b>Requirements: 1.3, 1.4, 2.1, 2.2, 2.4, 3.1, 3.2, 3.4, 4.3, 4.4, 4.5, 5.4, 7.1</b>
 *
 * <p>Requiere Docker en ejecución.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient(timeout = "PT30S")
class BootcampListEndpointIT {

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

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void seed() {
        bootcampCapabilityRepository.deleteAll().block();
        bootcampRepository.deleteAll().block();

        // Beta(1,2), Alfa(1), Gamma(sin capacidades)
        Long beta = bootcampRepository.save(new BootcampEntity(null, "Beta", "d",
                LocalDate.of(2026, 3, 1), 30)).map(BootcampEntity::getId).block();
        insertCap(beta, 1L, 2L);
        Long alfa = bootcampRepository.save(new BootcampEntity(null, "Alfa", "d",
                LocalDate.of(2026, 3, 1), 30)).map(BootcampEntity::getId).block();
        insertCap(alfa, 1L);
        bootcampRepository.save(new BootcampEntity(null, "Gamma", "d",
                LocalDate.of(2026, 3, 1), 30)).block();
    }

    private void insertCap(Long bootcampId, Long... capIds) {
        for (Long capId : capIds) {
            entityTemplate.insert(new BootcampCapabilityEntity(bootcampId, capId)).block();
        }
    }

    /**
     * Dispatcher que responde a {@code GET /api/v1/capabilities?ids=...} devolviendo
     * por cada id una capacidad {@code {id,name:"CapN",description,technologies:[{id,name}]}}.
     */
    private void dispatchCapabilitiesEcho() {
        capabilityService.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                int idx = path.indexOf("ids=");
                String body = "[]";
                if (idx >= 0) {
                    String[] ids = path.substring(idx + 4).split(",");
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < ids.length; i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        String id = ids[i].trim();
                        sb.append(String.format(
                                "{\"id\":%s,\"name\":\"Cap%s\",\"description\":\"d\","
                                        + "\"technologies\":[{\"id\":%s0,\"name\":\"Tech%s\"}]}",
                                id, id, id, id));
                    }
                    sb.append("]");
                    body = sb.toString();
                }
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(body);
            }
        });
    }

    @Test
    @DisplayName("GET válido por name ASC -> 200 con árbol capacidades->tecnologías")
    void getValid_returns200WithNestedTree() {
        dispatchCapabilitiesEcho();

        webTestClient.get()
                .uri("/api/v1/bootcamps?page=0&size=10&sortBy=name&sortDirection=asc")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BootcampPageResponse.class)
                .value(resp -> {
                    assertThat(resp.totalElements()).isEqualTo(3L);
                    assertThat(resp.content()).extracting("name")
                            .containsExactly("Alfa", "Beta", "Gamma");
                    var alfa = resp.content().get(0);
                    assertThat(alfa.capabilities()).hasSize(1);
                    assertThat(alfa.capabilities().get(0).technologies()).hasSize(1);
                    // Gamma sin capacidades
                    assertThat(resp.content().get(2).capabilities()).isEmpty();
                });
    }

    @Test
    @DisplayName("GET por capabilityCount DESC -> Beta(2) primero")
    void getByCapabilityCountDesc_returns200Ordered() {
        dispatchCapabilitiesEcho();

        webTestClient.get()
                .uri("/api/v1/bootcamps?sortBy=capabilityCount&sortDirection=desc")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BootcampPageResponse.class)
                .value(resp -> assertThat(resp.content()).extracting("name")
                        .containsExactly("Beta", "Alfa", "Gamma"));
    }

    @Test
    @DisplayName("Página fuera de rango -> 200 con content vacío")
    void pageOutOfRange_returns200Empty() {
        dispatchCapabilitiesEcho();

        webTestClient.get()
                .uri("/api/v1/bootcamps?page=5&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BootcampPageResponse.class)
                .value(resp -> {
                    assertThat(resp.content()).isEmpty();
                    assertThat(resp.totalElements()).isEqualTo(3L);
                });
    }

    @Test
    @DisplayName("sortBy inválido -> 400")
    void invalidSortBy_returns400() {
        webTestClient.get()
                .uri("/api/v1/bootcamps?sortBy=unknown")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(err -> assertThat(err.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("page negativo -> 400")
    void negativePage_returns400() {
        webTestClient.get()
                .uri("/api/v1/bootcamps?page=-1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("size mayor a 100 -> 400")
    void sizeTooLarge_returns400() {
        webTestClient.get()
                .uri("/api/v1/bootcamps?size=101")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Capability_Service responde 500 -> 502")
    void capabilityServiceDown_returns502() {
        capabilityService.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(500);
            }
        });

        webTestClient.get()
                .uri("/api/v1/bootcamps?page=0&size=10")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(ErrorResponse.class)
                .value(err -> assertThat(err.status()).isEqualTo(HttpStatus.BAD_GATEWAY.value()));
    }
}
