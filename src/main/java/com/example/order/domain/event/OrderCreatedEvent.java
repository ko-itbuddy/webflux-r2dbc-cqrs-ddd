package com.example.order.domain.event;

import com.example.order.domain.event.DomainEvent;
import com.example.common.domain.valueobject.Money;

import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String customerEmail,
        Money totalAmount,
        Instant createdAt
) implements DomainEvent {

    public OrderCreatedEvent {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be null or blank");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email must not be null or blank");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("Total amount must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at must not be null");
        }
    }
}
