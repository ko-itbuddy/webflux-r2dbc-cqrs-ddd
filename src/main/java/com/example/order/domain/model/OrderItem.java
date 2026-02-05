package com.example.order.domain.model;

import com.example.common.domain.valueobject.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItem {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    
    public static OrderItem of(String productId, String productName, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new OrderItem(productId, productName, quantity, unitPrice);
    }
    
    public Money calculateSubtotal() {
        return unitPrice.multiply(quantity);
    }
}