package com.example.order.application.service;

import com.example.order.application.dto.ConfirmOrderCommand;
import com.example.order.domain.port.OrderRepository;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.exception.BusinessException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ConfirmOrderHandler {
    private final OrderRepository commandPort;
    private final OrderQueryPort queryPort;

    public ConfirmOrderHandler(OrderRepository commandPort, OrderQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    public Mono<Order> handle(ConfirmOrderCommand command) {
        return queryPort.findById(command.orderId())
            .switchIfEmpty(Mono.error(new BusinessException("ORDER_006", "Order not found: " + command.orderId())))
            .filter(order -> order.getStatus() == OrderStatus.PENDING)
            .switchIfEmpty(Mono.error(new BusinessException("ORDER_004", "Can only confirm pending orders")))
            .flatMap(order -> {
                order.confirm();
                return commandPort.save(order);
            });
    }
}
