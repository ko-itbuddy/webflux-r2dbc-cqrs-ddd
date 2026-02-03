package com.example.order.application.query.result;

import com.example.order.domain.order.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simplified order list item for list views.
 * Optimized for displaying in order lists/tables.
 */
public record OrderListItemResult(
    String orderId,
    String customerId,
    OrderStatus status,
    BigDecimal finalAmount,
    String currency,
    int itemCount,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Get formatted amount with currency.
     */
    public String getFormattedAmount() {
        return finalAmount + " " + currency;
    }
    
    /**
     * Check if order is in active state (not cancelled or completed).
     */
    public boolean isActive() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.CONFIRMED ||
               status == OrderStatus.PAID;
    }
    
    /**
     * Check if order can be cancelled.
     */
    public boolean canCancel() {
        return status != OrderStatus.SHIPPED &&
               status != OrderStatus.DELIVERED;
    }
}
