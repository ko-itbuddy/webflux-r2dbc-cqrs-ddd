package com.example.order.application.service;

import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.port.OrderRepository;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreateOrderHandler implements CreateOrderUseCase {

    private final OrderRepository orderRepository;

    public CreateOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
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

        // Repository now handles domain events automatically!
        return orderRepository.save(order);
    }
}