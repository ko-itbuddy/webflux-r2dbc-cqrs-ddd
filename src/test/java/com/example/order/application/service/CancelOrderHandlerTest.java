package com.example.order.application.service;

import com.example.order.application.dto.CancelOrderCommand;
import com.example.order.application.port.out.OrderRepository;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.domain.model.Order;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelOrderHandlerTest {

    @Mock
    private OrderRepository commandPort;

    @Mock
    private OrderQueryPort queryPort;

    @InjectMocks
    private CancelOrderHandler handler;

    @Test
    void shouldCancelPendingOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.PENDING);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));
        when(commandPort.save(any())).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new CancelOrderCommand(orderId, "Customer request")))
            .assertNext(result -> {
                assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            })
            .verifyComplete();
    }

    @Test
    void shouldCancelConfirmedOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.CONFIRMED);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));
        when(commandPort.save(any())).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new CancelOrderCommand(orderId, "Out of stock")))
            .assertNext(result -> {
                assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "invalid-order";
        when(queryPort.findById(orderId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(handler.handle(new CancelOrderCommand(orderId, "reason")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(IllegalArgumentException.class);
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenCancellingShippedOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.SHIPPED);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new CancelOrderCommand(orderId, "Too late")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_008");
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenCancellingDeliveredOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.DELIVERED);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new CancelOrderCommand(orderId, "Too late")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_008");
            })
            .verify();
    }

    private Order createOrder(String orderId, OrderStatus status) {
        return Order.reconstitute(
            orderId,
            "customer-001",
            Email.of("test@example.com"),
            status,
            List.of(),
            Money.of(100, "USD"),
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }
}