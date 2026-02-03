package com.example.order;

import com.example.order.application.command.handler.CreateOrderHandler;
import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.domain.order.entity.Order;
import com.example.order.infrastructure.web.handler.OrderCommandWebHandler.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
class OrderApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CreateOrderHandler createOrderHandler;

    @Test
    void contextLoads() {
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
    void shouldHandleConcurrentRequestsWithoutBlocking() throws InterruptedException {
        int requestCount = 1000;
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        IntStream.range(0, requestCount).parallel().forEach(i -> {
            CreateOrderCommand command = new CreateOrderCommand(
                "customer-" + i,
                "test" + i + "@example.com",
                List.of(new CreateOrderCommand.OrderItemCommand(
                    "prod-001", "Product A", 1, new BigDecimal("50.00"), "USD"
                ))
            );

            createOrderHandler.handle(command)
                .doFinally(signal -> latch.countDown())
                .subscribe(
                    order -> successCount.incrementAndGet(),
                    error -> {
                        errorCount.incrementAndGet();
                        System.err.println("Error: " + error.getMessage());
                    }
                );
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        System.out.println("=== Concurrent Test Results ===");
        System.out.println("Total requests: " + requestCount);
        System.out.println("Success: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Time: " + (endTime - startTime) + "ms");
        System.out.println("Throughput: " + (requestCount * 1000.0 / (endTime - startTime)) + " req/sec");
        System.out.println("Completed within timeout: " + completed);

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(requestCount);
        assertThat(errorCount.get()).isEqualTo(0);
    }

    @Test
    void shouldNotBlockOnHighLoad() {
        int concurrentRequests = 500;

        List<Mono<Order>> requests = IntStream.range(0, concurrentRequests)
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
            long endTime = System.currentTimeMillis();
            System.out.println("=== High Load Test ===");
            System.out.println("Concurrent requests: " + concurrentRequests);
            System.out.println("Completed: " + count);
            System.out.println("Time: " + (endTime - startTime) + "ms");
            assertThat(count).isEqualTo(concurrentRequests);
        })
        .verifyComplete();
    }
}
