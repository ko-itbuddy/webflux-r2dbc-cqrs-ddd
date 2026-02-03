package com.example.order.infrastructure.persistence.command;

import com.example.order.application.out.command.OrderCommandPort;
import com.example.order.domain.order.entity.Order;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import com.example.order.infrastructure.persistence.mapper.OrderMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Command repository implementation using R2DBC.
 * Handles write operations (save, delete) for orders.
 */
@Repository
public class OrderR2dbcCommandRepository implements OrderCommandPort {
    
    private final R2dbcEntityTemplate r2dbcTemplate;
    private final DatabaseClient databaseClient;
    private final OrderMapper orderMapper;
    
    public OrderR2dbcCommandRepository(
            R2dbcEntityTemplate r2dbcTemplate,
            DatabaseClient databaseClient,
            OrderMapper orderMapper) {
        this.r2dbcTemplate = r2dbcTemplate;
        this.databaseClient = databaseClient;
        this.orderMapper = orderMapper;
    }
    
    @Override
    public Mono<Order> save(Order order) {
        // Convert domain to entity
        OrderEntity orderEntity = orderMapper.toEntity(order);
        List<OrderItemEntity> itemEntities = orderMapper.extractItemEntities(order);
        
        // Check if order exists
        return r2dbcTemplate.selectOne(
                Query.query(Criteria.where("id").is(order.getId())),
                OrderEntity.class)
            .flatMap(existing -> updateOrder(orderEntity, itemEntities))
            .switchIfEmpty(insertOrder(orderEntity, itemEntities));
    }
    
    private Mono<Order> insertOrder(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        return r2dbcTemplate.insert(orderEntity)
            .flatMap(savedOrder ->
                saveOrderItems(savedOrder.getId(), itemEntities)
                    .then(Mono.just(savedOrder))
            )
            .flatMap(savedOrder -> findById(savedOrder.getId()));
    }
    
    private Mono<Order> updateOrder(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        return r2dbcTemplate.update(orderEntity)
            .flatMap(updatedOrder ->
                deleteOrderItems(updatedOrder.getId())
                    .then(saveOrderItems(updatedOrder.getId(), itemEntities))
                    .then(Mono.just(updatedOrder))
            )
            .flatMap(updatedOrder -> findById(updatedOrder.getId()));
    }

    private Mono<Order> findById(String orderId) {
        return r2dbcTemplate.selectOne(
                Query.query(Criteria.where("id").is(orderId)),
                OrderEntity.class)
            .flatMap(savedEntity ->
                r2dbcTemplate.select(
                        Query.query(Criteria.where("order_id").is(orderId)),
                        OrderItemEntity.class)
                    .collectList()
                    .map(items -> orderMapper.toDomain(savedEntity, items))
            );
    }
    
    private Mono<Void> saveOrderItems(String orderId, List<OrderItemEntity> items) {
        if (items.isEmpty()) {
            return Mono.empty();
        }
        
        // Insert items one by one (could be optimized with batch insert)
        return Mono.when(
            items.stream()
                .map(item -> {
                    item.setOrderId(orderId);
                    return r2dbcTemplate.insert(item);
                })
                .toArray(Mono[]::new)
        );
    }
    
    private Mono<Void> deleteOrderItems(String orderId) {
        return databaseClient.sql("DELETE FROM order_items WHERE order_id = :orderId")
            .bind("orderId", orderId)
            .fetch()
            .rowsUpdated()
            .then();
    }
    
    @Override
    public Mono<Void> deleteById(String orderId) {
        // Delete items first (cascade)
        return deleteOrderItems(orderId)
            .then(r2dbcTemplate.delete(
                Query.query(Criteria.where("id").is(orderId)),
                OrderEntity.class
            ))
            .then();
    }
}
