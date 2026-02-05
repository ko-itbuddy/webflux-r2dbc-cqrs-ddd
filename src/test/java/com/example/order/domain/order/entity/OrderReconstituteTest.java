package com.example.order.domain.order.entity;

import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.domain.order.valueobject.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderReconstituteTest {

    @Test
    void shouldReconstituteOrder() {
        String orderId = "order-001";
        String customerId = "customer-001";
        Email email = Email.of("test@example.com");
        OrderStatus status = OrderStatus.PAID;
        Money totalAmount = Money.of(100, "USD");
        Money discountAmount = Money.of(90, "USD");
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        Order order = Order.reconstitute(
            orderId, customerId, email, status, List.of(),
            totalAmount, discountAmount, createdAt, updatedAt
        );

        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getCustomerEmail()).isEqualTo(email);
        assertThat(order.getStatus()).isEqualTo(status);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(order.getFinalAmount()).isEqualTo(discountAmount);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldReconstituteOrderWithItems() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.CONFIRMED, List.of(item), Money.of(100, "USD"),
            null, Instant.now(), Instant.now()
        );

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItemCount()).isEqualTo(2);
    }

    @Test
    void shouldReconstituteCancelledOrder() {
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.CANCELLED, List.of(), Money.of(100, "USD"),
            null, Instant.now(), Instant.now()
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldCalculateFinalAmountWhenNoDiscount() {
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.PENDING, List.of(), Money.of(100, "USD"),
            null, Instant.now(), Instant.now()
        );

        assertThat(order.getFinalAmount()).isEqualTo(Money.of(100, "USD"));
        assertThat(order.getDiscountAmount()).isNull();
    }

    @Test
    void shouldCalculateFinalAmountWithDiscount() {
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.PENDING, List.of(), Money.of(100, "USD"),
            Money.of(90, "USD"), Instant.now(), Instant.now()
        );

        assertThat(order.getFinalAmount()).isEqualTo(Money.of(90, "USD"));
        assertThat(order.getDiscountAmount()).isEqualTo(Money.of(90, "USD"));
    }

    @Test
    void shouldAllowStateTransitionsOnReconstitutedOrder() {
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.CONFIRMED, List.of(), Money.of(100, "USD"),
            null, Instant.now(), Instant.now()
        );

        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldTrackDomainEventsOnReconstitutedOrder() {
        Order order = Order.reconstitute(
            "order-001", "customer-001", Email.of("test@example.com"),
            OrderStatus.PENDING, List.of(), Money.of(100, "USD"),
            null, Instant.now(), Instant.now()
        );

        order.confirm();

        assertThat(order.getDomainEvents()).hasSize(1);
    }
}
