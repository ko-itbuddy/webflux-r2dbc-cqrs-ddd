package com.example.order.domain.order.entity;

import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void shouldCreateOrderWithItems() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        assertThat(order.getCustomerId()).isEqualTo("customer-001");
        assertThat(order.getCustomerEmail().getValue()).isEqualTo("test@example.com");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount().getAmount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrderWithoutItems() {
        assertThatThrownBy(() -> Order.create("customer-001", Email.of("test@example.com"), List.of()))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_001"))
            .hasMessageContaining("Order must contain at least one item");
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrderWithNullItems() {
        assertThatThrownBy(() -> Order.create("customer-001", Email.of("test@example.com"), null))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_001"))
            .hasMessageContaining("Order must contain at least one item");
    }

    @Test
    void shouldConfirmPendingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        order.confirm();
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldThrowExceptionWhenConfirmingNonPendingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        
        assertThatThrownBy(() -> order.confirm())
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_004"));
    }

    @Test
    void shouldPayConfirmedOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        
        order.pay();
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldThrowExceptionWhenPayingNonConfirmedOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        assertThatThrownBy(() -> order.pay())
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_005"));
    }

    @Test
    void shouldShipPaidOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        order.pay();
        
        order.ship();
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldDeliverShippedOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        order.pay();
        order.ship();
        
        order.deliver();
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldCancelPendingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        order.cancel("Test cancellation");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldCancelConfirmedOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();

        order.cancel("Test cancellation");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowExceptionWhenCancellingShippedOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        order.pay();
        order.ship();

        assertThatThrownBy(() -> order.cancel("Test cancellation"))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_008"));
    }

    @Test
    void shouldThrowExceptionWhenCancellingDeliveredOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        order.pay();
        order.ship();
        order.deliver();

        assertThatThrownBy(() -> order.cancel("Test cancellation"))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_008"));
    }

    @Test
    void shouldApplyDiscountToPendingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        order.applyDiscount(new BigDecimal("0.1"));
        
        assertThat(order.getDiscountAmount()).isNotNull();
        assertThat(order.getFinalAmount().getAmount()).isEqualByComparingTo(new BigDecimal("90"));
    }

    @Test
    void shouldThrowExceptionWhenApplyingDiscountToNonPendingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        
        assertThatThrownBy(() -> order.applyDiscount(new BigDecimal("0.1")))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_002"));
    }

    @Test
    void shouldThrowExceptionWhenApplyingInvalidDiscount() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        assertThatThrownBy(() -> order.applyDiscount(new BigDecimal("-0.1")))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_003"));
        
        assertThatThrownBy(() -> order.applyDiscount(new BigDecimal("1.5")))
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_003"));
    }

    @Test
    void shouldCalculateItemCount() {
        OrderItem item1 = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        OrderItem item2 = OrderItem.of("prod-002", "Product B", 3, Money.of(30, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item1, item2));
        
        assertThat(order.getItemCount()).isEqualTo(5);
    }

    @Test
    void shouldReturnFinalAmountAsTotalWhenNoDiscount() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        assertThat(order.getFinalAmount()).isEqualTo(order.getTotalAmount());
    }

    @Test
    void shouldReturnUnmodifiableItemsList() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        
        List<OrderItem> items = order.getItems();
        
        assertThatThrownBy(() -> items.add(item))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
