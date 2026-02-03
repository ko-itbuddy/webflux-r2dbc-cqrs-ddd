package com.example.order.infrastructure.web.router;

import com.example.order.infrastructure.web.handler.OrderCommandWebHandler;
import com.example.order.infrastructure.web.handler.OrderQueryWebHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Router configuration for Order API endpoints.
 * Defines all routes for command and query operations.
 */
@Configuration
public class OrderRouter {
    
    private static final String API_ORDERS = "/api/orders";
    private static final String API_CUSTOMERS = "/api/customers";
    
    @Bean
    public RouterFunction<ServerResponse> orderRoutes(
            OrderCommandWebHandler commandHandler,
            OrderQueryWebHandler queryHandler) {
        
        return RouterFunctions
            // Command routes (write operations)
            .route(POST(API_ORDERS).and(accept(MediaType.APPLICATION_JSON)), commandHandler::createOrder)
            .andRoute(POST(API_ORDERS + "/{orderId}/confirm"), commandHandler::confirmOrder)
            .andRoute(POST(API_ORDERS + "/{orderId}/pay"), commandHandler::payOrder)
            .andRoute(POST(API_ORDERS + "/{orderId}/cancel"), commandHandler::cancelOrder)
            .andRoute(POST(API_ORDERS + "/{orderId}/discount").and(accept(MediaType.APPLICATION_JSON)), commandHandler::applyDiscount)
            
            // Query routes (read operations)
            .andRoute(GET(API_ORDERS), queryHandler::listOrders)
            .andRoute(GET(API_ORDERS + "/{orderId}"), queryHandler::getOrder)
            .andRoute(GET(API_ORDERS + "/{orderId}/summary"), queryHandler::getOrderSummary)
            .andRoute(GET(API_ORDERS + "/by-status/{status}"), queryHandler::getOrdersByStatus)
            .andRoute(GET(API_CUSTOMERS + "/{customerId}/orders"), queryHandler::getCustomerOrders)
            .andRoute(GET(API_CUSTOMERS + "/{customerId}/stats"), queryHandler::getCustomerStats);
    }
}
