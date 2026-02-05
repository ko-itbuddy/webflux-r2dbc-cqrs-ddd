package com.example.order.application.service;

import com.example.order.application.dto.PayOrderCommand;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PayOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderQueryPort queryPort;

    public PayOrderHandler(OrderRepository orderRepository, OrderQueryPort queryPort) {
        this.orderRepository = orderRepository;
        this.queryPort = queryPort;
    }

    public Mono<Order> handle(PayOrderCommand command) {
        return queryPort.findById(command.orderId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + command.orderId())))
                .flatMap(order -> {
                    order.pay();
                    return orderRepository.save(order);
                });
    }
}