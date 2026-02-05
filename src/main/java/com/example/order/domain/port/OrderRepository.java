package com.example.order.domain.port;

import com.example.order.domain.model.Order;
import reactor.core.publisher.Mono;

public interface OrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(String orderId);
    Mono<Void> deleteById(String orderId);
}