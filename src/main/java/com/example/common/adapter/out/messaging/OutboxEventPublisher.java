package com.example.common.adapter.out.messaging;

import com.example.common.domain.port.DomainEventPublisher;
import com.example.common.domain.event.DomainEvent;
import com.example.order.domain.event.OrderCancelledEvent;
import com.example.order.domain.event.OrderConfirmedEvent;
import com.example.order.domain.event.OrderCreatedEvent;
import com.example.common.adapter.out.persistence.outbox.OutboxEvent;
import com.example.common.adapter.out.persistence.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

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

            return OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .processed(false)
                    .build();
        });
    }
}