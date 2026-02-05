package com.example.order.application.dto;

import com.example.common.domain.valueobject.Email;

import java.util.List;

public record CreateOrderCommand(
    String customerId,
    String customerEmail,
    List<OrderItemCommand> items
) {
    public record OrderItemCommand(
        String productId,
        String productName,
        int quantity,
        java.math.BigDecimal unitPrice,
        String currency
    ) {}
}
