package com.example.order.adapter.out.persistence.outbox;

import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class OutboxRepository {

    private final Mutiny.SessionFactory sessionFactory;

    public OutboxRepository(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Mono<OutboxEvent> save(OutboxEvent event) {
        return sessionFactory.withTransaction((session, tx) -> session.persist(event))
                .replaceWith(event)
                .convert().with(UniReactorConverters.toMono());
    }

    public Flux<OutboxEvent> findUnprocessed() {
        String hql = "from OutboxEvent where processed = false order by createdAt asc";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, OutboxEvent.class).getResultList()
        )
        .onItem().transformToMulti(list -> io.smallrye.mutiny.Multi.createFrom().iterable(list))
        .convert().with(io.smallrye.mutiny.converters.multi.MultiReactorConverters.toFlux());
    }

    public Mono<Void> markAsProcessed(Long id) {
        return sessionFactory.withTransaction((session, tx) -> 
                session.find(OutboxEvent.class, id)
                       .onItem().ifNotNull().invoke(event -> event.setProcessed(true))
        )
        .replaceWithVoid()
        .convert().with(UniReactorConverters.toMono());
    }
}