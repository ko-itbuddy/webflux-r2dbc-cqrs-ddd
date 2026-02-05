package com.example.order.application.command.handler;

import com.example.order.application.in.command.ConfirmOrderCommand;
import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.application.port.out.DomainEventPublisher;
import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderHandlerTest {

    @Mock
    private OrderCommandPort commandPort;

    @Mock
    private OrderQueryPort queryPort;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ConfirmOrderHandler handler;

    @Test
    void shouldConfirmPendingOrder() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.PENDING);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));
        when(commandPort.save(any())).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new ConfirmOrderCommand(orderId)))
            .assertNext(result -> {
                assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        String orderId = "invalid-order";
        when(queryPort.findById(orderId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(handler.handle(new ConfirmOrderCommand(orderId)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_006");
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotInPendingStatus() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId, OrderStatus.CONFIRMED);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(handler.handle(new ConfirmOrderCommand(orderId)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) error).getErrorCode()).isEqualTo("ORDER_004");
            })
            .verify();
    }

    private Order createOrder(String orderId, OrderStatus status) {
        return Order.reconstitute(
            orderId,
            "customer-001",
            com.example.order.domain.order.valueobject.Email.of("test@example.com"),
            status,
            List.of(),
            com.example.order.domain.order.valueobject.Money.of(100, "USD"),
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }
}
