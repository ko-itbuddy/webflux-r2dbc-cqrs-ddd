package com.example.order.application.port.out;

import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderQueryPort {
    Mono<Order> findById(String orderId);
    Flux<Order> findByCustomerId(String customerId);
    Flux<Order> findByStatus(OrderStatus status);
    Flux<Order> findAll(int page, int size);
    Mono<Long> count();
    
    Mono<OrderSummaryResult> findOrderSummary(String orderId);
    Flux<OrderListItemResult> findOrderList(int page, int size);
    Mono<CustomerOrderStatsResult> findCustomerStats(String customerId);

    // Keep the new method names if preferred, but align with service expectations
    default Mono<OrderSummaryResult> findOrderSummaryById(String orderId) { return findOrderSummary(orderId); }
    default Flux<OrderSummaryResult> findOrdersByCustomerId(String customerId) { return Flux.empty(); }
}
