package com.example.order.adapter.out.persistence;

import com.example.order.domain.model.Order;
import com.example.order.application.port.out.OrderRepository;
import com.example.common.domain.port.DomainEventPublisher;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public class OrderPersistenceCommandAdapter implements OrderRepository {

    private final Mutiny.SessionFactory sessionFactory;
    private final DomainEventPublisher eventPublisher;

    public OrderPersistenceCommandAdapter(Mutiny.SessionFactory sessionFactory, DomainEventPublisher eventPublisher) {
        this.sessionFactory = sessionFactory;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Order> save(Order order) {
        return sessionFactory.withTransaction((session, tx) -> {
            // 1. Extract events from AbstractAggregateRoot
            Collection<Object> events = order.getRawEvents();
            
            // 2. Persist order
            Uni<Order> savedOrder = session.merge(order);
            
            if (events.isEmpty()) {
                return savedOrder;
            }

            // 3. Publish events (this hooks into our Outbox publisher)
            Uni<Void> publishChain = Uni.createFrom().voidItem();
            for (Object event : events) {
                if (event instanceof com.example.common.domain.event.DomainEvent domainEvent) {
                    publishChain = publishChain.chain(() -> Uni.createFrom().completionStage(eventPublisher.publish(domainEvent).toFuture()));
                }
            }

            return publishChain.chain(() -> savedOrder)
                    .invoke(saved -> order.clearEvents()); // Clear via custom public method
        })
        .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Order> findById(String id) {
        return sessionFactory.withSession(session -> session.find(Order.class, id))
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return sessionFactory.withTransaction((session, tx) -> 
                session.find(Order.class, id)
                       .onItem().ifNotNull().transformToUni(session::remove)
        )
        .replaceWithVoid()
        .convert().with(UniReactorConverters.toMono());
    }
}