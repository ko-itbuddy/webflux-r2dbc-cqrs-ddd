package com.example.order.domain.order.entity;

import com.example.order.domain.order.valueobject.Money;
import com.example.order.domain.order.valueobject.OrderStatus;

public class OrderItem {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    
    private OrderItem(String productId, String productName, int quantity, Money unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    public static OrderItem of(String productId, String productName, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new OrderItem(productId, productName, quantity, unitPrice);
    }
    
    public Money calculateSubtotal() {
        return unitPrice.multiply(quantity);
    }
    
    public String getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public Money getUnitPrice() {
        return unitPrice;
    }
}
