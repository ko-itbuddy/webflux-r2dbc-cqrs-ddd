package com.example.order.application.out.command;

import com.example.order.domain.order.entity.Order;
import reactor.core.publisher.Mono;

public interface OrderCommandPort {
    Mono<Order> save(Order order);
    Mono<Void> deleteById(String orderId);
}
