package com.example.order.infrastructure.persistence;

import com.example.order.application.command.handler.CreateOrderHandler;
import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransactionTest {

    @Autowired
    private CreateOrderHandler createOrderHandler;

    @Autowired
    private DatabaseClient databaseClient;

    @MockBean(name = "rabbitConnectionFactory")
    private ConnectionFactory connectionFactory;

    @MockBean(name = "amqpTemplate")
    private AmqpTemplate amqpTemplate;

    @MockBean(name = "rabbitTransactionManager")
    private RabbitTransactionManager rabbitTransactionManager;

    @Test
    void createOrder_WithValidData_ShouldPersistOrderAndItems() {
        var item = new CreateOrderCommand.OrderItemCommand(
            "PROD-001",
            "Test Product",
            2,
            new BigDecimal("99.99"),
            "USD"
        );
        var command = new CreateOrderCommand(
            "CUST-001",
            "test@example.com",
            List.of(item)
        );

        StepVerifier.create(
            createOrderHandler.handle(command)
                .flatMap(order ->
                    databaseClient.sql("SELECT COUNT(*) FROM orders WHERE id = :id")
                        .bind("id", order.getId())
                        .map((row, metadata) -> row.get(0, Long.class))
                        .first()
                        .zipWith(
                            databaseClient.sql("SELECT COUNT(*) FROM order_items WHERE order_id = :orderId")
                                .bind("orderId", order.getId())
                                .map((row, metadata) -> row.get(0, Long.class))
                                .first()
                        )
                        .map(tuple -> new OrderVerificationResult(order, tuple.getT1(), tuple.getT2()))
                )
        )
        .assertNext(result -> {
            assertThat(result.order().getCustomerId()).isEqualTo("CUST-001");
            assertThat(result.order().getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.order().getItems()).hasSize(1);
            assertThat(result.orderCount()).isEqualTo(1L);
            assertThat(result.itemCount()).isEqualTo(1L);
        })
        .verifyComplete();
    }

    private record OrderVerificationResult(Order order, Long orderCount, Long itemCount) {}

    @Autowired
    private org.springframework.transaction.reactive.TransactionalOperator transactionalOperator;

    @AfterEach
    void cleanup() {
        databaseClient.sql("DELETE FROM order_items").then()
            .then(databaseClient.sql("DELETE FROM orders").then())
            .block();
    }

    @Test
    void transaction_ShouldRollbackOnError() {
        var item = new CreateOrderCommand.OrderItemCommand(
            "PROD-001",
            "Test Product",
            2,
            new BigDecimal("99.99"),
            "USD"
        );

        Mono<Order> failingTransaction = transactionalOperator.transactional(
            Mono.defer(() -> {
                var command = new CreateOrderCommand(
                    "CUST-ROLLBACK",
                    "rollback@example.com",
                    List.of(item)
                );
                return createOrderHandler.handle(command)
                    .flatMap(order -> Mono.error(new RuntimeException("Simulated error after save")));
            })
        );

        StepVerifier.create(failingTransaction)
            .expectError(RuntimeException.class)
            .verify();

        StepVerifier.create(
            databaseClient.sql("SELECT COUNT(*) FROM orders WHERE customer_id = :customerId")
                .bind("customerId", "CUST-ROLLBACK")
                .map((row, metadata) -> row.get(0, Long.class))
                .first()
        )
        .assertNext(count -> assertThat(count).isEqualTo(0L))
        .verifyComplete();

    }
}