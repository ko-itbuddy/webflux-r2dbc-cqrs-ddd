package com.example.order.domain.event;

import java.time.Instant;

public record OrderPaidEvent(
    String orderId,
    Instant occurredAt
) implements DomainEvent {}