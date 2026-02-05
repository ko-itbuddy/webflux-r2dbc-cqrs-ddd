package com.example.order.application.port.in;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.domain.model.Order;
import reactor.core.publisher.Mono;

public interface CreateOrderUseCase {
    Mono<Order> handle(CreateOrderCommand command);
}
