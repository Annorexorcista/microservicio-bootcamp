package com.bootcamp.bootcamp.infrastructure.adapters.driven.http;

import com.bootcamp.bootcamp.domain.exception.CapabilityValidationUnavailableException;
import com.bootcamp.bootcamp.domain.model.TechnologySummary;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del método batch {@link CapabilityGatewayAdapter#findCapabilitiesByIds}
 * usando {@link MockWebServer} para simular el Capability_Service.
 *
 * <p>Cubre: mapeo del árbol {@code {id,name,description,technologies:[{id,name}]}}
 * a {@code CapabilitySummary} con sus {@code TechnologySummary}, emisión del
 * subconjunto devuelto, cortocircuito ante entrada vacía y traducción de errores
 * 5xx a {@link CapabilityValidationUnavailableException}.
 *
 * <p>Requirements: 5.2, 5.5, 7.1
 */
class CapabilityGatewayFindByIdsTest {

    private MockWebServer server;
    private CapabilityGatewayAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        adapter = new CapabilityGatewayAdapter(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Mapea el árbol capacidad->tecnologías con una única llamada GET")
    void mapsCapabilityTreeWithSingleCall() throws InterruptedException {
        String body = "[{\"id\":10,\"name\":\"Backend\",\"description\":\"d\","
                + "\"technologies\":[{\"id\":100,\"name\":\"Java\"},{\"id\":101,\"name\":\"Spring\"}]},"
                + "{\"id\":11,\"name\":\"DevOps\",\"description\":\"d\","
                + "\"technologies\":[{\"id\":102,\"name\":\"Docker\"}]}]";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));

        StepVerifier.create(adapter.findCapabilitiesByIds(List.of(10L, 11L)))
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(10L);
                    assertThat(c.getName()).isEqualTo("Backend");
                    assertThat(c.getTechnologies()).extracting(TechnologySummary::getName)
                            .containsExactly("Java", "Spring");
                })
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(11L);
                    assertThat(c.getTechnologies()).extracting(TechnologySummary::getId)
                            .containsExactly(102L);
                })
                .verifyComplete();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/api/v1/capabilities?ids=10,11");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Solo emite el subconjunto devuelto por el service")
    void emitsOnlyReturnedSubset() {
        String body = "[{\"id\":10,\"name\":\"Backend\",\"description\":\"d\",\"technologies\":[]}]";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));

        StepVerifier.create(adapter.findCapabilitiesByIds(List.of(10L, 11L, 12L)))
                .assertNext(c -> assertThat(c.getId()).isEqualTo(10L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Entrada vacía retorna Flux.empty sin llamada HTTP")
    void emptyInputReturnsEmptyWithoutHttpCall() {
        StepVerifier.create(adapter.findCapabilitiesByIds(List.of()))
                .verifyComplete();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("Un error 5xx se traduce a CapabilityValidationUnavailableException")
    void serverErrorMapsToUnavailableException() {
        server.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.findCapabilitiesByIds(List.of(10L, 11L)))
                .expectError(CapabilityValidationUnavailableException.class)
                .verify();
    }
}
