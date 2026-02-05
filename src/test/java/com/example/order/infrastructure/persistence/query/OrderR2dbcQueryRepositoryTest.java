package com.example.order.infrastructure.persistence.query;

import com.example.order.application.query.result.CustomerOrderStatsResult;
import com.example.order.application.query.result.OrderSummaryResult;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderR2dbcQueryRepositoryTest {

    @Autowired
    private OrderR2dbcQueryRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM order_items").then()
            .then(databaseClient.sql("DELETE FROM orders").then())
            .block();
    }

    @Test
    void shouldFindOrderById() {
        String orderId = "order-001";
        String customerId = "customer-001";
        String email = "test@example.com";
        Instant createdAt = Instant.now();
        
        createTestOrder(orderId, customerId, email, "PENDING", new BigDecimal("199.98"), "USD", createdAt)
            .then(createTestOrderItem(orderId, "PROD-001", "Product 1", 2, new BigDecimal("99.99")))
            .then(createTestOrderItem(orderId, "PROD-002", "Product 2", 1, new BigDecimal("50.00")))
            .block();

        StepVerifier.create(repository.findById(orderId))
            .assertNext(order -> {
                assertThat(order.getId()).isEqualTo(orderId);
                assertThat(order.getCustomerId()).isEqualTo(customerId);
                assertThat(order.getCustomerEmail().getValue()).isEqualTo(email);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(order.getTotalAmount().getAmount()).isEqualTo(new BigDecimal("199.98"));
                assertThat(order.getTotalAmount().getCurrency()).isEqualTo("USD");
                assertThat(order.getItems()).hasSize(2);
                
                assertThat(order.getItems().get(0).getProductId()).isEqualTo("PROD-001");
                assertThat(order.getItems().get(0).getProductName()).isEqualTo("Product 1");
                assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
                assertThat(order.getItems().get(0).getUnitPrice().getAmount()).isEqualTo(new BigDecimal("99.99"));
                
                assertThat(order.getItems().get(1).getProductId()).isEqualTo("PROD-002");
                assertThat(order.getItems().get(1).getProductName()).isEqualTo("Product 2");
                assertThat(order.getItems().get(1).getQuantity()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        StepVerifier.create(repository.findById("non-existent-id"))
            .verifyComplete();
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        String customerId = "customer-002";
        Instant createdAt = Instant.now();
        
        createTestOrder("order-101", customerId, "email1@example.com", "PENDING", new BigDecimal("50.00"), "USD", createdAt)
            .then(createTestOrderItem("order-101", "PROD-101", "Product A", 1, new BigDecimal("50.00")))
            .then(createTestOrder("order-102", customerId, "email2@example.com", "CONFIRMED", new BigDecimal("75.00"), "USD", createdAt))
            .then(createTestOrderItem("order-102", "PROD-102", "Product B", 1, new BigDecimal("75.00")))
            .then(createTestOrder("order-103", "other-customer", "other@example.com", "PENDING", new BigDecimal("100.00"), "USD", createdAt))
            .block();

        StepVerifier.create(repository.findByCustomerId(customerId).collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(2);
                assertThat(orders.stream().map(Order::getCustomerId)).allMatch(id -> id.equals(customerId));
                assertThat(orders.stream().map(Order::getId)).containsExactlyInAnyOrder("order-101", "order-102");
            })
            .verifyComplete();
    }

    @Test
    void shouldFindOrdersByStatus() {
        Instant createdAt = Instant.now();
        
        createTestOrder("order-201", "customer-201", "email1@example.com", "PENDING", new BigDecimal("50.00"), "USD", createdAt)
            .then(createTestOrderItem("order-201", "PROD-201", "Product A", 1, new BigDecimal("50.00")))
            .then(createTestOrder("order-202", "customer-202", "email2@example.com", "PENDING", new BigDecimal("75.00"), "USD", createdAt))
            .then(createTestOrderItem("order-202", "PROD-202", "Product B", 1, new BigDecimal("75.00")))
            .then(createTestOrder("order-203", "customer-203", "email3@example.com", "CONFIRMED", new BigDecimal("100.00"), "USD", createdAt))
            .then(createTestOrderItem("order-203", "PROD-203", "Product C", 1, new BigDecimal("100.00")))
            .block();

        StepVerifier.create(repository.findByStatus(OrderStatus.PENDING).collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(2);
                assertThat(orders).allMatch(order -> order.getStatus().equals(OrderStatus.PENDING));
                assertThat(orders.stream().map(Order::getId)).containsExactlyInAnyOrder("order-201", "order-202");
            })
            .verifyComplete();
    }

    @Test
    void shouldFindAllOrdersWithPagination() {
        Instant createdAt = Instant.now();
        
        for (int i = 1; i <= 5; i++) {
            String orderId = String.format("order-%03d", 300 + i);
            String customerId = String.format("customer-%03d", 300 + i);
            createTestOrder(orderId, customerId, "email@example.com", "PENDING", new BigDecimal("10.00"), "USD", createdAt)
                .then(createTestOrderItem(orderId, "PROD-" + i, "Product " + i, 1, new BigDecimal("10.00")))
                .block();
        }

        StepVerifier.create(repository.findAll(0, 2).collectList())
            .assertNext(orders -> assertThat(orders).hasSize(2))
            .verifyComplete();

        StepVerifier.create(repository.findAll(1, 2).collectList())
            .assertNext(orders -> assertThat(orders).hasSize(2))
            .verifyComplete();

        StepVerifier.create(repository.findAll(2, 2).collectList())
            .assertNext(orders -> assertThat(orders).hasSize(1))
            .verifyComplete();
    }

    @Test
    void shouldCountOrders() {
        Instant createdAt = Instant.now();
        
        createTestOrder("order-401", "customer-401", "email1@example.com", "PENDING", new BigDecimal("10.00"), "USD", createdAt)
            .then(createTestOrderItem("order-401", "PROD-401", "Product A", 1, new BigDecimal("10.00")))
            .then(createTestOrder("order-402", "customer-402", "email2@example.com", "CONFIRMED", new BigDecimal("20.00"), "USD", createdAt))
            .then(createTestOrderItem("order-402", "PROD-402", "Product B", 1, new BigDecimal("20.00")))
            .then(createTestOrder("order-403", "customer-403", "email3@example.com", "DELIVERED", new BigDecimal("30.00"), "USD", createdAt))
            .then(createTestOrderItem("order-403", "PROD-403", "Product C", 1, new BigDecimal("30.00")))
            .block();

        StepVerifier.create(repository.count())
            .assertNext(count -> assertThat(count).isEqualTo(3L))
            .verifyComplete();
    }

    @Test
    @org.junit.jupiter.api.Disabled("H2 R2DBC type conversion issue: Cannot decode value of type java.time.Instant in complex join queries")
    void shouldFindOrderSummary() {
        String orderId = "order-501";
        String customerId = "customer-501";
        String email = "summary@example.com";
        Instant now = Instant.now();

        createTestOrder(orderId, customerId, email, "CONFIRMED", new BigDecimal("149.97"), "USD", now, now)
            .then(createTestOrderItem(orderId, "PROD-501", "Summary Product 1", 2, new BigDecimal("49.99")))
            .then(createTestOrderItem(orderId, "PROD-502", "Summary Product 2", 1, new BigDecimal("49.99")))
            .block();

        StepVerifier.create(repository.findOrderSummary(orderId))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    @org.junit.jupiter.api.Disabled("H2 R2DBC type conversion issue: Cannot decode value of type java.math.BigDecimal in aggregation queries")
    void shouldFindCustomerStats() {
        String customerId = "customer-600";
        Instant createdAt = Instant.now();
        
        createTestOrder("order-601", customerId, "email1@example.com", "PENDING", new BigDecimal("50.00"), "USD", createdAt)
            .then(createTestOrderItem("order-601", "PROD-601", "Product A", 1, new BigDecimal("50.00")))
            .then(createTestOrder("order-602", customerId, "email2@example.com", "CONFIRMED", new BigDecimal("75.00"), "USD", createdAt))
            .then(createTestOrderItem("order-602", "PROD-602", "Product B", 2, new BigDecimal("37.50")))
            .then(createTestOrder("order-603", customerId, "email3@example.com", "DELIVERED", new BigDecimal("100.00"), "USD", createdAt))
            .then(createTestOrderItem("order-603", "PROD-603", "Product C", 1, new BigDecimal("100.00")))
            .then(createTestOrder("order-604", customerId, "email4@example.com", "CANCELLED", new BigDecimal("25.00"), "USD", createdAt))
            .then(createTestOrderItem("order-604", "PROD-604", "Product D", 1, new BigDecimal("25.00")))
            .block();

        StepVerifier.create(repository.findCustomerStats(customerId))
            .expectNextCount(1)
            .verifyComplete();
    }

    private reactor.core.publisher.Mono<Void> createTestOrder(
            String id,
            String customerId,
            String email,
            String status,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt) {
        return databaseClient.sql("""
            INSERT INTO orders (id, customer_id, customer_email, status, total_amount, currency, 
                              created_at, updated_at)
            VALUES (:id, :customerId, :email, :status, :totalAmount, :currency, :createdAt, :updatedAt)
            """)
            .bind("id", id)
            .bind("customerId", customerId)
            .bind("email", email)
            .bind("status", status)
            .bind("totalAmount", totalAmount)
            .bind("currency", currency)
            .bind("createdAt", createdAt)
            .bind("updatedAt", createdAt)
            .then();
    }

    private reactor.core.publisher.Mono<Void> createTestOrder(
            String id,
            String customerId,
            String email,
            String status,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt,
            Instant updatedAt) {
        return databaseClient.sql("""
            INSERT INTO orders (id, customer_id, customer_email, status, total_amount, currency, 
                              created_at, updated_at)
            VALUES (:id, :customerId, :email, :status, :totalAmount, :currency, :createdAt, :updatedAt)
            """)
            .bind("id", id)
            .bind("customerId", customerId)
            .bind("email", email)
            .bind("status", status)
            .bind("totalAmount", totalAmount)
            .bind("currency", currency)
            .bind("createdAt", createdAt)
            .bind("updatedAt", updatedAt)
            .then();
    }

    private reactor.core.publisher.Mono<Void> createTestOrderItem(
            String orderId,
            String productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice) {
        return databaseClient.sql("""
            INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, currency)
            VALUES (:orderId, :productId, :productName, :quantity, :unitPrice, :currency)
            """)
            .bind("orderId", orderId)
            .bind("productId", productId)
            .bind("productName", productName)
            .bind("quantity", quantity)
            .bind("unitPrice", unitPrice)
            .bind("currency", "USD")
            .then();
    }
}