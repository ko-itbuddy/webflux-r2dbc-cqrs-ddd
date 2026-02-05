package com.example.order.domain.port;

import com.example.order.domain.event.DomainEvent;

import reactor.core.publisher.Mono;

/**
 * Output Port for persisting domain events.
 * Implementation resides in Infrastructure Layer.
 */
public interface DomainEventStore {

    /**
     * Store a domain event for later processing or audit trail.
     *
     * @param event the domain event to store
     * @return Mono<Void> indicating completion
     */
    Mono<Void> store(DomainEvent event);
}
