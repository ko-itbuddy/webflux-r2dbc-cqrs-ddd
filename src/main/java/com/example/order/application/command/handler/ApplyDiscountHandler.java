package com.example.order.application.command.handler;

import com.example.order.application.in.command.ApplyDiscountCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ApplyDiscountHandler {
    private final OrderCommandPort commandPort;
    private final OrderQueryPort queryPort;
    
    public ApplyDiscountHandler(OrderCommandPort commandPort, OrderQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }
    
    public Mono<Order> handle(ApplyDiscountCommand command) {
        return queryPort.findById(command.orderId())
            .flatMap(order -> {
                order.applyDiscount(command.discountPercentage());
                return commandPort.save(order);
            });
    }
}
