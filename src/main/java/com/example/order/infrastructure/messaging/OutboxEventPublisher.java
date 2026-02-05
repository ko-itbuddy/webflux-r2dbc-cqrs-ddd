package com.example.order.infrastructure.messaging;

import com.example.order.application.port.out.DomainEventPublisher;
import com.example.order.domain.event.DomainEvent;
import com.example.order.domain.order.event.OrderCancelledEvent;
import com.example.order.domain.order.event.OrderConfirmedEvent;
import com.example.order.domain.order.event.OrderCreatedEvent;
import com.example.order.infrastructure.persistence.outbox.OutboxEvent;
import com.example.order.infrastructure.persistence.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(DomainEvent event) {
        return toOutboxEvent(event)
                .flatMap(outboxRepository::save)
                .then();
    }

    private Mono<OutboxEvent> toOutboxEvent(DomainEvent event) {
        return Mono.fromCallable(() -> {
            String aggregateType;
            String aggregateId;
            String eventType = event.getClass().getSimpleName();
            String payload = objectMapper.writeValueAsString(event);

            if (event instanceof OrderCreatedEvent createdEvent) {
                aggregateType = "Order";
                aggregateId = createdEvent.orderId();
            } else if (event instanceof OrderConfirmedEvent confirmedEvent) {
                aggregateType = "Order";
                aggregateId = confirmedEvent.orderId();
            } else if (event instanceof OrderCancelledEvent cancelledEvent) {
                aggregateType = "Order";
                aggregateId = cancelledEvent.orderId();
            } else {
                aggregateType = "Unknown";
                aggregateId = "unknown";
            }

            return new OutboxEvent(aggregateType, aggregateId, eventType, payload);
        });
    }
}
