package com.example.order.adapter.out.persistence;

import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Repository
public class OrderR2dbcQueryRepository implements OrderQueryPort {

    private final Mutiny.SessionFactory sessionFactory;

    public OrderR2dbcQueryRepository(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Order> findById(String orderId) {
        return sessionFactory.withSession(session -> 
                session.find(Order.class, orderId))
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Flux<Order> findByCustomerId(String customerId) {
        String hql = "from Order where customerId = :customerId";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Order.class).setParameter("customerId", customerId).getResultList())
                .onItem().transformToMulti(list -> io.smallrye.mutiny.Multi.createFrom().iterable(list))
                .convert().with(io.smallrye.mutiny.converters.multi.MultiReactorConverters.toFlux());
    }

    @Override
    public Flux<Order> findByStatus(OrderStatus status) {
        String hql = "from Order where status = :status";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Order.class).setParameter("status", status).getResultList())
                .onItem().transformToMulti(list -> io.smallrye.mutiny.Multi.createFrom().iterable(list))
                .convert().with(io.smallrye.mutiny.converters.multi.MultiReactorConverters.toFlux());
    }

    @Override
    public Flux<Order> findAll(int page, int size) {
        String hql = "from Order order by createdAt desc";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Order.class)
                       .setFirstResult(page * size)
                       .setMaxResults(size)
                       .getResultList())
                .onItem().transformToMulti(list -> io.smallrye.mutiny.Multi.createFrom().iterable(list))
                .convert().with(io.smallrye.mutiny.converters.multi.MultiReactorConverters.toFlux());
    }

    @Override
    public Mono<Long> count() {
        String hql = "select count(o) from Order o";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Long.class).getSingleResult())
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<OrderSummaryResult> findOrderSummary(String orderId) {
        String hql = "select o from Order o left join fetch o.items where o.id = :id";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Order.class).setParameter("id", orderId).getSingleResultOrNull())
                .map(entity -> entity == null ? null : toSummaryResult(entity))
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Flux<OrderListItemResult> findOrderList(int page, int size) {
        return findAll(page, size).map(o -> new OrderListItemResult(
            o.getId(), o.getCustomerId(), o.getStatus(), 
            o.getFinalAmount().getAmount(), o.getFinalAmount().getCurrency(),
            o.getItemCount(), o.getCreatedAt(), o.getUpdatedAt()
        ));
    }

    @Override
    public Mono<CustomerOrderStatsResult> findCustomerStats(String customerId) {
        String hql = "select count(o), sum(o.totalAmount.amount) from Order o where o.customerId = :customerId";
        return sessionFactory.withSession(session -> 
                session.createQuery(hql, Object[].class).setParameter("customerId", customerId).getSingleResult())
                .map(row -> {
                    long total = (Long)row[0];
                    java.math.BigDecimal spent = java.math.BigDecimal.valueOf(row[1] == null ? 0.0 : ((Number)row[1]).doubleValue());
                    return new CustomerOrderStatsResult(
                        customerId, total, 0, 0, 0, 
                        spent, total == 0 ? spent : spent.divide(java.math.BigDecimal.valueOf(total), java.math.RoundingMode.HALF_UP),
                        "USD", Collections.emptyMap(), 0
                    );
                })
                .convert().with(UniReactorConverters.toMono());
    }

    private OrderSummaryResult toSummaryResult(Order order) {
        List<OrderSummaryResult.OrderItemSummary> itemSummaries = order.getItems().stream()
            .map(item -> new OrderSummaryResult.OrderItemSummary(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice().getAmount(),
                item.calculateSubtotal().getAmount()
            )).toList();

        return new OrderSummaryResult(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerEmail().getValue(),
            order.getStatus(),
            order.getTotalAmount().getAmount(),
            order.getTotalAmount().getCurrency(),
            order.getDiscountAmount() != null ? order.getDiscountAmount().getAmount() : null,
            order.getFinalAmount().getAmount(),
            null,
            order.getItemCount(),
            itemSummaries,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
