package com.example.common.domain.port;

import com.example.common.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

public interface DomainEventPublisher {
    Mono<Void> publish(DomainEvent event);
}