package com.bootcamp.bootcamp.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente HTTP reactivo {@link WebClient} usado para consultar
 * al microservicio de Capacidad (Capability_Service).
 *
 * <p>Define un bean {@link WebClient} con la {@code baseUrl} del Capability_Service
 * parametrizada por la propiedad {@code capability.service.url} (variable de
 * entorno {@code CAPABILITY_SERVICE_URL}, por defecto {@code http://localhost:8081}),
 * tal como se declara en {@code application.yml}. Es no bloqueante y se compone
 * con el pipeline reactivo del {@code CapabilityGatewayAdapter} (Req 7.1, 8.3).
 */
@Configuration
public class WebClientConfig {

    private final String capabilityServiceUrl;

    public WebClientConfig(
            @Value("${capability.service.url:http://localhost:8081}") String capabilityServiceUrl) {
        this.capabilityServiceUrl = capabilityServiceUrl;
    }

    /**
     * Cliente HTTP reactivo apuntando al Capability_Service.
     *
     * @return el {@link WebClient} con la {@code baseUrl} del microservicio de Capacidad.
     */
    @Bean
    public WebClient capabilityWebClient() {
        return WebClient.builder()
                .baseUrl(capabilityServiceUrl)
                .build();
    }
}
