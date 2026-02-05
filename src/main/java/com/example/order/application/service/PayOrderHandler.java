package com.example.order.application.service;

import com.example.order.application.dto.PayOrderCommand;
import com.example.order.domain.port.OrderRepository;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.exception.BusinessException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PayOrderHandler {
    private final OrderRepository commandPort;
    private final OrderQueryPort queryPort;

    public PayOrderHandler(OrderRepository commandPort, OrderQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    public Mono<Order> handle(PayOrderCommand command) {
        return queryPort.findById(command.orderId())
            .switchIfEmpty(Mono.error(new BusinessException("ORDER_006", "Order not found: " + command.orderId())))
            .flatMap(order -> {
                order.pay();
                return commandPort.save(order);
            });
    }
}
