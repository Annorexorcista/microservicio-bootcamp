package com.bootcamp.bootcamp.application.config;

import com.bootcamp.bootcamp.domain.api.IBootcampServicePort;
import com.bootcamp.bootcamp.domain.spi.IBootcampPersistencePort;
import com.bootcamp.bootcamp.domain.spi.ICapabilityGatewayPort;
import com.bootcamp.bootcamp.domain.usecase.BootcampUseCase;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.http.CapabilityGatewayAdapter;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.adapter.BootcampPersistenceAdapter;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.mapper.BootcampEntityMapper;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampCapabilityRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driven.r2dbc.repository.IBootcampRepository;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.handler.BootcampHandler;
import com.bootcamp.bootcamp.infrastructure.adapters.driving.webflux.mapper.BootcampDtoMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cableado (wiring) de la arquitectura hexagonal.
 *
 * <p>Concentra en la capa de aplicación la construcción de los beans del dominio
 * y sus adaptadores, de modo que el núcleo ({@link BootcampUseCase},
 * {@link com.bootcamp.bootcamp.domain.model.Bootcamp} y los puertos) y los
 * adaptadores driven permanecen como clases planas, libres de anotaciones de
 * Spring ({@code @Component}). Replica el enfoque de {@code microservicio_capacidad}.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Adaptador de persistencia R2DBC que implementa el puerto de salida
     * {@link IBootcampPersistencePort}.
     */
    @Bean
    public IBootcampPersistencePort bootcampPersistencePort(
            IBootcampRepository bootcampRepository,
            IBootcampCapabilityRepository bootcampCapabilityRepository,
            BootcampEntityMapper mapper,
            TransactionalOperator transactionalOperator,
            R2dbcEntityTemplate entityTemplate) {
        return new BootcampPersistenceAdapter(
                bootcampRepository, bootcampCapabilityRepository, mapper,
                transactionalOperator, entityTemplate);
    }

    /**
     * Adaptador gateway que implementa el puerto de salida
     * {@link ICapabilityGatewayPort} consultando al Capability_Service vía WebClient.
     */
    @Bean
    public ICapabilityGatewayPort capabilityGatewayPort(WebClient capabilityWebClient) {
        return new CapabilityGatewayAdapter(capabilityWebClient);
    }

    /**
     * Caso de uso del dominio, implementación del puerto de entrada
     * {@link IBootcampServicePort}.
     */
    @Bean
    public IBootcampServicePort bootcampServicePort(
            IBootcampPersistencePort persistencePort,
            ICapabilityGatewayPort capabilityGatewayPort) {
        return new BootcampUseCase(persistencePort, capabilityGatewayPort);
    }

    /**
     * Handler de la capa driving (WebFlux funcional) que orquesta el registro de
     * bootcamps. Se cablea aquí como clase plana (sin {@code @Component}).
     */
    @Bean
    public BootcampHandler bootcampHandler(
            IBootcampServicePort bootcampServicePort,
            BootcampDtoMapper bootcampDtoMapper) {
        return new BootcampHandler(bootcampServicePort, bootcampDtoMapper);
    }
}
