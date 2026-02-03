package com.example.order.infrastructure.config;

import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * R2DBC configuration for reactive database access.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.example.order.infrastructure.persistence")
public class R2dbcConfig extends AbstractR2dbcConfiguration {
    
    @Override
    public ConnectionFactory connectionFactory() {
        // ConnectionFactory will be auto-configured by Spring Boot based on application.yml
        return null;
    }
    
    @Override
    protected java.util.List<Object> getCustomConverters() {
        return java.util.List.of();
    }
}
