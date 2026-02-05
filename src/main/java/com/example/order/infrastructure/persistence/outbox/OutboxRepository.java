package com.example.order.infrastructure.persistence.outbox;

import com.example.order.infrastructure.persistence.outbox.OutboxEvent;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class OutboxRepository {

    private final R2dbcEntityTemplate r2dbcTemplate;
    private final DatabaseClient databaseClient;

    public OutboxRepository(R2dbcEntityTemplate r2dbcTemplate, DatabaseClient databaseClient) {
        this.r2dbcTemplate = r2dbcTemplate;
        this.databaseClient = databaseClient;
    }

    public Mono<OutboxEvent> save(OutboxEvent event) {
        return r2dbcTemplate.insert(event);
    }

    public Flux<OutboxEvent> findByProcessedFalse() {
        return r2dbcTemplate.select(
                Query.query(Criteria.where("processed").is(false))
                        .sort(org.springframework.data.domain.Sort.by("created_at").ascending()),
                OutboxEvent.class);
    }

    public Mono<Void> markAsProcessed(Long id) {
        return r2dbcTemplate.update(
                        Query.query(Criteria.where("id").is(id)),
                        Update.update("processed", true),
                        OutboxEvent.class)
                .then();
    }
}
