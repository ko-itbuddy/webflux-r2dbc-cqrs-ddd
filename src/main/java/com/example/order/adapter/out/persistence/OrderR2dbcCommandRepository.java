package com.example.order.adapter.out.persistence;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.OrderRepository;
import com.example.order.adapter.out.persistence.entity.OrderEntity;
import com.example.order.adapter.out.persistence.mapper.OrderMapper;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class OrderR2dbcCommandRepository implements OrderRepository {

    private final Mutiny.SessionFactory sessionFactory;
    private final OrderMapper orderMapper;

    public OrderR2dbcCommandRepository(Mutiny.SessionFactory sessionFactory, OrderMapper orderMapper) {
        this.sessionFactory = sessionFactory;
        this.orderMapper = orderMapper;
    }

    @Override
    public Mono<Order> save(Order order) {
        OrderEntity entity = orderMapper.toEntity(order);
        return sessionFactory.withTransaction((session, tx) -> session.persist(entity))
                .replaceWith(entity)
                .map(orderMapper::toDomain)
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Order> findById(String id) {
        return sessionFactory.withSession(session -> session.find(OrderEntity.class, id))
                .map(orderMapper::toDomain)
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return sessionFactory.withTransaction((session, tx) -> 
                session.find(OrderEntity.class, id)
                       .flatMap(session::remove)
        )
        .convert().with(UniReactorConverters.toMono());
    }
}