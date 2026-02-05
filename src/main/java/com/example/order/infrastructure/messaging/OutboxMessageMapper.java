package com.example.order.infrastructure.messaging;

import org.springframework.stereotype.Component;

@Component
public class OutboxMessageMapper {

    public String toRoutingKey(String eventType) {
        return switch (eventType) {
            case "OrderCreatedEvent" -> "order.created";
            case "OrderConfirmedEvent" -> "order.confirmed";
            case "OrderCancelledEvent" -> "order.cancelled";
            default -> "order.events";
        };
    }
}
