package com.example.order.application.service;

import com.example.order.application.dto.CancelOrderCommand;
import com.example.order.domain.port.OrderRepository;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.order.domain.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class CancelOrderHandler {
    private final OrderRepository commandPort;
    private final OrderQueryPort queryPort;
    private final TransactionalOperator transactionalOperator;

    public CancelOrderHandler(OrderRepository commandPort, OrderQueryPort queryPort,
                              TransactionalOperator transactionalOperator) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<Order> handle(CancelOrderCommand command) {
        return queryPort.findById(command.orderId())
            .switchIfEmpty(Mono.error(new BusinessException("ORDER_006", "Order not found: " + command.orderId())))
            .flatMap(order -> {
                order.cancel(command.reason());
                return commandPort.save(order);
            })
            .as(transactionalOperator::transactional);
    }
}
