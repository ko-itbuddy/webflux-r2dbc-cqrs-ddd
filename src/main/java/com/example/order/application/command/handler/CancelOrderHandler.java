package com.example.order.application.command.handler;

import com.example.order.application.in.command.CancelOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class CancelOrderHandler {
    private final OrderCommandPort commandPort;
    private final OrderQueryPort queryPort;
    private final TransactionalOperator transactionalOperator;

    public CancelOrderHandler(OrderCommandPort commandPort, OrderQueryPort queryPort,
                              TransactionalOperator transactionalOperator) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<Order> handle(CancelOrderCommand command) {
        return queryPort.findById(command.orderId())
            .flatMap(order -> {
                order.cancel(command.reason());
                return commandPort.save(order);
            })
            .as(transactionalOperator::transactional);
    }
}
