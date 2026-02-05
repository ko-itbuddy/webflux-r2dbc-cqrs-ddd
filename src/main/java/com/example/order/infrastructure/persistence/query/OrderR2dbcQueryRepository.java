package com.example.order.infrastructure.persistence.query;

import com.example.order.application.query.port.OrderQueryPort;
import com.example.order.application.query.result.CustomerOrderStatsResult;
import com.example.order.application.query.result.OrderListItemResult;
import com.example.order.application.query.result.OrderSummaryResult;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import com.example.order.infrastructure.persistence.mapper.OrderMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query repository implementation using R2DBC.
 * Handles read operations with complex queries for orders.
 */
@Repository
public class OrderR2dbcQueryRepository implements OrderQueryPort {
    
    private final R2dbcEntityTemplate r2dbcTemplate;
    private final DatabaseClient databaseClient;
    private final OrderMapper orderMapper;
    
    public OrderR2dbcQueryRepository(
            R2dbcEntityTemplate r2dbcTemplate,
            DatabaseClient databaseClient,
            OrderMapper orderMapper) {
        this.r2dbcTemplate = r2dbcTemplate;
        this.databaseClient = databaseClient;
        this.orderMapper = orderMapper;
    }
    
    @Override
    public Mono<Order> findById(String orderId) {
        return r2dbcTemplate.selectOne(
                Query.query(Criteria.where("id").is(orderId)),
                OrderEntity.class)
            .flatMap(orderEntity -> 
                findItemsByOrderId(orderId)
                    .collectList()
                    .map(items -> orderMapper.toDomain(orderEntity, items))
            );
    }
    
    @Override
    public Flux<Order> findByCustomerId(String customerId) {
        return r2dbcTemplate.select(
                Query.query(Criteria.where("customer_id").is(customerId)),
                OrderEntity.class)
            .flatMap(orderEntity ->
                findItemsByOrderId(orderEntity.getId())
                    .collectList()
                    .map(items -> orderMapper.toDomain(orderEntity, items))
            );
    }
    
    @Override
    public Flux<Order> findByStatus(OrderStatus status) {
        return r2dbcTemplate.select(
                Query.query(Criteria.where("status").is(status)),
                OrderEntity.class)
            .flatMap(orderEntity ->
                findItemsByOrderId(orderEntity.getId())
                    .collectList()
                    .map(items -> orderMapper.toDomain(orderEntity, items))
            );
    }
    
    @Override
    public Flux<Order> findAll(int page, int size) {
        return r2dbcTemplate.select(
                Query.empty()
                    .offset((long) page * size)
                    .limit(size),
                OrderEntity.class)
            .flatMap(orderEntity ->
                findItemsByOrderId(orderEntity.getId())
                    .collectList()
                    .map(items -> orderMapper.toDomain(orderEntity, items))
            );
    }
    
    @Override
    public Mono<Long> count() {
        return databaseClient.sql("SELECT COUNT(*) FROM orders")
            .map(row -> row.get(0, Long.class))
            .first();
    }
    
    /**
     * Find order items by order ID.
     */
    private Flux<OrderItemEntity> findItemsByOrderId(String orderId) {
        return r2dbcTemplate.select(
            Query.query(Criteria.where("order_id").is(orderId)),
            OrderItemEntity.class
        );
    }
    
    /**
     * Complex query: Find detailed order summary with calculated fields.
     */
    @Override
    public Mono<OrderSummaryResult> findOrderSummary(String orderId) {
        String sql = """
            SELECT 
                o.id as order_id,
                o.customer_id,
                o.customer_email,
                o.status,
                o.total_amount,
                o.currency,
                o.discount_amount,
                o.created_at,
                o.updated_at,
                oi.product_id,
                oi.product_name,
                oi.quantity,
                oi.unit_price,
                oi.currency as item_currency
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.id = :orderId
            """;
        
        return databaseClient.sql(sql)
            .bind("orderId", orderId)
            .map((row, metadata) -> {
                // Build order summary from row
                String id = row.get("order_id", String.class);
                String customerId = row.get("customer_id", String.class);
                String customerEmail = row.get("customer_email", String.class);
                OrderStatus status = OrderStatus.valueOf(row.get("status", String.class));
                BigDecimal totalAmount = row.get("total_amount", BigDecimal.class);
                String currency = row.get("currency", String.class);
                BigDecimal discountAmount = row.get("discount_amount", BigDecimal.class);
                Instant createdAt = row.get("created_at", Instant.class);
                Instant updatedAt = row.get("updated_at", Instant.class);
                
                String productId = row.get("product_id", String.class);
                String productName = row.get("product_name", String.class);
                Integer quantity = row.get("quantity", Integer.class);
                BigDecimal unitPrice = row.get("unit_price", BigDecimal.class);
                String itemCurrency = row.get("item_currency", String.class);
                
                OrderSummaryResult.OrderItemSummary itemSummary = null;
                if (productId != null) {
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                    itemSummary = new OrderSummaryResult.OrderItemSummary(
                        productId, productName, quantity, unitPrice, subtotal
                    );
                }
                
                return Map.entry(
                    new OrderSummaryResult(
                        id, customerId, customerEmail, status, totalAmount, currency,
                        discountAmount != null ? discountAmount : totalAmount,
                        discountAmount != null ? discountAmount : totalAmount,
                        BigDecimal.ZERO, // calculated discount - will be computed later
                        0, List.of(), createdAt, updatedAt
                    ),
                    itemSummary
                );
            })
            .all()
            .collectList()
            .map(entries -> {
                if (entries.isEmpty()) {
                    return null;
                }
                
                OrderSummaryResult first = entries.get(0).getKey();
                List<OrderSummaryResult.OrderItemSummary> items = entries.stream()
                    .map(Map.Entry::getValue)
                    .filter(item -> item != null)
                    .toList();
                
                int itemCount = items.stream().mapToInt(OrderSummaryResult.OrderItemSummary::quantity).sum();
                
                return new OrderSummaryResult(
                    first.orderId(), first.customerId(), first.customerEmail(),
                    first.status(), first.totalAmount(), first.currency(),
                    first.discountAmount(), first.finalAmount(),
                    BigDecimal.ZERO, itemCount, items,
                    first.createdAt(), first.updatedAt()
                );
            });
    }
    
    /**
     * Complex query: Find simplified order list for list views.
     */
    @Override
    public Flux<OrderListItemResult> findOrderList(int page, int size) {
        String sql = """
            SELECT 
                o.id as order_id,
                o.customer_id,
                o.status,
                o.discount_amount as final_amount,
                o.total_amount,
                o.currency,
                o.created_at,
                o.updated_at,
                COUNT(oi.id) as item_count,
                SUM(oi.quantity) as total_quantity
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            GROUP BY o.id, o.customer_id, o.status, o.discount_amount, o.total_amount, o.currency, o.created_at, o.updated_at
            ORDER BY o.created_at DESC
            LIMIT :limit OFFSET :offset
            """;
        
        return databaseClient.sql(sql)
            .bind("limit", size)
            .bind("offset", page * size)
            .map((row, metadata) -> new OrderListItemResult(
                row.get("order_id", String.class),
                row.get("customer_id", String.class),
                OrderStatus.valueOf(row.get("status", String.class)),
                row.get("final_amount", BigDecimal.class) != null 
                    ? row.get("final_amount", BigDecimal.class) 
                    : row.get("total_amount", BigDecimal.class),
                row.get("currency", String.class),
                row.get("total_quantity", Long.class) != null 
                    ? row.get("total_quantity", Long.class).intValue() 
                    : 0,
                row.get("created_at", Instant.class),
                row.get("updated_at", Instant.class)
            ))
            .all();
    }
    
    /**
     * Complex query: Find customer order statistics with aggregations.
     */
    @Override
    public Mono<CustomerOrderStatsResult> findCustomerStats(String customerId) {
        String sql = """
            SELECT 
                COUNT(*) as total_orders,
                SUM(CASE WHEN status IN ('PENDING', 'CONFIRMED', 'PAID') THEN 1 ELSE 0 END) as active_orders,
                SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as completed_orders,
                SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_orders,
                COALESCE(SUM(CASE WHEN discount_amount IS NOT NULL THEN discount_amount ELSE total_amount END), 0) as total_spent,
                currency,
                COALESCE(SUM((SELECT SUM(quantity) FROM order_items WHERE order_id = o.id)), 0) as total_items
            FROM orders o
            WHERE customer_id = :customerId
            GROUP BY currency
            """;
        
        return databaseClient.sql(sql)
            .bind("customerId", customerId)
            .map((row, metadata) -> {
                long totalOrders = row.get("total_orders", Long.class);
                long activeOrders = row.get("active_orders", Long.class);
                long completedOrders = row.get("completed_orders", Long.class);
                long cancelledOrders = row.get("cancelled_orders", Long.class);
                BigDecimal totalSpent = row.get("total_spent", BigDecimal.class);
                String currency = row.get("currency", String.class);
                long totalItems = row.get("total_items", Long.class);
                
                BigDecimal avgOrderValue = totalOrders > 0 
                    ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
                
                Map<OrderStatus, Long> ordersByStatus = Map.of(
                    OrderStatus.PENDING, 0L,
                    OrderStatus.CONFIRMED, 0L,
                    OrderStatus.PAID, activeOrders,
                    OrderStatus.SHIPPED, 0L,
                    OrderStatus.DELIVERED, completedOrders,
                    OrderStatus.CANCELLED, cancelledOrders
                );
                
                return new CustomerOrderStatsResult(
                    customerId, totalOrders, activeOrders, completedOrders, cancelledOrders,
                    totalSpent, avgOrderValue, currency, ordersByStatus, totalItems
                );
            })
            .first()
            .switchIfEmpty(Mono.just(new CustomerOrderStatsResult(
                customerId, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, "USD",
                Map.of(), 0L
            )));
    }
}
