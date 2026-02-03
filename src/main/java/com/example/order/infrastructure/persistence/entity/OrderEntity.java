package com.example.order.infrastructure.persistence.entity;

import com.example.order.domain.order.valueobject.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R2DBC entity representing an Order in the database.
 */
@Table("orders")
public class OrderEntity {
    
    @Id
    private String id;
    
    @Column("customer_id")
    private String customerId;
    
    @Column("customer_email")
    private String customerEmail;
    
    @Column("status")
    private OrderStatus status;
    
    @Column("total_amount")
    private BigDecimal totalAmount;
    
    @Column("currency")
    private String currency;
    
    @Column("discount_amount")
    private BigDecimal discountAmount;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("updated_at")
    private Instant updatedAt;
    
    // Default constructor for R2DBC
    public OrderEntity() {
    }
    
    // Constructor with fields
    public OrderEntity(String id, String customerId, String customerEmail, 
                       OrderStatus status, BigDecimal totalAmount, String currency,
                       BigDecimal discountAmount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.discountAmount = discountAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
