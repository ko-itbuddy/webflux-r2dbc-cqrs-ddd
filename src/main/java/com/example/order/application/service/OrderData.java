package com.example.order.application.service;

import com.example.order.domain.order.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderData(
    String orderId,
    String customerId,
    BigDecimal totalAmount,
    int itemCount,
    OrderStatus status,
    Instant createdAt
) {
}
