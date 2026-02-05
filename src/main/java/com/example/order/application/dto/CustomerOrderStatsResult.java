package com.example.order.application.dto;

import com.example.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Customer order statistics aggregation result.
 * Provides insights into customer's ordering behavior.
 */
public record CustomerOrderStatsResult(
    String customerId,
    long totalOrders,
    long activeOrders,
    long completedOrders,
    long cancelledOrders,
    BigDecimal totalSpent,
    BigDecimal averageOrderValue,
    String currency,
    Map<OrderStatus, Long> ordersByStatus,
    long totalItemsOrdered
) {
    /**
     * Calculate customer tier based on total spent.
     * VIP: > 1000, Regular: < 100
     */
    public String getCustomerTier() {
        if (totalSpent.compareTo(new BigDecimal("1000")) > 0) {
            return "VIP";
        } else if (totalSpent.compareTo(new BigDecimal("100")) < 0) {
            return "NEW";
        }
        return "REGULAR";
    }
    
    /**
     * Check if customer qualifies for VIP status.
     */
    public boolean isVip() {
        return "VIP".equals(getCustomerTier());
    }
    
    /**
     * Get order completion rate percentage.
     */
    public BigDecimal getCompletionRate() {
        if (totalOrders == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedOrders)
            .divide(BigDecimal.valueOf(totalOrders), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Get cancellation rate percentage.
     */
    public BigDecimal getCancellationRate() {
        if (totalOrders == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cancelledOrders)
            .divide(BigDecimal.valueOf(totalOrders), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
