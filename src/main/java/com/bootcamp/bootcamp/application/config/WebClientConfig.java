package com.bootcamp.bootcamp.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String capabilityServiceUrl;

    public WebClientConfig(
            @Value("${capability.service.url:http://localhost:8081}") String capabilityServiceUrl) {
        this.capabilityServiceUrl = capabilityServiceUrl;
    }

    @Bean
    public WebClient capabilityWebClient() {
        return WebClient.builder()
                .baseUrl(capabilityServiceUrl)
                .build();
    }
}
