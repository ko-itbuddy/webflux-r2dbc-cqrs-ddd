package com.example.order.domain.event;

import java.time.Instant;

public record OrderConfirmedEvent(
    String orderId,
    Instant occurredAt
) implements DomainEvent {}