package com.example.order.application.query.port;

import com.example.order.application.query.result.CustomerOrderStatsResult;
import com.example.order.application.query.result.OrderListItemResult;
import com.example.order.application.query.result.OrderSummaryResult;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface OrderQueryPort {
    Mono<Order> findById(String orderId);
    Flux<Order> findByCustomerId(String customerId);
    Flux<Order> findByStatus(OrderStatus status);
    Flux<Order> findAll(int page, int size);
    Mono<Long> count();
    
    Mono<OrderSummaryResult> findOrderSummary(String orderId);
    Flux<OrderListItemResult> findOrderList(int page, int size);
    Mono<CustomerOrderStatsResult> findCustomerStats(String customerId);
}
