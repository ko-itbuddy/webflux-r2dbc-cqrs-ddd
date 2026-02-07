package com.example.order.application.service;

import com.example.order.application.dto.CancelOrderCommand;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.application.port.out.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CancelOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderQueryPort queryPort;

    public CancelOrderHandler(OrderRepository orderRepository, OrderQueryPort queryPort) {
        this.orderRepository = orderRepository;
        this.queryPort = queryPort;
    }

    public Mono<Order> handle(CancelOrderCommand command) {
        return queryPort.findById(command.orderId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + command.orderId())))
                .flatMap(order -> {
                    order.cancel(command.reason());
                    return orderRepository.save(order);
                });
    }
}