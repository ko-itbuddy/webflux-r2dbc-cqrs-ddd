package com.example.order.application.command.handler;

import com.example.order.application.in.command.PayOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.shared.BusinessException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PayOrderHandler {
    private final OrderCommandPort commandPort;
    private final OrderQueryPort queryPort;

    public PayOrderHandler(OrderCommandPort commandPort, OrderQueryPort queryPort) {
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
