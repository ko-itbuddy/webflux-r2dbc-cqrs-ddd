package com.example.order.infrastructure.web.handler;

import com.example.order.application.out.query.OrderQueryPort;
import com.example.order.application.query.result.CustomerOrderStatsResult;
import com.example.order.application.query.result.OrderListItemResult;
import com.example.order.application.query.result.OrderSummaryResult;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.valueobject.OrderStatus;
import com.example.order.infrastructure.persistence.query.OrderR2dbcQueryRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web handler for order query operations.
 * Handles GET requests for retrieving order information.
 */
@Component
public class OrderQueryWebHandler {
    
    private final OrderQueryPort orderQueryPort;
    private final OrderR2dbcQueryRepository orderQueryRepository;
    
    public OrderQueryWebHandler(
            OrderQueryPort orderQueryPort,
            OrderR2dbcQueryRepository orderQueryRepository) {
        this.orderQueryPort = orderQueryPort;
        this.orderQueryRepository = orderQueryRepository;
    }
    
    /**
     * GET /api/orders/{orderId} - Get a specific order.
     */
    public Mono<ServerResponse> getOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        
        return orderQueryPort.findById(orderId)
            .flatMap(order -> ServerResponse.ok().bodyValue(mapToOrderResponse(order)))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    /**
     * GET /api/orders/{orderId}/summary - Get detailed order summary.
     */
    public Mono<ServerResponse> getOrderSummary(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        
        return orderQueryRepository.findOrderSummary(orderId)
            .flatMap(summary -> ServerResponse.ok().bodyValue(summary))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    /**
     * GET /api/orders - List all orders (paginated).
     */
    public Mono<ServerResponse> listOrders(ServerRequest request) {
        int page = request.queryParam("page")
            .map(Integer::parseInt)
            .orElse(0);
        int size = request.queryParam("size")
            .map(Integer::parseInt)
            .orElse(10);
        
        return orderQueryRepository.findOrderList(page, size)
            .collectList()
            .flatMap(orders -> ServerResponse.ok().bodyValue(new OrderListResponse(orders, orders.size())));
    }
    
    /**
     * GET /api/customers/{customerId}/orders - Get orders by customer.
     */
    public Mono<ServerResponse> getCustomerOrders(ServerRequest request) {
        String customerId = request.pathVariable("customerId");
        
        return orderQueryPort.findByCustomerId(customerId)
            .collectList()
            .flatMap(orders -> ServerResponse.ok().bodyValue(
                orders.stream().map(this::mapToOrderResponse).toList()
            ));
    }
    
    /**
     * GET /api/customers/{customerId}/stats - Get customer order statistics.
     */
    public Mono<ServerResponse> getCustomerStats(ServerRequest request) {
        String customerId = request.pathVariable("customerId");
        
        return orderQueryRepository.findCustomerStats(customerId)
            .flatMap(stats -> ServerResponse.ok().bodyValue(stats));
    }
    
    /**
     * GET /api/orders/by-status/{status} - Get orders by status.
     */
    public Mono<ServerResponse> getOrdersByStatus(ServerRequest request) {
        String statusStr = request.pathVariable("status");
        OrderStatus status = OrderStatus.valueOf(statusStr.toUpperCase());
        
        return orderQueryPort.findByStatus(status)
            .collectList()
            .flatMap(orders -> ServerResponse.ok().bodyValue(
                orders.stream().map(this::mapToOrderResponse).toList()
            ));
    }
    
    // Helper methods
    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerEmail().getValue(),
            order.getStatus().name(),
            order.getTotalAmount().getAmount(),
            order.getTotalAmount().getCurrency(),
            order.getDiscountAmount() != null ? order.getDiscountAmount().getAmount() : null,
            order.getFinalAmount().getAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
    
    // DTO Records
    public record OrderResponse(
        String orderId,
        String customerId,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }
    
    public record OrderListResponse(
        List<OrderListItemResult> orders,
        int count
    ) {
    }
}
