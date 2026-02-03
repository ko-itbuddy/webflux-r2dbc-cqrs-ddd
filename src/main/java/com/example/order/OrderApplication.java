package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Order Management System.
 * CQRS Hexagonal DDD architecture with WebFlux and R2DBC.
 */
@SpringBootApplication(scanBasePackages = "com.example.order")
public class OrderApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
