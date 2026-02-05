package com.example.order.domain.service;

import com.example.common.domain.valueobject.Money;
import com.example.order.domain.model.OrderData;
import com.example.order.domain.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

public class DiscountCalculationService {
    
    private static final BigDecimal VIP_THRESHOLD = new BigDecimal("1000.00");
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal BULK_THRESHOLD = new BigDecimal("10");
    private static final BigDecimal BULK_DISCOUNT = new BigDecimal("0.05");
    private static final BigDecimal FIRST_ORDER_DISCOUNT = new BigDecimal("0.03");
    private static final BigDecimal MAX_DISCOUNT = new BigDecimal("0.15");
    
    public BigDecimal calculateDiscount(OrderData data) {
        BigDecimal discount = BigDecimal.ZERO;
        
        if (isVipCustomer(data)) {
            discount = discount.add(VIP_DISCOUNT);
        }
        
        if (isBulkOrder(data)) {
            discount = discount.add(BULK_DISCOUNT);
        }
        
        if (isFirstOrder(data)) {
            discount = discount.add(FIRST_ORDER_DISCOUNT);
        }
        
        return discount.min(MAX_DISCOUNT);
    }
    
    private boolean isVipCustomer(OrderData data) {
        return data.totalAmount().compareTo(VIP_THRESHOLD) >= 0;
    }
    
    private boolean isBulkOrder(OrderData data) {
        return data.itemCount() >= BULK_THRESHOLD.intValue();
    }
    
    private boolean isFirstOrder(OrderData data) {
        return data.status() == OrderStatus.PENDING && 
               data.createdAt().isAfter(Instant.now().minusSeconds(3600));
    }
    
    public Function<OrderData, BigDecimal> getDiscountCalculator() {
        return this::calculateDiscount;
    }
}
