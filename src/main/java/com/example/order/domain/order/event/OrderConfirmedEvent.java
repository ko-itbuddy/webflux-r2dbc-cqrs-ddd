package com.example.order.domain.order.event;

import com.example.order.domain.event.DomainEvent;
import java.time.Instant;

public record OrderConfirmedEvent(
        String orderId,
        Instant confirmedAt
) implements DomainEvent {

    public OrderConfirmedEvent {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be null or blank");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("Confirmed at must not be null");
        }
    }
}
