package com.example.order.adapter.in.web.handler;

import com.example.order.application.service.ApplyDiscountHandler;
import com.example.order.application.service.CancelOrderHandler;
import com.example.order.application.service.ConfirmOrderHandler;
import com.example.order.application.service.CreateOrderHandler;
import com.example.order.application.service.PayOrderHandler;
import com.example.order.application.dto.ApplyDiscountCommand;
import com.example.order.application.dto.CancelOrderCommand;
import com.example.order.application.dto.ConfirmOrderCommand;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.PayOrderCommand;
import com.example.order.domain.model.Order;
import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import com.example.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
class OrderCommandWebHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreateOrderHandler createOrderHandler;

    @MockBean
    private ConfirmOrderHandler confirmOrderHandler;

    @MockBean
    private PayOrderHandler payOrderHandler;

    @MockBean
    private CancelOrderHandler cancelOrderHandler;

    @MockBean
    private ApplyDiscountHandler applyDiscountHandler;

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
    void shouldCreateOrder() {
        when(createOrderHandler.handle(any(CreateOrderCommand.class))).thenReturn(Mono.just(testOrder));

        String requestBody = """
            {
                "customerId": "customer-001",
                "customerEmail": "test@example.com",
                "items": [
                    {
                        "productId": "prod-001",
                        "productName": "Product A",
                        "quantity": 2,
                        "unitPrice": 100.00,
                        "currency": "USD"
                    }
                ]
            }
        """;

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderCommandWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isNotEmpty();
                assertThat(response.customerId()).isEqualTo("customer-001");
                assertThat(response.customerEmail()).isEqualTo("test@example.com");
                assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
                assertThat(response.totalAmount()).isEqualTo(BigDecimal.valueOf(200));
                assertThat(response.finalAmount()).isEqualTo(BigDecimal.valueOf(200));
            });

        ArgumentCaptor<CreateOrderCommand> captor = ArgumentCaptor.forClass(CreateOrderCommand.class);
        verify(createOrderHandler).handle(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo("customer-001");
        assertThat(captor.getValue().customerEmail()).isEqualTo("test@example.com");
        assertThat(captor.getValue().items()).hasSize(1);
    }

    @Test
    void shouldReturn400ForInvalidRequest() {
        when(createOrderHandler.handle(any(CreateOrderCommand.class)))
            .thenReturn(Mono.error(new IllegalArgumentException("Invalid email format")));

        String requestBody = """
            {
                "customerId": "customer-001",
                "customerEmail": "invalid-email",
                "items": [
                    {
                        "productId": "prod-001",
                        "productName": "Product A",
                        "quantity": 2,
                        "unitPrice": 100.00,
                        "currency": "USD"
                    }
                ]
            }
        """;

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(OrderCommandWebHandler.ErrorResponse.class)
            .value(response -> {
                assertThat(response.error()).isNotNull();
            });
    }

    @Test
    void shouldReturn400ForMalformedJson() {
        String malformedJson = "{invalid json}";

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(malformedJson)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn404WhenOrderNotFoundForConfirm() {
        when(confirmOrderHandler.handle(any(ConfirmOrderCommand.class)))
            .thenReturn(Mono.error(new RuntimeException("Order not found")));

        webTestClient.post()
            .uri("/api/orders/non-existent-id/confirm")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(OrderCommandWebHandler.ErrorResponse.class)
            .value(response -> {
                assertThat(response.error()).isNotNull();
            });
    }

    @Test
    void shouldConfirmOrderSuccessfully() {
        testOrder.confirm();
        when(confirmOrderHandler.handle(any(ConfirmOrderCommand.class))).thenReturn(Mono.just(testOrder));

        webTestClient.post()
            .uri("/api/orders/" + testOrderId + "/confirm")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderCommandWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED.name());
            });

        ArgumentCaptor<ConfirmOrderCommand> captor = ArgumentCaptor.forClass(ConfirmOrderCommand.class);
        verify(confirmOrderHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(testOrderId);
    }

    @Test
    void shouldPayOrderSuccessfully() {
        testOrder.confirm();
        testOrder.pay();
        when(payOrderHandler.handle(any(PayOrderCommand.class))).thenReturn(Mono.just(testOrder));

        webTestClient.post()
            .uri("/api/orders/" + testOrderId + "/pay")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderCommandWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.status()).isEqualTo(OrderStatus.PAID.name());
            });

        ArgumentCaptor<PayOrderCommand> captor = ArgumentCaptor.forClass(PayOrderCommand.class);
        verify(payOrderHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(testOrderId);
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        testOrder.cancel("Customer request");
        when(cancelOrderHandler.handle(any(CancelOrderCommand.class))).thenReturn(Mono.just(testOrder));

        webTestClient.post()
            .uri("/api/orders/" + testOrderId + "/cancel")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderCommandWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED.name());
            });

        ArgumentCaptor<CancelOrderCommand> captor = ArgumentCaptor.forClass(CancelOrderCommand.class);
        verify(cancelOrderHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(testOrderId);
        assertThat(captor.getValue().reason()).isEqualTo("Customer request");
    }

    @Test
    void shouldApplyDiscountSuccessfully() {
        testOrder.applyDiscount(BigDecimal.valueOf(0.10));
        when(applyDiscountHandler.handle(any(ApplyDiscountCommand.class))).thenReturn(Mono.just(testOrder));

        String requestBody = """
            {
                "discountPercentage": 0.10
            }
        """;

        webTestClient.post()
            .uri("/api/orders/" + testOrderId + "/discount")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderCommandWebHandler.OrderResponse.class)
            .value(response -> {
                assertThat(response.orderId()).isEqualTo(testOrderId);
                assertThat(response.discountAmount().compareTo(BigDecimal.valueOf(180))).isEqualTo(0);
                assertThat(response.finalAmount().compareTo(BigDecimal.valueOf(180))).isEqualTo(0);
            });

        ArgumentCaptor<ApplyDiscountCommand> captor = ArgumentCaptor.forClass(ApplyDiscountCommand.class);
        verify(applyDiscountHandler).handle(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(testOrderId);
        assertThat(captor.getValue().discountPercentage().compareTo(BigDecimal.valueOf(0.10))).isEqualTo(0);
    }
}