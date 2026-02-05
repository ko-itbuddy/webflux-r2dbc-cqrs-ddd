package com.example.order.domain.event;

import com.example.order.domain.event.DomainEvent;
import java.time.Instant;

public record OrderPaidEvent(
        String orderId,
        Instant paidAt
) implements DomainEvent {

    public OrderPaidEvent {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be null or blank");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("Paid at must not be null");
        }
    }
}
