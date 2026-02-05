package com.example.order.domain.model;

import com.example.order.domain.model.OrderStatus;

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
