package com.example.order.application.service;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.domain.port.OrderRepository;
import com.example.order.domain.port.DomainEventPublisher;
import com.example.order.domain.event.DomainEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateOrderHandler implements CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final TransactionalOperator transactionalOperator;

    public CreateOrderHandler(
            OrderRepository orderRepository,
            DomainEventPublisher domainEventPublisher,
            TransactionalOperator transactionalOperator) {
        this.orderRepository = orderRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.transactionalOperator = transactionalOperator;
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

        List<DomainEvent> events = order.getDomainEvents();
        order.clearDomainEvents();

        return orderRepository.save(order)
            .flatMap(savedOrder ->
                Flux.fromIterable(events)
                    .flatMap(domainEventPublisher::publish)
                    .then(Mono.just(savedOrder))
            )
            .as(transactionalOperator::transactional);
    }
}
