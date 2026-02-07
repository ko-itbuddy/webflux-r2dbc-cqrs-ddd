package com.example.order.application.service;

import com.example.order.application.dto.ApplyDiscountCommand;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.application.port.out.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApplyDiscountHandler {

    private final OrderRepository orderRepository;
    private final OrderQueryPort queryPort;

    public ApplyDiscountHandler(OrderRepository orderRepository, OrderQueryPort queryPort) {
        this.orderRepository = orderRepository;
        this.queryPort = queryPort;
    }

    public Mono<Order> handle(ApplyDiscountCommand command) {
        return queryPort.findById(command.orderId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + command.orderId())))
                .flatMap(order -> {
                    order.applyDiscount(command.discountPercentage());
                    return orderRepository.save(order);
                });
    }
}
