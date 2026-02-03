package com.example.order.application.out.query;

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
}
