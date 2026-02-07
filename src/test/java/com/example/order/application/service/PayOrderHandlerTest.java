package com.example.order.application.service;

import com.example.order.application.dto.PayOrderCommand;
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
class PayOrderHandlerTest {

    @Mock
    private OrderRepository commandPort;

    @Mock
    private OrderQueryPort queryPort;

    @InjectMocks
    private PayOrderHandler handler;

    @Test
    void shouldPayConfirmedOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.CONFIRMED);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));
        when(commandPort.save(any())).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new PayOrderCommand(orderId)))
            .assertNext(result -> {
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "invalid-order";
        when(queryPort.findById(orderId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(handler.handle(new PayOrderCommand(orderId)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(IllegalArgumentException.class);
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenPayingNonConfirmedOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.PENDING);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new PayOrderCommand(orderId)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_005");
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