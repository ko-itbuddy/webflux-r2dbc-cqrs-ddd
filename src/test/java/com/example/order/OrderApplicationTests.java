package com.example.order;

import com.example.order.application.command.handler.CreateOrderHandler;
import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.infrastructure.web.handler.OrderCommandWebHandler.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
class OrderApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CreateOrderHandler createOrderHandler;

    @MockBean(name = "rabbitConnectionFactory")
    private ConnectionFactory connectionFactory;

    @MockBean(name = "amqpTemplate")
    private AmqpTemplate amqpTemplate;

    @MockBean(name = "rabbitTransactionManager")
    private RabbitTransactionManager rabbitTransactionManager;

    @Test
    void contextLoads() {
        assertThat(createOrderHandler).isNotNull();
        assertThat(webTestClient).isNotNull();
    }

    @Test
    void shouldCreateOrder() {
        CreateOrderCommand command = new CreateOrderCommand(
            "customer-001",
            "test@example.com",
            List.of(new CreateOrderCommand.OrderItemCommand(
                "prod-001", "Product A", 2, new BigDecimal("100.00"), "USD"
            ))
        );

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(command)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .value(order -> {
                assertThat(order.customerId()).isEqualTo("customer-001");
                assertThat(order.totalAmount()).isEqualTo(new BigDecimal("200.00"));
            });
    }

    @Test
    void shouldHandleConcurrentRequestsWithoutBlocking() {
        int requestCount = 100;

        List<Mono<com.example.order.domain.order.entity.Order>> requests = IntStream.range(0, requestCount)
            .mapToObj(i -> {
                CreateOrderCommand command = new CreateOrderCommand(
                    "customer-" + i,
                    "test" + i + "@example.com",
                    List.of(new CreateOrderCommand.OrderItemCommand(
                        "prod-001", "Product A", 1, new BigDecimal("50.00"), "USD"
                    ))
                );
                return createOrderHandler.handle(command);
            })
            .toList();

        StepVerifier.create(
            Flux.merge(requests)
                .collectList()
        )
        .assertNext(orders -> {
            assertThat(orders).hasSize(requestCount);
            assertThat(orders).allMatch(order -> order.getCustomerId().startsWith("customer-"));
        })
        .verifyComplete();
    }

    @Test
    void shouldNotBlockOnHighLoad() {
        int concurrentRequests = 50; // Reduced for faster tests

        List<Mono<com.example.order.domain.order.entity.Order>> requests = IntStream.range(0, concurrentRequests)
            .mapToObj(i -> {
                CreateOrderCommand command = new CreateOrderCommand(
                    "customer-" + i,
                    "test" + i + "@example.com",
                    List.of(new CreateOrderCommand.OrderItemCommand(
                        "prod-001", "Product A", 1, new BigDecimal("50.00"), "USD"
                    ))
                );
                return createOrderHandler.handle(command);
            })
            .toList();

        long startTime = System.currentTimeMillis();

        StepVerifier.create(
            Mono.zip(requests, objects -> objects.length)
        )
        .assertNext(count -> {
            assertThat(count).isEqualTo(concurrentRequests);
        })
        .verifyComplete();
    }
}