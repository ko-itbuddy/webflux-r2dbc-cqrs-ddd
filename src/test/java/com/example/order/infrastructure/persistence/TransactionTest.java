package com.example.order.infrastructure.persistence;

import com.example.order.application.command.handler.CreateOrderHandler;
import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
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
                        .map(tuple -> {
                            assertThat(tuple.getT1()).isEqualTo(1L);
                            assertThat(tuple.getT2()).isEqualTo(1L);
                            return order;
                        })
                )
        )
        .assertNext(order -> {
            assertThat(order.getCustomerId()).isEqualTo("CUST-001");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getItems()).hasSize(1);
        })
        .verifyComplete();
    }

    @Autowired
    private org.springframework.transaction.reactive.TransactionalOperator transactionalOperator;

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

        System.out.println("Rollback completed - No data persisted for failed transaction");
    }
}