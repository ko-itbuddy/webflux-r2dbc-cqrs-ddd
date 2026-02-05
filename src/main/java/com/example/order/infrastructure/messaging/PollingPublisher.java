package com.example.order.infrastructure.messaging;

import com.example.order.infrastructure.persistence.outbox.OutboxEvent;
import com.example.order.infrastructure.persistence.outbox.OutboxRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class PollingPublisher {

    private final OutboxRepository outboxRepository;
    private final AmqpTemplate amqpTemplate;
    private final OutboxMessageMapper messageMapper;

    public PollingPublisher(OutboxRepository outboxRepository, 
                           AmqpTemplate amqpTemplate,
                           OutboxMessageMapper messageMapper) {
        this.outboxRepository = outboxRepository;
        this.amqpTemplate = amqpTemplate;
        this.messageMapper = messageMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAndPublish() {
        outboxRepository.findByProcessedFalse()
                .flatMap(this::publishEvent)
                .subscribe();
    }

    private Mono<Void> publishEvent(OutboxEvent event) {
        String routingKey = messageMapper.toRoutingKey(event.getEventType());
        
        return Mono.fromCallable(() -> {
                    amqpTemplate.convertAndSend(routingKey, event.getPayload());
                    return event.getId();
                })
                .flatMap(eventId -> outboxRepository.markAsProcessed(eventId))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                .onErrorResume(e -> {
                    System.err.println("Failed to publish event: " + event.getId() + " - " + e.getMessage());
                    return Mono.empty();
                });
    }
}
