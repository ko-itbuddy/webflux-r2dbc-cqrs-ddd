package com.example.order.domain.model;

import com.example.common.domain.event.DomainEvent;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDomainEventsTest {

    @Test
    void shouldRegisterEventWhenCreatingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));

        List<DomainEvent> events = order.getDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(com.example.order.domain.event.OrderCreatedEvent.class);
    }

    @Test
    void shouldRegisterEventWhenConfirmingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.clearDomainEvents();

        order.confirm();

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(com.example.order.domain.event.OrderConfirmedEvent.class);
    }

    @Test
    void shouldRegisterEventWhenPayingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();
        order.clearDomainEvents();

        order.pay();

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(com.example.order.domain.event.OrderPaidEvent.class);
    }

    @Test
    void shouldRegisterEventWhenCancellingOrder() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.clearDomainEvents();

        order.cancel("Test cancellation");

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(com.example.order.domain.event.OrderCancelledEvent.class);
    }

    @Test
    void shouldClearAllDomainEvents() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));
        order.confirm();

        assertThat(order.getDomainEvents()).hasSize(2);

        order.clearDomainEvents();

        assertThat(order.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldAccumulateMultipleEvents() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));

        order.confirm();
        order.pay();

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(3);
    }

    @Test
    void shouldReturnUnmodifiableEventList() {
        OrderItem item = OrderItem.of("prod-001", "Product A", 1, Money.of(50, "USD"));
        Order order = Order.create("customer-001", Email.of("test@example.com"), List.of(item));

        List<DomainEvent> events = order.getDomainEvents();

        assertThatThrownBy(() -> events.add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
