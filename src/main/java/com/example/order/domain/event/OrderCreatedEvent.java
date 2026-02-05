package com.example.order.domain.event;

import com.example.common.domain.valueobject.Money;
import java.time.Instant;

public record OrderCreatedEvent(
    String orderId,
    String customerEmail,
    Money totalAmount,
    Instant occurredAt
) implements DomainEvent {}