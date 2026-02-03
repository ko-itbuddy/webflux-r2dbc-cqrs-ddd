package com.example.order.application.command.handler;

import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.entity.OrderItem;
import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateOrderHandler {
    private final OrderCommandPort orderCommandPort;
    
    public CreateOrderHandler(OrderCommandPort orderCommandPort) {
        this.orderCommandPort = orderCommandPort;
    }
    
    public Mono<Order> handle(CreateOrderCommand command) {
        List<OrderItem> items = command.items().stream()
            .map(item -> OrderItem.of(
                item.productId(),
                item.productName(),
                item.quantity(),
                Money.of(item.unitPrice(), item.currency())
            ))
            .collect(Collectors.toList());
        
        Order order = Order.create(
            command.customerId(),
            Email.of(command.customerEmail()),
            items
        );
        
        return orderCommandPort.save(order);
    }
}
