package com.example.order.application.service;

import com.example.order.application.dto.OrderSummaryQuery;
import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Query handler for order-related queries.
 * Part of the CQRS read model.
 */
public interface OrderQueryHandler {
    
    /**
     * Handle OrderSummaryQuery - get detailed order summary.
     */
    Mono<OrderSummaryResult> handle(OrderSummaryQuery query);
    
    /**
     * Find order by ID.
     */
    Mono<Order> findById(String orderId);
    
    /**
     * Find orders by customer ID.
     */
    Flux<Order> findByCustomerId(String customerId);
    
    /**
     * Find orders by status.
     */
    Flux<Order> findByStatus(OrderStatus status);
    
    /**
     * Find all orders with pagination.
     */
    Flux<Order> findAll(int page, int size);
    
    /**
     * Count total orders.
     */
    Mono<Long> count();
    
    /**
     * Find order list for list views.
     */
    Flux<OrderListItemResult> findOrderList(int page, int size);
    
    /**
     * Find customer statistics.
     */
    Mono<CustomerOrderStatsResult> findCustomerStats(String customerId);
}
