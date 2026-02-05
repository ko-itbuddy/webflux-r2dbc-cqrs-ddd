package com.example.order.adapter.out.persistence;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.OrderRepository;
import com.example.order.domain.port.DomainEventPublisher;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class OrderR2dbcCommandRepository implements OrderRepository {

    private final Mutiny.SessionFactory sessionFactory;
    private final DomainEventPublisher eventPublisher;

    public OrderR2dbcCommandRepository(Mutiny.SessionFactory sessionFactory, DomainEventPublisher eventPublisher) {
        this.sessionFactory = sessionFactory;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Order> save(Order order) {
        return sessionFactory.withTransaction((session, tx) -> {
            // 1. Persist the order
            Uni<Order> savedOrder = session.merge(order);
            
            // 2. Extract and publish events
            List<com.example.order.domain.event.DomainEvent> events = order.getDomainEvents();
            if (events.isEmpty()) {
                return savedOrder;
            }

            // Create a chain of event publishing Unis
            Uni<Void> publishChain = Uni.createFrom().voidItem();
            for (var event : events) {
                // Use completionStage to handle Mono<Void> safely
                publishChain = publishChain.chain(() -> Uni.createFrom().completionStage(eventPublisher.publish(event).toFuture()));
            }

            // Clear events after publishing and return the saved order
            return publishChain.chain(() -> savedOrder)
                    .invoke(Order::clearDomainEvents);
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