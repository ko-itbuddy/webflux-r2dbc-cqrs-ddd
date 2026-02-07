package com.example.order.domain.event;

import com.example.common.domain.event.DomainEvent;

import java.time.Instant;

public record OrderCancelledEvent(
    String orderId,
    String reason,
    Instant occurredAt
) implements DomainEvent {}