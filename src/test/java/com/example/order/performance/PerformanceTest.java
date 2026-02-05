package com.example.order.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.ExchangeResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
class PerformanceTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "rabbitConnectionFactory")
    private ConnectionFactory connectionFactory;

    @MockBean(name = "amqpTemplate")
    private AmqpTemplate amqpTemplate;

    @MockBean(name = "rabbitTransactionManager")
    private RabbitTransactionManager rabbitTransactionManager;

    private String validOrderRequestTemplate;

    @BeforeEach
    void setUp() {
        validOrderRequestTemplate = "{\"customerId\":\"customer-test-%d\",\"customerEmail\":\"customer%d@example.com\",\"items\":[{\"productId\":\"prod-001\",\"productName\":\"Product A\",\"quantity\":2,\"unitPrice\":100.00,\"currency\":\"USD\"}]}";
    }

    private Mono<Integer> createOrderRequest(int index) {
        String requestBody = String.format(validOrderRequestTemplate, index, index);
        try {
            int statusCode = webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .returnResult(DataBuffer.class)
                .getStatus()
                .value();
            return Mono.just(statusCode);
        } catch (Exception e) {
            return Mono.just(500);
        }
    }

    @Test
    void shouldHandleHighThroughputOrderCreation() {
        int numberOfOrders = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        Instant startTime = Instant.now();

        Flux.range(0, numberOfOrders)
            .flatMap(index -> 
                createOrderRequest(index)
                    .map(statusCode -> {
                        if (statusCode >= 200 && statusCode < 300) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        return statusCode;
                    }), 20)
            .collectList()
            .block(Duration.ofSeconds(30));

        Instant endTime = Instant.now();
        long durationMs = Duration.between(startTime, endTime).toMillis();
        double throughputPerSecond = (numberOfOrders * 1000.0) / durationMs;

        assertThat(successCount.get())
            .as("All requests should succeed")
            .isEqualTo(numberOfOrders);

        assertThat(throughputPerSecond)
            .as("Should handle at least 50 orders/sec (smoke test)")
            .isGreaterThanOrEqualTo(50.0);
    }

    @Test
    void shouldMaintainResponseTimeUnderLoad() {
        int numberOfOrders = 100;
        List<Long> latencies = new ArrayList<>();

        Flux.range(0, numberOfOrders)
            .flatMap(index -> {
                Instant requestStartTime = Instant.now();
                String requestBody = String.format(validOrderRequestTemplate, index, index);
                return Mono.defer(() -> {
                    try {
                        int statusCode = webTestClient.post()
                            .uri("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .exchange()
                            .returnResult(DataBuffer.class)
                            .getStatus()
                            .value();
                        
                        if (statusCode >= 200 && statusCode < 300) {
                            long latency = Duration.between(requestStartTime, Instant.now()).toMillis();
                            latencies.add(latency);
                        }
                        return Mono.just(statusCode);
                    } catch (Exception e) {
                        return Mono.just(500);
                    }
                });
            }, 20)
            .collectList()
            .block(Duration.ofSeconds(30));

        assertThat(latencies)
            .as("Should have collected latencies from all successful requests")
            .hasSize(numberOfOrders);

        long avgLatency = latencies.stream()
            .mapToLong(l -> l)
            .sum() / latencies.size();

        List<Long> sortedLatencies = latencies.stream()
            .sorted()
            .collect(Collectors.toList());

        int p95Index = (int) Math.ceil(sortedLatencies.size() * 0.95) - 1;
        long p95Latency = sortedLatencies.get(p95Index);

        assertThat(avgLatency)
            .as("Average latency should be reasonable")
            .isLessThan(500L);

        assertThat(p95Latency)
            .as("P95 latency should be under 500ms")
            .isLessThan(500L);
    }

    @Test
    void shouldNotExceedMemoryThreshold() {
        Runtime runtime = Runtime.getRuntime();

        System.gc();

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        int numberOfOrders = 100;
        AtomicInteger successCount = new AtomicInteger(0);

        Flux.range(0, numberOfOrders)
            .flatMap(index -> 
                createOrderRequest(index)
                    .doOnNext(statusCode -> {
                        if (statusCode >= 200 && statusCode < 300) {
                            successCount.incrementAndGet();
                        }
                    }), 20)
            .collectList()
            .block(Duration.ofSeconds(30));

        System.gc();

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        assertThat(successCount.get())
            .as("All requests should succeed")
            .isEqualTo(numberOfOrders);

        assertThat(memoryIncrease)
            .as("Memory increase should be reasonable (under 100MB)")
            .isLessThan(100 * 1024 * 1024);
    }

    @Test
    void shouldHandleBurstTraffic() {
        int initialConcurrency = 10;
        int burstConcurrency = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Flux.range(0, initialConcurrency)
            .flatMap(index -> 
                createOrderRequest(index)
                    .doOnNext(statusCode -> {
                        if (statusCode >= 200 && statusCode < 300) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }), 10)
            .collectList()
            .block(Duration.ofSeconds(30));

        Flux.range(0, burstConcurrency)
            .flatMap(index -> 
                createOrderRequest(index + initialConcurrency)
                    .doOnNext(statusCode -> {
                        if (statusCode >= 200 && statusCode < 300) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }), 50)
            .collectList()
            .block(Duration.ofSeconds(60));

        int totalExpected = initialConcurrency + burstConcurrency;

        assertThat(successCount.get())
            .as("At least 90%% of requests should succeed during burst")
            .isGreaterThan((int) (totalExpected * 0.9));
    }

    @Test
    void shouldRecoverAfterStress() {
        int stressIterations = 200;
        int recoveryIterations = 100;

        List<List<Long>> allLatencies = new ArrayList<>();

        for (int phase = 0; phase < 2; phase++) {
            final int iterations = (phase == 0) ? stressIterations : recoveryIterations;
            final int offset = phase * stressIterations;
            List<Long> latencies = new ArrayList<>();

            Flux.range(0, iterations)
                .flatMap(index -> {
                    Instant requestStartTime = Instant.now();
                    String requestBody = String.format(validOrderRequestTemplate, index + offset, index + offset);
                    return Mono.defer(() -> {
                        try {
                            int statusCode = webTestClient.post()
                                .uri("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestBody)
                                .exchange()
                                .returnResult(DataBuffer.class)
                                .getStatus()
                                .value();
                            
                            if (statusCode >= 200 && statusCode < 300) {
                                long latency = Duration.between(requestStartTime, Instant.now()).toMillis();
                                latencies.add(latency);
                            }
                            return Mono.just(statusCode);
                        } catch (Exception e) {
                            return Mono.just(500);
                        }
                    });
                }, 20)
                .collectList()
                .block(Duration.ofSeconds(30));

            allLatencies.add(latencies);
        }

        assertThat(allLatencies.get(0))
            .as("Stress phase should complete")
            .hasSize(stressIterations);

        assertThat(allLatencies.get(1))
            .as("Recovery phase should complete")
            .hasSize(recoveryIterations);

        long stressAvgLatency = allLatencies.get(0).stream()
            .mapToLong(l -> l)
            .sum() / allLatencies.get(0).size();

        long recoveryAvgLatency = allLatencies.get(1).stream()
            .mapToLong(l -> l)
            .sum() / allLatencies.get(1).size();

        assertThat(recoveryAvgLatency)
            .as("System should recover with similar or better latency")
            .isLessThan(stressAvgLatency * 2);
    }
}