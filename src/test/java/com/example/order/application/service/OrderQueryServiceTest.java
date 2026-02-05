package com.example.order.application.service;

import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderQueryPort queryPort;

    @InjectMocks
    private OrderQueryService service;

    @Test
    void shouldFindOrderById() {
        // Given
        String orderId = "order-001";
        Order order = createOrder(orderId);

        when(queryPort.findById(orderId)).thenReturn(Mono.just(order));

        // When & Then
        StepVerifier.create(service.findById(orderId))
            .assertNext(result -> {
                assertThat(result.getId()).isEqualTo(orderId);
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        // Given
        String orderId = "invalid-order";
        when(queryPort.findById(orderId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(service.findById(orderId))
            .verifyComplete();
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        // Given
        String customerId = "customer-001";
        Order order1 = createOrder("order-001");
        Order order2 = createOrder("order-002");

        when(queryPort.findByCustomerId(customerId)).thenReturn(Flux.just(order1, order2));

        // When & Then
        StepVerifier.create(service.findByCustomerId(customerId).collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxWhenCustomerHasNoOrders() {
        // Given
        String customerId = "customer-no-orders";
        when(queryPort.findByCustomerId(customerId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(service.findByCustomerId(customerId))
            .verifyComplete();
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        Order order = createOrder("order-001");

        when(queryPort.findByStatus(status)).thenReturn(Flux.just(order));

        // When & Then
        StepVerifier.create(service.findByStatus(status).collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(1);
            })
            .verifyComplete();
    }

    @Test
    void shouldFindAllOrdersWithPagination() {
        // Given
        int page = 0;
        int size = 10;
        Order order = createOrder("order-001");

        when(queryPort.findAll(page, size)).thenReturn(Flux.just(order));

        // When & Then
        StepVerifier.create(service.findAll(page, size).collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(1);
            })
            .verifyComplete();
    }

    @Test
    void shouldCountOrders() {
        // Given
        when(queryPort.count()).thenReturn(Mono.just(100L));

        // When & Then
        StepVerifier.create(service.count())
            .assertNext(count -> {
                assertThat(count).isEqualTo(100L);
            })
            .verifyComplete();
    }

    @Test
    void shouldFindOrderList() {
        // Given
        int page = 0;
        int size = 10;
        OrderListItemResult item = new OrderListItemResult(
            "order-001",
            "customer-001",
            OrderStatus.PENDING,
            new BigDecimal("100.00"),
            "USD",
            2,
            Instant.now(),
            Instant.now()
        );

        when(queryPort.findOrderList(page, size)).thenReturn(Flux.just(item));

        // When & Then
        StepVerifier.create(service.findOrderList(page, size).collectList())
            .assertNext(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).orderId()).isEqualTo("order-001");
            })
            .verifyComplete();
    }

    @Test
    void shouldFindCustomerStats() {
        // Given
        String customerId = "customer-001";
        CustomerOrderStatsResult stats = new CustomerOrderStatsResult(
            customerId,
            10L,
            3L,
            5L,
            2L,
            new BigDecimal("1000.00"),
            new BigDecimal("100.00"),
            "USD",
            Map.of(OrderStatus.PENDING, 2L, OrderStatus.DELIVERED, 5L),
            25L
        );

        when(queryPort.findCustomerStats(customerId)).thenReturn(Mono.just(stats));

        // When & Then
        StepVerifier.create(service.findCustomerStats(customerId))
            .assertNext(result -> {
                assertThat(result.customerId()).isEqualTo(customerId);
                assertThat(result.totalOrders()).isEqualTo(10L);
            })
            .verifyComplete();
    }

    @Test
    void shouldFindOrderSummary() {
        // Given
        String orderId = "order-001";
        OrderSummaryResult summary = new OrderSummaryResult(
            orderId,
            "customer-001",
            "test@example.com",
            OrderStatus.PENDING,
            new BigDecimal("100.00"),
            "USD",
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            2,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(queryPort.findOrderSummary(orderId)).thenReturn(Mono.just(summary));

        // When & Then
        StepVerifier.create(service.handle(new com.example.order.application.dto.OrderSummaryQuery(orderId)))
            .assertNext(result -> {
                assertThat(result.orderId()).isEqualTo(orderId);
            })
            .verifyComplete();
    }

    private Order createOrder(String orderId) {
        return Order.reconstitute(
            orderId,
            "customer-001",
            com.example.common.domain.valueobject.Email.of("test@example.com"),
            OrderStatus.PENDING,
            List.of(),
            com.example.common.domain.valueobject.Money.of(100, "USD"),
            null,
            Instant.now(),
            Instant.now()
        );
    }
}
