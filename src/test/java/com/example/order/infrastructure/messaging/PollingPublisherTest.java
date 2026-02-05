package com.example.order.infrastructure.messaging;

import com.example.order.infrastructure.persistence.outbox.OutboxEvent;
import com.example.order.infrastructure.persistence.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollingPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private OutboxMessageMapper messageMapper;

    @InjectMocks
    private PollingPublisher pollingPublisher;

    private long eventIdCounter = 1L;

    @Test
    void shouldRetryOnRabbitMQFailure() throws InterruptedException {
        OutboxEvent event = createTestEvent("OrderCreatedEvent");
        CountDownLatch latch = new CountDownLatch(3);

        when(messageMapper.toRoutingKey(anyString())).thenReturn("order.created");
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        when(outboxRepository.markAsProcessed(anyLong())).thenReturn(Mono.empty());
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        verify(outboxRepository, never()).markAsProcessed(anyLong());
    }

    @Test
    void shouldMarkAsProcessedAfterSuccessfulPublish() throws InterruptedException {
        OutboxEvent event = createTestEvent("OrderCreatedEvent");
        Long eventId = event.getId();
        CountDownLatch latch = new CountDownLatch(1);

        when(messageMapper.toRoutingKey(anyString())).thenReturn("order.created");
        doNothing().when(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        when(outboxRepository.markAsProcessed(anyLong())).thenReturn(Mono.fromRunnable(latch::countDown));
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(outboxRepository).markAsProcessed(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(eventId);
    }

    @Test
    void shouldLogErrorWhenRetryExhausted() throws InterruptedException {
        OutboxEvent event = createTestEvent("OrderCreatedEvent");
        CountDownLatch latch = new CountDownLatch(3);

        when(messageMapper.toRoutingKey(anyString())).thenReturn("order.created");
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        when(outboxRepository.markAsProcessed(anyLong())).thenReturn(Mono.empty());
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        verify(outboxRepository, never()).markAsProcessed(anyLong());
    }

    @Test
    void shouldProcessMultipleEvents() throws InterruptedException {
        OutboxEvent event1 = createTestEvent("OrderCreatedEvent");
        OutboxEvent event2 = createTestEvent("OrderConfirmedEvent");
        CountDownLatch latch = new CountDownLatch(2);

        when(messageMapper.toRoutingKey("OrderCreatedEvent")).thenReturn("order.created");
        when(messageMapper.toRoutingKey("OrderConfirmedEvent")).thenReturn("order.confirmed");
        doNothing().when(amqpTemplate).convertAndSend(anyString(), any(Object.class));
        when(outboxRepository.markAsProcessed(anyLong())).thenReturn(Mono.fromRunnable(latch::countDown));
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event1, event2));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        verify(amqpTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
        verify(outboxRepository, times(2)).markAsProcessed(anyLong());
    }

    @Test
    void shouldHandlePartialFailure() throws InterruptedException {
        OutboxEvent event1 = createTestEvent("OrderCreatedEvent");
        OutboxEvent event2 = createTestEvent("OrderConfirmedEvent");
        CountDownLatch latch = new CountDownLatch(1);

        when(messageMapper.toRoutingKey("OrderCreatedEvent")).thenReturn("order.created");
        when(messageMapper.toRoutingKey("OrderConfirmedEvent")).thenReturn("order.confirmed");

        doNothing().when(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        doThrow(new RuntimeException("Connection failed"))
                .when(amqpTemplate).convertAndSend(eq("order.confirmed"), any(Object.class));

        when(outboxRepository.markAsProcessed(event1.getId())).thenReturn(Mono.fromRunnable(latch::countDown));
        when(outboxRepository.markAsProcessed(event2.getId())).thenReturn(Mono.empty());
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event1, event2));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        verify(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        verify(outboxRepository).markAsProcessed(event1.getId());
        verify(outboxRepository, never()).markAsProcessed(event2.getId());
    }

    @Test
    void shouldContinueOnSingleEventFailure() throws InterruptedException {
        OutboxEvent event1 = createTestEvent("OrderCreatedEvent");
        OutboxEvent event2 = createTestEvent("OrderConfirmedEvent");
        CountDownLatch latch = new CountDownLatch(1);

        when(messageMapper.toRoutingKey("OrderCreatedEvent")).thenReturn("order.created");
        when(messageMapper.toRoutingKey("OrderConfirmedEvent")).thenReturn("order.confirmed");

        doThrow(new RuntimeException("Permanent failure"))
                .when(amqpTemplate).convertAndSend(eq("order.created"), any(Object.class));
        doNothing().when(amqpTemplate).convertAndSend(eq("order.confirmed"), any(Object.class));

        when(outboxRepository.markAsProcessed(anyLong())).thenReturn(Mono.fromRunnable(latch::countDown));
        when(outboxRepository.findByProcessedFalse()).thenReturn(Flux.just(event1, event2));

        pollingPublisher.pollAndPublish();

        latch.await(3, TimeUnit.SECONDS);
        verify(amqpTemplate).convertAndSend(eq("order.confirmed"), any(Object.class));
        verify(outboxRepository, never()).markAsProcessed(event1.getId());
        verify(outboxRepository).markAsProcessed(event2.getId());
    }

    private OutboxEvent createTestEvent(String eventType) {
        OutboxEvent event = new OutboxEvent(
                "ORDER",
                "aggregate-" + eventIdCounter,
                eventType,
                "{\"orderId\":\"" + eventIdCounter + "\"}"
        );
        event.setId(eventIdCounter++);
        event.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        event.setProcessed(false);
        return event;
    }
}