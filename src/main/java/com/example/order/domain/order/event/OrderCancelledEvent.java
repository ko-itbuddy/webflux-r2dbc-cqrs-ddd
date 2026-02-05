package com.example.order.domain.order.event;

import com.example.order.domain.event.DomainEvent;
import java.time.Instant;

public record OrderCancelledEvent(
        String orderId,
        String reason,
        Instant cancelledAt
) implements DomainEvent {

    public OrderCancelledEvent {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be null or blank");
        }
        if (cancelledAt == null) {
            throw new IllegalArgumentException("Cancelled at must not be null");
        }
    }

    public OrderCancelledEvent(String orderId, String reason) {
        this(orderId, reason, Instant.now());
    }
}
