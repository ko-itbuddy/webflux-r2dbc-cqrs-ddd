package com.example.order.domain.event;

import com.example.common.domain.event.DomainEvent;

import java.time.Instant;

public record OrderConfirmedEvent(
    String orderId,
    Instant occurredAt
) implements DomainEvent {}