package com.example.order.adapter.out.messaging;

import com.example.order.adapter.out.persistence.outbox.OutboxEvent;
import com.example.order.adapter.out.persistence.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class PollingPublisher {

    private static final Logger log = LoggerFactory.getLogger(PollingPublisher.class);

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
        outboxRepository.findUnprocessed()
                .flatMap(this::publishEvent)
                .subscribe(
                    null,
                    error -> log.error("Failed to poll and publish outbox events", error)
                );
    }

    private Mono<Void> publishEvent(OutboxEvent event) {
        String routingKey = messageMapper.toRoutingKey(event.getEventType());

        return Mono.fromCallable(() -> {
                    amqpTemplate.convertAndSend(routingKey, event.getPayload());
                    return event.getId();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(eventId -> outboxRepository.markAsProcessed(eventId))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                .onErrorResume(e -> {
                    log.error("Failed to publish event: {} - {}", event.getId(), e.getMessage());
                    return Mono.empty();
                });
    }
}