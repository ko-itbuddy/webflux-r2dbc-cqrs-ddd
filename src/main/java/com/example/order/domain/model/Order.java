package com.example.order.domain.model;

import com.example.common.domain.event.DomainEvent;
import com.example.common.domain.model.BaseEntity;
import com.example.order.domain.event.OrderCancelledEvent;
import com.example.order.domain.event.OrderConfirmedEvent;
import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.OrderPaidEvent;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import com.example.order.domain.exception.BusinessException;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity<Order> {
    
    private static final BigDecimal VIP_THRESHOLD = new BigDecimal("1000.00");
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal BULK_THRESHOLD = new BigDecimal("10");
    private static final BigDecimal BULK_DISCOUNT = new BigDecimal("0.05");
    private static final BigDecimal FIRST_ORDER_DISCOUNT = new BigDecimal("0.03");
    private static final BigDecimal MAX_DISCOUNT = new BigDecimal("0.15");
    
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "customer_email"))
    })
    private Email customerEmail;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money totalAmount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "discount_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "discount_currency"))
    })
    private Money discountAmount;

    private Order(String id, String customerId, Email customerEmail, List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.items = new ArrayList<>(items);
        this.items.forEach(item -> item.setOrder(this));
        this.status = OrderStatus.PENDING;
        calculateTotal();
        validatePrice();
    }

    private Order(String id, String customerId, Email customerEmail, OrderStatus status,
                  List<OrderItem> items, Money totalAmount, Money discountAmount,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.status = status;
        this.items = new ArrayList<>(items);
        this.items.forEach(item -> item.setOrder(this));
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static Order create(String customerId, Email customerEmail, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("ORDER_001", "Order must contain at least one item");
        }
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, customerId, customerEmail, items);
        order.applyAutoDiscount();
        
        order.registerEvent(new OrderCreatedEvent(
                orderId,
                customerEmail.getValue(),
                order.getFinalAmount(),
                Instant.now()
        ));
        return order;
    }

    private void validatePrice() {
        if (totalAmount == null) return;
        BigDecimal amount = totalAmount.getAmount();
        if (amount.compareTo(MIN_PRICE) < 0 || amount.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException("ORDER_009", "Total price exceeds valid range");
        }
    }

    public void applyAutoDiscount() {
        BigDecimal discount = BigDecimal.ZERO;
        if (isVipCustomer()) discount = discount.add(VIP_DISCOUNT);
        if (isBulkOrder()) discount = discount.add(BULK_DISCOUNT);
        
        BigDecimal finalPercentage = discount.min(MAX_DISCOUNT);
        if (finalPercentage.compareTo(BigDecimal.ZERO) > 0) {
            applyDiscount(finalPercentage);
        }
    }

    private boolean isVipCustomer() {
        return totalAmount.getAmount().compareTo(VIP_THRESHOLD) >= 0;
    }

    private boolean isBulkOrder() {
        return getItemCount() >= BULK_THRESHOLD.intValue();
    }

    public void applyDiscount(BigDecimal percentage) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("ORDER_002", "Can only apply discount to pending orders");
        }
        this.discountAmount = this.totalAmount.discount(percentage);
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("ORDER_004", "Can only confirm pending orders");
        }
        this.status = OrderStatus.CONFIRMED;
        this.registerEvent(new OrderConfirmedEvent(this.id, Instant.now()));
    }
    
    public void pay() {
        if (status != OrderStatus.CONFIRMED) {
            throw new BusinessException("ORDER_005", "Can only pay confirmed orders");
        }
        this.status = OrderStatus.PAID;
        this.registerEvent(new OrderPaidEvent(this.id, Instant.now()));
    }
    
    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException("ORDER_006", "Can only ship paid orders");
        }
        this.status = OrderStatus.SHIPPED;
    }
    
    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new BusinessException("ORDER_007", "Can only deliver shipped orders");
        }
        this.status = OrderStatus.DELIVERED;
    }
    
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_008", "Cannot cancel shipped or delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
        this.registerEvent(new OrderCancelledEvent(this.id, reason, Instant.now()));
    }
    
    private void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::calculateSubtotal)
            .reduce(Money::add)
            .orElse(Money.zero("USD"));
    }
    
    public Money getFinalAmount() {
        return discountAmount != null ? discountAmount : totalAmount;
    }
    
    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public static Order reconstitute(String id, String customerId, Email customerEmail,
                                     OrderStatus status, List<OrderItem> items,
                                     Money totalAmount, Money discountAmount,
                                     Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, customerEmail, status, items,
                        totalAmount, discountAmount, createdAt, updatedAt);
    }
}
