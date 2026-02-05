package com.example.order.domain.model;

import com.example.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    void shouldCreateOrderItem() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 2, Money.of(50, "USD"));
        
        assertThat(item.getProductId()).isEqualTo("prod-001");
        assertThat(item.getProductName()).isEqualTo("Product A");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice().getAmount()).isEqualByComparingTo(new BigDecimal("50"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsZero() {
        assertThatThrownBy(() -> OrderItem.of("prod-001", "Product A", 0, Money.of(50, "USD")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsNegative() {
        assertThatThrownBy(() -> OrderItem.of("prod-001", "Product A", -1, Money.of(50, "USD")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void shouldCalculateSubtotal() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 3, Money.of(50, "USD"));
        
        Money subtotal = item.calculateSubtotal();
        
        assertThat(subtotal.getAmount()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(subtotal.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldCalculateSubtotalWithQuantityOne() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(100, "USD"));
        
        Money subtotal = item.calculateSubtotal();
        
        assertThat(subtotal.getAmount()).isEqualByComparingTo(new BigDecimal("100"));
    }
}
