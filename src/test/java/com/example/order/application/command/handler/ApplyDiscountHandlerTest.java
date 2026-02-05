package com.example.order.application.command.handler;

import com.example.order.application.in.command.ApplyDiscountCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyDiscountHandlerTest {

    @Mock
    private OrderCommandPort commandPort;

    @Mock
    private OrderQueryPort queryPort;

    @InjectMocks
    private ApplyDiscountHandler handler;

    @Test
    void shouldApplyDiscountToPendingOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.PENDING, Money.of(100, "USD"));

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));
        when(commandPort.save(any())).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new ApplyDiscountCommand(orderId, new BigDecimal("0.1"))))
            .assertNext(result -> {
                assertThat(result.getDiscountAmount()).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "invalid-order";
        when(queryPort.findById(orderId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(handler.handle(new ApplyDiscountCommand(orderId, new BigDecimal("0.1"))))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenApplyingDiscountToNonPendingOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.CONFIRMED, Money.of(100, "USD"));

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new ApplyDiscountCommand(orderId, new BigDecimal("0.1"))))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_002");
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenApplyingInvalidDiscount() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.PENDING, Money.of(100, "USD"));

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then - negative discount
        StepVerifier.create(handler.handle(new ApplyDiscountCommand(orderId, new BigDecimal("-0.1"))))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_003");
            })
            .verify();

        // When & Then - discount > 100%
        StepVerifier.create(handler.handle(new ApplyDiscountCommand(orderId, new BigDecimal("1.5"))))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_003");
            })
            .verify();
    }

    private Order createOrder(String orderId, OrderStatus status, Money totalAmount) {
        return Order.reconstitute(
            orderId,
            "customer-001",
            Email.of("test@example.com"),
            status,
            List.of(),
            totalAmount,
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }
}
