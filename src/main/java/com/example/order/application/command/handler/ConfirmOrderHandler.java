package com.example.order.application.command.handler;

import com.example.order.application.in.command.ConfirmOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ConfirmOrderHandler {
    private final OrderCommandPort commandPort;
    private final OrderQueryPort queryPort;
    
    public ConfirmOrderHandler(OrderCommandPort commandPort, OrderQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }
    
    public Mono<Order> handle(ConfirmOrderCommand command) {
        return queryPort.findById(command.orderId())
            .flatMap(order -> {
                order.confirm();
                return commandPort.save(order);
            });
    }
}
