package com.example.common.adapter.config;

import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateReactiveConfig {

    @Bean
    public Mutiny.SessionFactory sessionFactory() {
        // In a real app, these would come from application.yml
        Map<String, Object> props = Map.of(
            "jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/orders",
            "jakarta.persistence.jdbc.user", "user",
            "jakarta.persistence.jdbc.password", "password",
            "hibernate.connection.pool_size", "10",
            "hibernate.show_sql", "true",
            "hibernate.format_sql", "true",
            "hibernate.hbm2ddl.auto", "update"
        );

        return Persistence.createEntityManagerFactory("order-service", props)
                .unwrap(Mutiny.SessionFactory.class);
    }
}
