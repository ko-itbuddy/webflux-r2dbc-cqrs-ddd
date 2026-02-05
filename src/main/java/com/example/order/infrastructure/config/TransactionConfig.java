package com.example.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import io.r2dbc.spi.ConnectionFactory;

/**
 * Configuration for reactive transaction management.
 * Sets up R2DBC transaction manager and TransactionalOperator for WebFlux environment.
 * 
 * Note: @Transactional annotation is NOT used as it's designed for blocking environments.
 * Instead, TransactionalOperator is used with .as(transactionalOperator::transactional).
 */
@Configuration
public class TransactionConfig {

    /**
     * Creates a ReactiveTransactionManager for R2DBC.
     */
    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Creates a TransactionalOperator for programmatic transaction management.
     * This is the preferred way to handle transactions in WebFlux reactive applications.
     */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    /**
     * Creates a TransactionalOperator with default read-only settings.
     * Useful for read-only query operations.
     */
    @Bean
    public TransactionalOperator readOnlyTransactionalOperator(ReactiveTransactionManager transactionManager) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(true);
        return TransactionalOperator.create(transactionManager, definition);
    }
}
