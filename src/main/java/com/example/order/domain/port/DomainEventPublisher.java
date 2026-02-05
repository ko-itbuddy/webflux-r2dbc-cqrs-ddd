package com.example.order.domain.port;

import com.example.order.domain.event.DomainEvent;

import reactor.core.publisher.Mono;

/**
 * Output Port for publishing domain events to external systems.
 * Implementation resides in Infrastructure Layer.
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event to external message broker or event bus.
     *
     * @param event the domain event to publish
     * @return Mono<Void> indicating completion
     */
    Mono<Void> publish(DomainEvent event);
}
