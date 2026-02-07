package com.example.common.adapter.out.messaging;

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
