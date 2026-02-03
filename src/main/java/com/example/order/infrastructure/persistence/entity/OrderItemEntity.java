package com.example.order.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * R2DBC entity representing an OrderItem in the database.
 */
@Table("order_items")
public class OrderItemEntity {
    
    @Id
    private Long id;
    
    @Column("order_id")
    private String orderId;
    
    @Column("product_id")
    private String productId;
    
    @Column("product_name")
    private String productName;
    
    @Column("quantity")
    private int quantity;
    
    @Column("unit_price")
    private BigDecimal unitPrice;
    
    @Column("currency")
    private String currency;
    
    // Default constructor for R2DBC
    public OrderItemEntity() {
    }
    
    // Constructor with fields
    public OrderItemEntity(Long id, String orderId, String productId, String productName,
                           int quantity, BigDecimal unitPrice, String currency) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
