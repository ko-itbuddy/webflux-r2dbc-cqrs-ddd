package com.example.order.application.service;

import com.example.order.application.dto.OrderSummaryQuery;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.application.service.OrderQueryHandler;
import com.example.order.application.dto.CustomerOrderStatsResult;
import com.example.order.application.dto.OrderListItemResult;
import com.example.order.application.dto.OrderSummaryResult;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.model.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Query service implementation that delegates to repository.
 * Part of CQRS read model.
 */
@Service
public class OrderQueryService implements OrderQueryHandler {
    
    private final OrderQueryPort queryPort;
    
    public OrderQueryService(OrderQueryPort queryPort) {
        this.queryPort = queryPort;
    }
    
    @Override
    public Mono<OrderSummaryResult> handle(OrderSummaryQuery query) {
        return queryPort.findOrderSummary(query.orderId());
    }
    
    @Override
    public Mono<Order> findById(String orderId) {
        return queryPort.findById(orderId);
    }
    
    @Override
    public Flux<Order> findByCustomerId(String customerId) {
        return queryPort.findByCustomerId(customerId);
    }
    
    @Override
    public Flux<Order> findByStatus(OrderStatus status) {
        return queryPort.findByStatus(status);
    }
    
    @Override
    public Flux<Order> findAll(int page, int size) {
        return queryPort.findAll(page, size);
    }
    
    @Override
    public Mono<Long> count() {
        return queryPort.count();
    }
    
    @Override
    public Flux<OrderListItemResult> findOrderList(int page, int size) {
        return queryPort.findOrderList(page, size);
    }
    
    @Override
    public Mono<CustomerOrderStatsResult> findCustomerStats(String customerId) {
        return queryPort.findCustomerStats(customerId);
    }
}
