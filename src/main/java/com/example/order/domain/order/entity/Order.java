package com.example.order.domain.order.entity;

import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.domain.shared.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {
    private final String id;
    private final String customerId;
    private final Email customerEmail;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    private Money discountAmount;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private Order(String id, String customerId, Email customerEmail, List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        calculateTotal();
    }
    
    public static Order create(String customerId, Email customerEmail, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("ORDER_001", "Order must contain at least one item");
        }
        String orderId = UUID.randomUUID().toString();
        return new Order(orderId, customerId, customerEmail, items);
    }
    
    public void applyDiscount(BigDecimal percentage) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("ORDER_002", "Can only apply discount to pending orders");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException("ORDER_003", "Discount percentage must be between 0 and 1");
        }
        this.discountAmount = this.totalAmount.discount(percentage);
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("ORDER_004", "Can only confirm pending orders");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }
    
    public void pay() {
        if (status != OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_005", "Can only pay confirmed orders");
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }
    
    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException("ORDER_006", "Can only ship paid orders");
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }
    
    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new BusinessException("ORDER_007", "Can only deliver shipped orders");
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }
    
    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_008", "Cannot cancel shipped or delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
    
    private void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::calculateSubtotal)
            .reduce(Money::add)
            .orElse(Money.zero("USD"));
    }
    
    public Money getFinalAmount() {
        if (discountAmount != null) {
            return discountAmount;
        }
        return totalAmount;
    }
    
    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
    
    public String getId() {
        return id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Email getCustomerEmail() {
        return customerEmail;
    }
    
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public Money getTotalAmount() {
        return totalAmount;
    }
    
    public Money getDiscountAmount() {
        return discountAmount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
