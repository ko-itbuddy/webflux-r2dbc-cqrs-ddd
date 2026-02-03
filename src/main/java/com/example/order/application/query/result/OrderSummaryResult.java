package com.example.order.application.query.result;

import com.example.order.domain.order.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Query result containing complete order details with calculated fields.
 * Used for detailed order views.
 */
public record OrderSummaryResult(
    String orderId,
    String customerId,
    String customerEmail,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    BigDecimal calculatedDiscount,
    int itemCount,
    List<OrderItemSummary> items,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Nested record for order item summary in query results.
     */
    public record OrderItemSummary(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {
    }
    
    /**
     * Check if order has discount applied.
     */
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(totalAmount) < 0;
    }
    
    /**
     * Get savings amount (difference between total and final amount).
     */
    public BigDecimal getSavingsAmount() {
        if (hasDiscount()) {
            return totalAmount.subtract(finalAmount);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate savings percentage.
     */
    public BigDecimal getSavingsPercentage() {
        if (!hasDiscount() || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getSavingsAmount()
            .divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
