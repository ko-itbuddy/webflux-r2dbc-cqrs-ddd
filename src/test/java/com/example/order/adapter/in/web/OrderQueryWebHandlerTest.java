package com.example.order.adapter.in.web.handler;

import com.example.order.domain.model.Order;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import com.example.order.domain.model.OrderStatus;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
class OrderQueryWebHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderQueryPort orderQueryPort;

    @MockBean(name = "rabbitConnectionFactory")
    private ConnectionFactory connectionFactory;

    @MockBean(name = "amqpTemplate")
    private AmqpTemplate amqpTemplate;

    @MockBean(name = "rabbitTransactionManager")
    private RabbitTransactionManager rabbitTransactionManager;

    private Order testOrder;
    private String testOrderId;

    @BeforeEach
    void setUp() {
        testOrder = Order.create(
            "customer-001",
            Email.of("test@example.com"),
            List.of(com.example.order.domain.model.OrderItem.of(
                "prod-001",
                "Product A",
                2,
                Money.of(BigDecimal.valueOf(100), "USD")
            ))
        );
        testOrderId = testOrder.getId();
    }

    @Test
    void shouldGetOrderById() {
        when(orderQueryPort.findById(testOrderId)).thenReturn(Mono.just(testOrder));

        webTestClient.get()
            .uri("/api/orders/" + testOrderId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderQueryWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.customerId()).isEqualTo("customer-001");
                assertThat(response.customerEmail()).isEqualTo("test@example.com");
                assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
                assertThat(response.totalAmount().compareTo(BigDecimal.valueOf(200))).isEqualTo(0);
            });
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        when(orderQueryPort.findById("non-existent")).thenReturn(Mono.empty());

        webTestClient.get()
            .uri("/api/orders/non-existent")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void shouldListOrders() {
        OrderListItemResult item1 = new OrderListItemResult(
            testOrder.getId(),
            testOrder.getCustomerId(),
            testOrder.getStatus(),
            testOrder.getFinalAmount().getAmount(),
            testOrder.getTotalAmount().getCurrency(),
            testOrder.getItemCount(),
            testOrder.getCreatedAt(),
            testOrder.getUpdatedAt()
        );

        OrderListItemResult item2 = new OrderListItemResult(
            "order-002",
            "customer-002",
            OrderStatus.PENDING,
            BigDecimal.valueOf(50),
            "USD",
            1,
            Instant.now(),
            Instant.now()
        );

        when(orderQueryPort.findOrderList(0, 10))
            .thenReturn(Flux.just(item1, item2));

        webTestClient.get()
            .uri("/api/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderQueryWebHandler.OrderListResponse.class)
            .value(response -> {
                assertThat(response.orders()).hasSize(2);
                assertThat(response.count()).isEqualTo(2);
            });
    }

    @Test
    void shouldGetOrdersByCustomer() {
        when(orderQueryPort.findByCustomerId("customer-001"))
            .thenReturn(Flux.just(testOrder));

        webTestClient.get()
            .uri("/api/customers/customer-001/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(OrderQueryWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response).hasSize(1);
                assertThat(response.get(0).customerId()).isEqualTo("customer-001");
            });
    }

    @Test
    void shouldGetOrdersByStatus() {
        when(orderQueryPort.findByStatus(OrderStatus.PENDING))
            .thenReturn(Flux.just(testOrder));

        webTestClient.get()
            .uri("/api/orders/by-status/" + OrderStatus.PENDING.name())
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(OrderQueryWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response).hasSize(1);
                assertThat(response.get(0).status()).isEqualTo(OrderStatus.PENDING.name());
            });
    }

    @Test
    void shouldGetOrderSummary() {
        testOrder.applyDiscount(BigDecimal.valueOf(0.10));

        OrderSummaryResult.OrderItemSummary itemSummary = new OrderSummaryResult.OrderItemSummary(
            "prod-001",
            "Product A",
            2,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(200)
        );

        OrderSummaryResult summary = new OrderSummaryResult(
            testOrderId,
            testOrder.getCustomerId(),
            testOrder.getCustomerEmail().getValue(),
            testOrder.getStatus(),
            testOrder.getTotalAmount().getAmount(),
            testOrder.getTotalAmount().getCurrency(),
            testOrder.getDiscountAmount() != null ? testOrder.getDiscountAmount().getAmount() : null,
            testOrder.getFinalAmount().getAmount(),
            BigDecimal.valueOf(20),
            2,
            List.of(itemSummary),
            testOrder.getCreatedAt(),
            testOrder.getUpdatedAt()
        );

        when(orderQueryPort.findOrderSummary(testOrderId))
            .thenReturn(Mono.just(summary));

        webTestClient.get()
            .uri("/api/orders/" + testOrderId + "/summary")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderSummaryResult.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.itemCount()).isEqualTo(2);
                assertThat(response.items()).hasSize(1);
                assertThat(response.hasDiscount()).isTrue();
                assertThat(response.getSavingsAmount().compareTo(BigDecimal.valueOf(20))).isEqualTo(0);
            });
    }

    @Test
    void shouldGetCustomerStats() {
        CustomerOrderStatsResult stats = new CustomerOrderStatsResult(
            "customer-001",
            5,
            2,
            2,
            1,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(200),
            "USD",
            java.util.Map.of(
                OrderStatus.PENDING, 2L,
                OrderStatus.PAID, 2L,
                OrderStatus.CANCELLED, 1L
            ),
            10
        );

        when(orderQueryPort.findCustomerStats("customer-001"))
            .thenReturn(Mono.just(stats));

        webTestClient.get()
            .uri("/api/customers/customer-001/stats")
            .exchange()
            .expectStatus().isOk()
            .expectBody(CustomerOrderStatsResult.class)
            .value(response -> {
                assertThat(response.customerId()).isEqualTo("customer-001");
                assertThat(response.totalOrders()).isEqualTo(5);
                assertThat(response.totalSpent().compareTo(BigDecimal.valueOf(1000))).isEqualTo(0);
                assertThat(response.averageOrderValue().compareTo(BigDecimal.valueOf(200))).isEqualTo(0);
            });
    }

    @Test
    void shouldReturn404ForNonExistentOrderSummary() {
        when(orderQueryPort.findOrderSummary("non-existent"))
            .thenReturn(Mono.empty());

        webTestClient.get()
            .uri("/api/orders/non-existent/summary")
            .exchange()
            .expectStatus().isNotFound();
    }
}
