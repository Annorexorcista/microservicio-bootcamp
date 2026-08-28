package com.bootcamp.bootcamp.application.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Configuración de transaccionalidad reactiva para R2DBC.
 *
 * <p>Registra un {@link ReactiveTransactionManager} respaldado por
 * {@link R2dbcTransactionManager} sobre el {@link ConnectionFactory} que
 * autoconfigura Spring Data R2DBC, y un {@link TransactionalOperator}
 * programático. La transacción se propaga por el {@code Context} de Reactor (no
 * por {@code ThreadLocal}): commit al completar el pipeline y rollback ante error,
 * sin uso de {@code .block()}.
 *
 * <p>El {@link TransactionalOperator} lo consume el {@code BootcampPersistenceAdapter}
 * para persistir el bootcamp y sus asociaciones de forma atómica (Req 1.2, 8.1, 8.2).
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }
}
