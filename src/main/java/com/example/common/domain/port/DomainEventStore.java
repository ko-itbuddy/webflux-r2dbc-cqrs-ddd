package com.example.common.domain.port;

import com.example.common.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

public interface DomainEventStore {
    Mono<Void> save(DomainEvent event);
}