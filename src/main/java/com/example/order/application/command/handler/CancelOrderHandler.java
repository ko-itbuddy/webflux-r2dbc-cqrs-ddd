package com.example.order.application.command.handler;

import com.example.order.application.in.command.CancelOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.out.query.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CancelOrderHandler {
    private final OrderCommandPort commandPort;
    private final OrderQueryPort queryPort;
    
    public CancelOrderHandler(OrderCommandPort commandPort, OrderQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }
    
    public Mono<Order> handle(CancelOrderCommand command) {
        return queryPort.findById(command.orderId())
            .flatMap(order -> {
                order.cancel();
                return commandPort.save(order);
            });
    }
}
