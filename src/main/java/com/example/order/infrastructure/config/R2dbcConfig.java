package com.example.order.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * R2DBC configuration for reactive database access.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.example.order.infrastructure.persistence")
public class R2dbcConfig {
}